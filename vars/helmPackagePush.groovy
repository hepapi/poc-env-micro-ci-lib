import org.hepapi.microcilib.Config

def call(Map params = [:]) {
  final String service        = (params.service ?: "").trim()
  final String environment    = (params.environment ?: "dev").trim()
  final String chartName      = (params.chartName ?: "${service}-${environment}").trim()
  final String helmValuesFile = (params.helmValuesFile ?: "non-prod/${environment}/${service}-values.yaml").trim()

  final String helmBaseRepo   = (params.helmBaseRepo ?: "https://github.com/hepapi/helm-base-common.git") as String
  final String appValuesRepo  = (params.appValuesRepo ?: "https://github.com/hepapi/app-values.git") as String
  final String helmBasePath   = (params.helmBasePath ?: "v1.32") as String

  final String helmRepoUrl    = (params.helmRepoUrl ?: Config.HELM_REPO) as String
  final String credsId        = (params.credsId     ?: Config.CREDS_ID) as String

  if (!service) {
    error "[helmPackagePush] 'service' parametresi zorunludur."
  }

  try {
    container('helm') {
      sh """
        set -euxo pipefail
        rm -rf helm-base-common app-values helm-cur-chart || true

        git clone '${helmBaseRepo}'
        git clone '${appValuesRepo}'

        mkdir -p helm-cur-chart/templates
        cp -r helm-base-common/'${helmBasePath}'/templates/* helm-cur-chart/templates/
        cp helm-base-common/'${helmBasePath}'/Chart.yaml helm-cur-chart/Chart.yaml
        cp app-values/'${helmValuesFile}' helm-cur-chart/values.yaml

        IMAGE_TAG="1.0.${BUILD_NUMBER}"
        echo "\$IMAGE_TAG" > version.txt
        VERSION=\$(cat version.txt)

        # Chart metadata
        sed -i "s/^name:.*/name: ${chartName}/" helm-cur-chart/Chart.yaml
        sed -i "s/^version:.*/version: \${VERSION}/" helm-cur-chart/Chart.yaml
        sed -i "s/^appVersion:.*/appVersion: \${VERSION}/" helm-cur-chart/Chart.yaml

        # Update image tag
        sed -i "s#\\(tag:\\).*#\\1 \${VERSION}#" helm-cur-chart/values.yaml

        # Update image repo
        sed -i "s#\\(repository:\\).*#\\1 ${Config.REGISTRY}/${Config.IMAGE_REPO}/${service}-${environment}#" helm-cur-chart/values.yaml

        helm lint ./helm-cur-chart || true
        helm package ./helm-cur-chart
      """

      withCredentials([usernamePassword(credentialsId: credsId, usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
        sh """
          set -euxo pipefail
          FILENAME=\$(ls -t ${chartName}-*.tgz | head -n 1)

          echo "Upload: \$FILENAME -> ${helmRepoUrl}"

          HTTP_STATUS=\$(curl -u "\$NEXUS_USER:\$NEXUS_PASS" \
            --upload-file "\$FILENAME" \
            "${helmRepoUrl}" \
            -w "%{http_code}" -o /dev/null -s)

          if [ "\$HTTP_STATUS" != "200" ] && [ "\$HTTP_STATUS" != "201" ]; then
            echo "Chart upload failed (HTTP \$HTTP_STATUS)"
            exit 1
          fi
          echo "Chart uploaded"
        """
      }
    }
  } catch (e) {
    error "[helmPackagePush] Hata: ${e.getMessage()}"
  }
}
