def call(Map params = [:]) {

    final String SERVICE         = (params.service ?: "demo").trim()
    final String CHART_NAME      = "${SERVICE}-dev"
    final String NEXUS_HELM_REPO = "http://my-nexus-repository-manager.nexus.svc.cluster.local:8081/repository/nexushelmrepository/"

    stage('Clone Helm Base & App Values') {
        sh """
            set -e
            rm -rf helm-base-common app-values helm-cur-chart
            git clone https://github.com/hepapi/helm-base-common.git
            git clone https://github.com/hepapi/app-values.git
        """
    }

    stage('Prepare Helm Chart') {
        sh """
            set -e
            mkdir -p helm-cur-chart/templates
            cp -r helm-base-common/v1.32/templates/* helm-cur-chart/templates/
            cp helm-base-common/v1.32/Chart.yaml helm-cur-chart/
            cp app-values/non-prod/dev/${SERVICE}-values.yaml helm-cur-chart/values.yaml
            # Chart adı dosyada CHART_NAME ile güncellenecek
            sed -i "s/^name:.*/name: ${CHART_NAME}/" helm-cur-chart/Chart.yaml
        """
    }

    stage('Patch & Push Chart') {
        withCredentials([usernamePassword(credentialsId: 'nexus-docker-creds', usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
            sh """
                set -e
                IMAGE_TAG="1.0.${BUILD_NUMBER}"

                sed -i "s/^  tag:.*/  tag: \${IMAGE_TAG}/" helm-cur-chart/values.yaml
                sed -i "s/^appVersion:.*/appVersion: \${IMAGE_TAG}/" helm-cur-chart/Chart.yaml
                sed -i "s/^version:.*/version: \${IMAGE_TAG}/" helm-cur-chart/Chart.yaml

                helm lint ./helm-cur-chart || true
                helm package ./helm-cur-chart

                FILENAME=\$(ls -t *.tgz | head -n 1)
                echo "Uploading: \$FILENAME"

                HTTP_STATUS=\$(curl -u "\$NEXUS_USER:\$NEXUS_PASS" \
                  --upload-file "\$FILENAME" \
                  "${NEXUS_HELM_REPO}" \
                  -w "%{http_code}" -o /dev/null -s)

                if [ "\$HTTP_STATUS" -ne 201 ] && [ "\$HTTP_STATUS" -ne 200 ]; then
                  echo "Chart upload failed! HTTP=\$HTTP_STATUS"
                  exit 1
                fi
            """
        }
    }
}
