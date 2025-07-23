//noinspection GroovyUnusedAssignment
@Library('COMMUNITY@master') _

import groovy.transform.Field

env.ARTIFACTORY_URL = "https://artifactory.datev.de"

//noinspection GroovyAssignabilityCheck -- false positive
properties([
    disableConcurrentBuilds(),
    gitLabConnection('git.datev.de'),
    buildDiscarder(logRotator(numToKeepStr: '10'))
])

@Field final Map settings = [
    'global': [
        org                          : 'refsys-online',
        space                        : 'aggregationservice',
        app                          : 'refsys-aggregation-processing-service',
        artifactoryId                : 'artifactory.datev.de',
        credentialsId                : 'artifactory.datev.de',
        fortifyAppName               : 'refsys-online.aggregation-service.refsys-aggregation-processing-service',
        fortifySourceVersion         : '17',
        healthEndpoint               : '/actuator/health',
        // set to true if the build should fail when SonarQube's quality gate fails
        sonarAbortPipelineOnFailure  : false,
        sonarType                    : 'cloud',
        // set to true if not only sonar but also jenkins should display jacoco coverage results
        enableJacocoJenkinsPublishing: false,
        // set to true if you want to enable a retry in case of a failed dev deployment
        allowRetryOnDevFailure       : true,
        // set to true if you want to enable a retry in case of a failed qs deployment
        allowRetryOnQsFailure        : true,
        // set to true if you want to enable a retry in case of a failed prod deployment
        allowRetryOnProdFailure      : true,
        whiteSource                  : [
            // all settings here are optional!
            serviceId            : '300571',
            changeAssignmentGroup: 'RefSys-Online',
            scanMode             : 'eua',
            failOnPolicyViolation: 'never',
            policy               : 'datev', // customer | internal
            configOpts           : []       // configOpts to pass to whitesource.scan. The default is []
        ],
        jdkVersion                   : 'openjdk 17',
        // services to be created with cf cs
        services                     : [
            'app-autoscaler standard autoscaler',
            'splunk-hec-logger standard splunk',
            'IcingaService HealthCheck icinga',
        ],
        icingaServiceName            : 'icinga',
        routes                       : [
            finalRoute    : 'refsys-aggregation-processing-service',
            finalTempRoute: 'refsys-aggregation-processing-service-temp'
        ],
        prodNotificationEmail        : 'RefSys-Online-Product-Team@datev.de',
        deleteReleaseBranchOnError   : 'false',
        pactFlowTokenCredentialsId : 'PACTFLOW_API_TOKEN',
        pactBrokerUrl: "https://datev.pactflow.io",
        withTestcontainersCloud: true,
        testcontainersCloudTokenSecretName: "TC_CLOUD_TOKEN"
    ],
    'dev'   : [
        api                 : 'https://deployment.apps-internal.dev.datev.de',
        // apps-internal.dev.datev.de (only accessible internally), apps.dev.datev.de (accessible over the public internet)
        routeDomain         : 'apps-internal.dev.datev.de',
        user                : 'cfUserCnRZ',
        vars                : 'vars-dev.yml',
        routes              : [
            testingRoute    : 'refsys-aggregation-processing-service-testing',
            testingTempRoute: 'refsys-aggregation-processing-service-testing-temp',
        ],
        services                     : [
        ]
    ],
    'qs'    : [
        api                 : 'https://deployment.apps-internal.qs.datev.de',
        // apps-internal.qs.datev.de (only accessible internally), apps.qs.datev.de (accessible over the public internet)
        routeDomain         : 'apps-internal.qs.datev.de',
        user                : 'cfUserCnRZQs',
        vars                : 'vars-qs.yml',
        services                     : [
        ]
    ],
    'prod'  : [
        api                 : 'https://api.apps-internal.datev.de',
        // apps-internal.datev.de (only accessible internally), apps.datev.de (accessible over the public internet)
        routeDomain         : 'apps-internal.datev.de',
        user                : 'cfUserCnRZProd',
        vars                : 'vars-prod.yml',
        services                     : [
        ]
    ]
]

def generateChangelog() {
    node('Linux') {
        stage('Generate Changelog on develop') {
            deleteDir()     //workspace reinigen
            checkout scm
            withGitEnv() {
                sh 'git config --global credential.helper cache'
                sh 'git config --global push.default simple'
                sh 'git config --global user.name "Jenkins Automation"'
                sh 'git config --global user.email jenkins-cnrz-refsys-online@datev.de'
                sh 'git fetch --unshallow; ./tools/git-chglog -o CHANGELOG.generated.md 1.0.1..'
                sh 'git add ./CHANGELOG.generated.md; git commit -m "[ci-skip] Update CHANGELOG.generated.md"|| true'
                sh "git push -u origin ${env.BRANCH_NAME} || true"
            }
        }
    }
}

if (env.BRANCH_NAME == 'develop')  {
    generateChangelog()
}

springBootCfApp(settings)
