pipeline {
    agent {
        docker {
            label 'docker && linux'
            image 'eclipse-temurin:17-jdk'
            args '-v $HOME/.gradle:/home/jenkins/.gradle'
        }
    }
    
    options {
        skipDefaultCheckout()
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: scm.branches,
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [
                        [$class: 'SubmoduleOption',
                        disableSubmodules: false,
                        parentCredentials: true,
                        recursiveSubmodules: true,
                        trackingSubmodules: false]
                    ],
                    userRemoteConfigs: scm.userRemoteConfigs
                ])
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
            when {
                changeset 'traccar-web'
            }
            steps {
                sh '''
                    # install node & npm (if not cached in container)
                    curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
                    apt-get update
                    apt-get install -y nodejs

                    cd traccar-web
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
