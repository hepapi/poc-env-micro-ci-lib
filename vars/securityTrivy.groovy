// ───────────────────────────────────────────────────────────────
// vars/securityTrivy.groovy
// Trivy image taraması
// Kullanım: securityTrivy(enable: 'enable')
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {

  if (params.get('enable', '') != 'enable' && params.get('trivy', '') != 'enable') {
    echo "⏭️ Trivy scan skipped (not enabled)"
    return
  }

  def severity = params.get('severity', 'CRITICAL')
  def image    = params.get('image', '')

  if (!image?.trim()) {
    image = sh(
      script: "cat version.txt | xargs -I {} echo \"${env.REGISTRY}/${env.REPO_PATH}/${env.IMAGE_NAME}:{}\"",
      returnStdout: true
    ).trim()
  }

  container('trivy') {
    echo "🔍 Running Trivy scan for image: ${image}"
    sh """
      trivy image --severity ${severity} --exit-code 1 \
      --cache-dir /root/.cache/trivy \
      ${image}
    """
  }

  echo "✅ Trivy scan completed successfully."
}
