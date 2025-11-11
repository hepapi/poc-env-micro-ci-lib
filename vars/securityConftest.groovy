def call(Map params = [:]) {
  def severity = params.get('severity', 'CRITICAL')
  def image    = params.get('image', '')

  if (!image?.trim()) {
    image = sh(script: "cat version.txt | xargs -I {} echo \"${env.REGISTRY}/${env.REPO_PATH}/${env.IMAGE_NAME}:{}\"", returnStdout: true).trim()
  }

  container('trivy') {
    sh """
      trivy image --severity ${severity} --exit-code 1 \
      --cache-dir /root/.cache/trivy \
      ${image}
    """
  }
}
