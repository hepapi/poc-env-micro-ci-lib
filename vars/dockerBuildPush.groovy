import org.hepapi.microcilib.Config

def call(Map params = [:]) {
  // Zorunlu/opsiyonel parametreler
  final String service        = (params.service ?: "demo") as String
  final String gitRepo        = (params.gitRepo ?: "") as String
  final String gitBranch      = (params.gitBranch ?: "main") as String
  final String dockerfilePath = (params.dockerfilePath ?: "Dockerfile") as String
  final String contextPath    = (params.contextPath ?: ".") as String

  final String registry  = (params.registry ?: Config.REGISTRY) as String
  final String imageRepo = (params.imageRepo ?: Config.IMAGE_REPO) as String
  final String credsId   = (params.credsId   ?: Config.CREDS_ID) as String

  if (!gitRepo) {
    error "[dockerBuildPush] 'gitRepo' zorunludur."
  }

  // Bu step, declarative pipeline içindeki stage/steps içinde çağrılır.
  // Kubernetes agent kullanıyorsan, ilgili 'docker' container'ına geçiyoruz.
  try {
    container('docker') {
      sh """
        set -euxo pipefail
        rm -rf srcrepo || true
        git clone -b '${gitBranch}' '${gitRepo}' srcrepo

        cd srcrepo/'${contextPath}'
        IMAGE_TAG="1.0.${BUILD_NUMBER}"
        echo "\$IMAGE_TAG" > version.txt

        # Docker login
      """
      withCredentials([usernamePassword(credentialsId: credsId, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
        sh """
          echo "\$PASS" | docker login "http://${registry}" -u "\$USER" --password-stdin

          cd srcrepo/'${contextPath}'
          test -f '${dockerfilePath}' || { echo "❌ Dockerfile bulunamadı: ${dockerfilePath}"; ls -al; exit 1; }

          IMAGE_TAG="\$(cat version.txt)"
          echo "🐳 Build: ${registry}/${imageRepo}/${service}:\$IMAGE_TAG"
          docker build -t "${registry}/${imageRepo}/${service}:\$IMAGE_TAG" -f "${dockerfilePath}" .

          echo "📤 Push: ${registry}/${imageRepo}/${service}:\$IMAGE_TAG"
          docker push "${registry}/${imageRepo}/${service}:\$IMAGE_TAG"
        """
      }
    }
  } catch (e) {
    error "[dockerBuildPush] Hata: ${e.getMessage()}"
  }
}
