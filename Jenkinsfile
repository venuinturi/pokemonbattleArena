pipeline {
    agent any

    parameters {
        string(name: 'LOOP_COUNT', defaultValue: '3', description: 'Number of times to run the automation loop')
        string(name: 'CUCUMBER_TAGS', defaultValue: '@trainer', description: 'Cucumber tags to execute (e.g., @trainer, @gym, @hunt)')
    }

    triggers {
        // Runs automatically every night at midnight (Jenkins time)
        cron('H 0 * * *')
    }

    options {
        // Prevent multiple concurrent runs from interfering with each other
        disableConcurrentBuilds()
        // Safety timeout so the job doesn't hang forever
        timeout(time: 6, unit: 'HOURS')
    }

    // tools block removed since Maven is called directly

    stages {
        stage('Checkout') {
            steps {
                // Checkout the repository
                checkout scm
            }
        }

        stage('Compile') {
            steps {
                echo 'Compiling the project...'
                sh '/opt/homebrew/bin/mvn clean test-compile'
            }
        }

        stage('Run Automation Loop') {
            steps {
                script {
                    def loopCount = params.LOOP_COUNT as Integer
                    echo "Starting automation loop for ${loopCount} iterations targeting tags: ${params.CUCUMBER_TAGS}"
                    
                    for (int i = 1; i <= loopCount; i++) {
                        echo "========================================"
                        echo " RUNNING ITERATION: ${i} / ${loopCount}"
                        echo "========================================"
                        
                        // We use catchError so that if an iteration fails (e.g., website timeout), 
                        // the pipeline will continue to the next iteration rather than stopping completely.
                        catchError(buildResult: 'UNSTABLE', stageResult: 'FAILURE') {
                            sh "/opt/homebrew/bin/mvn test -Dcucumber.filter.tags=\"${params.CUCUMBER_TAGS}\""
                        }
                        
                        // Sleep briefly between iterations
                        sleep(time: 30, unit: 'SECONDS')
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Automation Loop Finished.'
            // If you use Cucumber reporting plugins in Jenkins, you can archive them here:
            // cucumber buildStatus: 'UNSTABLE', fileIncludePattern: '**/cucumber.json'
        }
        success {
            echo 'All iterations completed successfully!'
        }
        unstable {
            echo 'Some iterations failed or had errors.'
        }
        failure {
            echo 'Pipeline failed!'
        }
    }
}
