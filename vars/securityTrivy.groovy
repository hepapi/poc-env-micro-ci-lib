// ───────────────────────────────────────────────────────────────
// vars/securityTrivy.groovy
// Trivy image taraması
// Kullanım: securityTrivy(enable: 'enable')
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {
  def severity   = params.get('severity', 'CRITICAL')
  def registry   = params.get('registry', env.REGISTRY ?: '')
  def repoPath   = params.get('repoPath', env.REPO_PATH ?: '')
  def imageName  = params.get('imageName', env.IMAGE_NAME ?: '')
  def image      = params.get('image', '')

  // version.txt araması
  def versionFile = fileExists('version.txt') ? 'version.txt' :
                    fileExists('srcrepo/version.txt') ? 'srcrepo/version.txt' : null

  if (!versionFile) {
    error "❌ version.txt not found in workspace or srcrepo!"
  }

  if (!image?.trim()) {
    image = sh(
      script: "cat ${versionFile} | xargs -I {} echo \"${registry}/${repoPath}/${imageName}:{}\"",
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
}


