def call(Map params = [:]) {

    // ✅ Groovy değişkenleri burada tanımlanır
    def SERVICE   = params.service ?: "demo"
    def NEXUS_HELM_REPO = "http://my-nexus-repository-manager.nexus.svc.cluster.local:8081/repository/nexushelmrepository/"

    pipeline {
        agent {
            kubernetes {
                label 'k8s-agent-multi'
                defaultContainer 'helm'
            }
        }

        stages {

            stage('Clone Helm Base and App Values') {
                steps {
                    sh """
                        rm -rf helm-base-common app-values helm-cur-chart
                        git clone https://github.com/hepapi/helm-base-common.git
                        git clone https://github.com/hepapi/app-values.git
                    """
                }
            }

            stage('Prepare Helm Chart') {
                steps {
                    sh """
                        mkdir -p helm-cur-chart/templates
                        cp -r helm-base-common/v1.32/templates/* helm-cur-chart/templates/
                        cp helm-base-common/v1.32/Chart.yaml helm-cur-chart/
                        cp app-values/non-prod/dev/${SERVICE}-values.yaml helm-cur-chart/values.yaml
                    """
                }
            }

            stage('Patch & Push Chart') {
                steps {
                    withCredentials([usernamePassword(credentialsId: 'nexus-docker-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
                        sh """
                            IMAGE_TAG="1.0.${BUILD_NUMBER}"
                            sed -i "s/^  tag:.*/  tag: \$IMAGE_TAG/" helm-cur-chart/values.yaml
                            sed -i "s/^appVersion:.*/appVersion: \$IMAGE_TAG/" helm-cur-chart/Chart.yaml
                            sed -i "s/^version:.*/version: \$IMAGE_TAG/" helm-cur-chart/Chart.yaml

                            helm lint ./helm-cur-chart
                            helm package ./helm-cur-chart
                            FILENAME=\$(ls -t ${SERVICE}-*.tgz | head -n 1)

                            echo "📤 Uploading: \$FILENAME"
                            curl -u "\$NEXUS_USER:\$NEXUS_PASS" --upload-file "\$FILENAME" "${NEXUS_HELM_REPO}"
                        """
                    }
                }
            }
        }
    }
}
