/*
 * Copyright 2019-2020 ForgeRock AS. All Rights Reserved
 *
 * Use of this code requires a commercial software license with ForgeRock AS.
 * or with one of its affiliates. All use shall be exclusively subject
 * to such license between the licensee and ForgeRock AS.
 */

import com.forgerock.pipeline.reporting.PipelineRun

void runStage(PipelineRun pipelineRun) {

    def stageName = 'PIT2 Binary Upgrade'
    def normalizedStageName = dashboard_utils.normalizeStageName(stageName)

    pipelineRun.pushStageOutcome(normalizedStageName, stageDisplayName: stageName) {
        node('google-cloud') {
            stage(stageName) {
                pipelineRun.updateStageStatusAsInProgress()

                dir('lodestar') {
                    def stagesCloud = [:]

                    // Upgrade tests
                    def subStageName = 'binary_upgrade'
                    stagesCloud[subStageName] = dashboard_utils.spyglaasStageCloud(subStageName)

                    dashboard_utils.determineUnitOutcome(stagesCloud[subStageName]) {
                        def config = [
                            TESTS_SCOPE                             : 'tests/pit2/upgrade',
                            CLUSTER_DOMAIN                          : 'pit-24-7.forgeops.com',
                            CLUSTER_NAMESPACE                       : subStageName,
                            COMPONENTS_AM_IMAGE_UPGRADE_TAG         : commonModule.getCurrentTag('am'),
                            COMPONENTS_IDM_IMAGE_UPGRADE_TAG        : commonModule.getCurrentTag('idm'),
                            COMPONENTS_IG_IMAGE_UPGRADE_TAG         : commonModule.getCurrentTag('ig'),
                            COMPONENTS_DSIDREPO_IMAGE_UPGRADE_TAG   : commonModule.getCurrentTag('ds-idrepo'),
                            STASH_LODESTAR_BRANCH                   : commonModule.LODESTAR_GIT_COMMIT,
                            STASH_FORGEOPS_BRANCH                   : 'fraas-production',
                            REPORT_NAME                             : subStageName,
                        ]

                        withGKESpyglaasNoStages(config)
                    }

                    summaryReportGen.createAndPublishSummaryReport(stagesCloud, stageName, '', false,
                        normalizedStageName, "${normalizedStageName}.html")
                    return dashboard_utils.determineLodestarOutcome(stagesCloud,
                        "${env.BUILD_URL}/${normalizedStageName}/")
                }
            }
        }
    }
}

return this
