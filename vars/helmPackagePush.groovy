import org.hepapi.microcilib.Config

def call(Map params = [:]) {
  final String service        = (params.service ?: "demo") as String
  final String chartName      = (params.chartName ?: "${service}-dev") as String
  final String helmValuesFile = (params.helmValuesFile ?: "non-prod/dev/${service}-values.yaml") as String

  // Opsiyonel override’lar (istersen değiştir)
  final String helmBaseRepo   = (params.helmBaseRepo ?: "https://github.com/hepapi/helm-base-common.git") as String
  final String appValuesRepo  = (params.appValuesRepo ?: "https://github.com/hepapi/app-values.git") as String
  final String helmBasePath   = (params.helmBasePath ?: "v1.32") as String

  final String helmRepoUrl    = (params.helmRepoUrl ?: Config.HELM_REPO) as String
  final String credsId        = (params.credsId     ?: Config.CREDS_ID) as String

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

        # Version aynı build numarasından üretilecek
        IMAGE_TAG="1.0.${BUILD_NUMBER}"
        echo "\$IMAGE_TAG" > version.txt

        IMAGE_TAG=\$(cat version.txt)
        # values.yaml içindeki image.tag alanı (varsa) güncellenir
        sed -i "s/^\\(\\s*tag:\\).*/\\1 \${IMAGE_TAG}/" helm-cur-chart/values.yaml || true

        # Chart meta
        sed -i "s/^appVersion:.*/appVersion: \${IMAGE_TAG}/" helm-cur-chart/Chart.yaml
        sed -i "s/^version:.*/version: \${IMAGE_TAG}/"     helm-cur-chart/Chart.yaml
        sed -i "s/^name:.*/name: ${chartName}/"            helm-cur-chart/Chart.yaml

        helm lint ./helm-cur-chart || true
        helm package ./helm-cur-chart

        ls -lt
      """

      withCredentials([usernamePassword(credentialsId: credsId, usernameVariable: 'NEXUS_USER', passwordVariable: 'NEXUS_PASS')]) {
        sh """
          set -euxo pipefail
          FILENAME=\$(ls -t ${chartName}-*.tgz | head -n 1)
          echo "📦 Upload: \$FILENAME -> ${helmRepoUrl}"

          HTTP_STATUS=\$(curl -u "\$NEXUS_USER:\$NEXUS_PASS" \
            --upload-file "\$FILENAME" \
            "${helmRepoUrl}" \
            -w "%{http_code}" -o /dev/null -s)

          if [ "\$HTTP_STATUS" != "200" ] && [ "\$HTTP_STATUS" != "201" ]; then
            echo "❌ Chart upload failed (HTTP \$HTTP_STATUS)"
            exit 1
          fi
          echo "✅ Chart uploaded"
        """
      }
    }
  } catch (e) {
    error "[helmPackagePush] Hata: ${e.getMessage()}"
  }
}
