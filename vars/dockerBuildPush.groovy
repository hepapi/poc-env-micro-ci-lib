def call(Map params = [:]) {

    // Parametreleri oku
    final String SERVICE   = (params.service ?: "demo").trim()
    final String REGISTRY  = "my-nexus-repository-manager.nexus.svc.cluster.local:8082"
    final String REPO_PATH = "repository/nexusimagerepository"
    final String REPO_URL  = "https://github.com/hepapi/poc-env-microservices-demo.git"
    final String BRANCH    = (params.branch ?: "main").trim()

    stage("Checkout ${SERVICE}") {
        sh """
            set -e
            rm -rf repo || true
            git clone -b ${BRANCH} ${REPO_URL} repo
            cp -R repo/src/${SERVICE}/. .
            rm -rf repo
            ls -al
        """
    }

    stage('Login to Nexus (Docker)') {
        withCredentials([usernamePassword(credentialsId: 'nexus-docker-creds', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
            sh """
                set -e
                echo "\$PASS" | docker login "http://${REGISTRY}" -u "\$USER" --password-stdin
            """
        }
    }

    stage("Build & Push Image (${SERVICE})") {
        sh """
            set -e
            IMAGE_TAG="1.0.${BUILD_NUMBER}"
            echo \$IMAGE_TAG > version.txt

            if [ ! -f Dockerfile ]; then
              echo "Dockerfile bulunamadı"; ls -al; exit 1
            fi

            docker build -t "${REGISTRY}/${REPO_PATH}/${SERVICE}-dev:\$IMAGE_TAG" .
            docker push  "${REGISTRY}/${REPO_PATH}/${SERVICE}-dev:\$IMAGE_TAG"
        """
    }
}
