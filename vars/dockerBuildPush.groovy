import org.hepapi.microcilib.Config

def call(Map params = [:]) {
  // Zorunlu parametreler
  final String service     = (params.service ?: "").trim()
  final String gitRepo     = (params.gitRepo ?: "").trim()

  // Opsiyonel parametreler
  final String gitBranch      = (params.gitBranch ?: "main") as String
  final String dockerfileName = (params.dockerfileName ?: "Dockerfile") as String
  final String contextPath    = (params.contextPath ?: ".") as String
  final String environment    = (params.environment ?: "dev") as String   

  // Config defaults
  final String registry  = (params.registry ?: Config.REGISTRY) as String
  final String imageRepo = (params.imageRepo ?: Config.IMAGE_REPO) as String
  final String credsId   = (params.credsId   ?: Config.CREDS_ID) as String

  // Validasyon
  if (!service) {
    error "[dockerBuildPush] 'service' parametresi zorunludur."
  }
  if (!gitRepo) {
    error "[dockerBuildPush] 'gitRepo' parametresi zorunludur."
  }

  try {
    container('docker') {
      sh """
        set -euxo pipefail
        rm -rf srcrepo || true
        git clone -b '${gitBranch}' '${gitRepo}' srcrepo

        cd srcrepo/'${contextPath}'
        IMAGE_TAG="1.0.${BUILD_NUMBER}"
        echo "\$IMAGE_TAG" > version.txt
      """

      withCredentials([usernamePassword(credentialsId: credsId, usernameVariable: 'USER', passwordVariable: 'PASS')]) {
        sh """
          echo "\$PASS" | docker login "http://${registry}" -u "\$USER" --password-stdin

          cd srcrepo/'${contextPath}'
          test -f '${dockerfileName}' || { echo "Dockerfile bulunamadı: ${dockerfileName}"; ls -al; exit 1; }

          IMAGE_TAG="\$(cat version.txt)"
          FULL_IMAGE="${registry}/${imageRepo}/${service}-${environment}:\$IMAGE_TAG"

          echo "📦 Build: \$FULL_IMAGE"
          docker build -t "\$FULL_IMAGE" -f "${dockerfileName}" .

          echo "🚀 Push: \$FULL_IMAGE"
          docker push "\$FULL_IMAGE"

          # Export image bilgilerini pipeline environment'a yaz
          echo "REGISTRY=${registry}"   >  build_env.txt
          echo "REPO_PATH=${imageRepo}" >> build_env.txt
          echo "IMAGE_NAME=${service}-${environment}" >> build_env.txt
        """
      }

      // 🔹 Jenkins environment’a import et
      script {
        def envFile = readFile('srcrepo/' + contextPath + '/build_env.txt').trim().split('\n')
        envFile.each {
          def (key, value) = it.split('=')
          env[key.trim()] = value.trim()
        }
      }

      echo "✅ Environment set:"
      echo "   REGISTRY:   ${env.REGISTRY}"
      echo "   REPO_PATH:  ${env.REPO_PATH}"
      echo "   IMAGE_NAME: ${env.IMAGE_NAME}"
    }
  } catch (e) {
    error "[dockerBuildPush] Hata: ${e.getMessage()}"
  }
}
