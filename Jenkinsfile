pipeline {
    agent {
        docker {
            label 'docker && linux'
            image 'traccar-build:latest'
            args '-v $HOME/.gradle:/home/jenkins/.gradle'
        }
    }
    
    options {
        skipDefaultCheckout()
    }
    
    stages {
        stage('Checkout') {
            steps {
                script {
                    // Checkout main repository
                    checkout scm
                    
                    // Initialize and update submodules to their correct HEAD revision
                    sh '''
                        echo "=== Initializing and updating submodules ==="
                        git submodule init
                        git submodule update --remote --recursive
                        
                        echo "=== Submodule status ==="
                        git submodule status --recursive
                        
                        # Verify traccar-web submodule is checked out correctly
                        if [ -d "traccar-web" ]; then
                            echo "✓ traccar-web submodule exists"
                            cd traccar-web
                            echo "Current HEAD: $(git rev-parse HEAD)"
                            echo "Current branch/ref: $(git describe --always --all)"
                            cd ..
                        else
                            echo "✗ traccar-web submodule missing!"
                            exit 1
                        fi
                    '''
                }
            }
        }
        
        stage('Check Dependencies') {
            steps {
                script {
                    sh '''
                        if ! command -v java >/dev/null 2>&1; then
                            echo "Java is not installed!" >&2
                            exit 1
                        fi
                        if [ ! -f "./gradlew" ]; then
                            echo "Gradle wrapper (./gradlew) is missing!" >&2
                            exit 1
                        fi
                    '''
                }
            }
        }

        stage('Build Traccar Server JAR') {
            steps {
                script {
                    sh './gradlew assemble'
                }
            }
        }

        stage('Build Web') {
            // when {
            //     changeset 'traccar-web'
            // }
            steps {
                sh '''
                    cd traccar-web
                    rm -rf node_modules
                    npm cache clean --force
                    npm ci
                    npm run build
                '''
            }
        }

        stage('Archive Distribution') {
            steps {
                script {
                    sh 'mkdir -p target/dist'

                    def jarPath = fileExists('build/libs/tracker-server.jar') ? 'build/libs/tracker-server.jar' : 'target/tracker-server.jar'
                    def libPath = fileExists('build/libs') ? 'build/libs' : 'target/lib'

                    sh """
                        cp ${jarPath} target/dist/
                        if [ -d "${libPath}" ]; then
                        cp -r ${libPath} target/dist/
                        fi
                        cd target && tar czf traccar-dist.tgz dist
                    """

                    archiveArtifacts artifacts: 'target/traccar-dist.tgz', fingerprint: true
                }
            }
        }
    }

    post {
        failure {
            archiveArtifacts artifacts: 'build/reports/checkstyle/main.html', allowEmptyArchive: true
        }
        success {
            withCredentials([string(credentialsId: 'traccar2-deploy-webhook', variable: 'DEPLOY_TOKEN')]) {
                script {
                    def artifactUrl = "${env.BUILD_URL}artifact/target/traccar-dist.tgz"
                    sh """
                        curl -X POST \
                            -H "Content-Type: application/json" \
                            -H "X-Deploy-Token: ${DEPLOY_TOKEN}" \
                            -d '{"artifactUrl":"${artifactUrl}"}' \
                            https://www.herkenhoff.rocks/hooks/deploy-traccar2
                    """
                }
            }
        }
    }
}
