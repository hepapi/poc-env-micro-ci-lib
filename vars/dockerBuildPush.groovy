def call(Map params = [:]) {

    // ✅ Environment değişkenleri Script içinde tanımlanır
    def SERVICE   = params.service ?: "demo"
    def REGISTRY  = "my-nexus-repository-manager.nexus.svc.cluster.local:8082"
    def REPO_PATH = "repository/nexusimagerepository"

    pipeline {
        agent {
            kubernetes {
                label 'k8s-agent-multi'
                defaultContainer 'docker'
            }
        }

        stages {

            stage('Checkout') {
                steps {
                    sh """
                        echo '📌 Checking out source...'
                        rm -rf repo || true
                        git clone -b main https://github.com/hepapi/poc-env-microservices-demo.git repo
                        cd repo/src/${SERVICE}
                        echo '✅ Source ready'
                    """
                }
            }

            stage('Login to Nexus') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'nexus-docker-creds', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        sh """
                            echo "🔐 Logging into Nexus..."
                            echo "\$PASS" | docker login "http://${REGISTRY}" -u "\$USER" --password-stdin
                        """
                    }
                }
            }

            stage('Build & Push Image') {
                steps {
                    sh """
                        cd repo/src/${SERVICE}
                        IMAGE_TAG="1.0.${BUILD_NUMBER}"
                        echo "🐳 Building image: ${REGISTRY}/${REPO_PATH}/${SERVICE}:\$IMAGE_TAG"
                        docker build -t "${REGISTRY}/${REPO_PATH}/${SERVICE}:\$IMAGE_TAG" .
                        docker push "${REGISTRY}/${REPO_PATH}/${SERVICE}:\$IMAGE_TAG"
                    """
                }
            }
        }
    }
}
