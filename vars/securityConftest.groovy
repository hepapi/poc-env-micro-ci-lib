// ───────────────────────────────────────────────────────────────
// vars/securityConftest.groovy
// Conftest (OPA) policy kontrolü
// Kullanım: securityConftest(enable: 'enable')
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {
  def enable = params.get('enable', 'disable')

  if (enable != 'enable') {
    echo "Conftest disabled (enable != 'enable')"
    return
  }

  echo "Running Conftest (OPA policy check)..."

  container('conftest') {

    // Hem root hem srcrepo altında arama
    def rootDockerfileExists     = fileExists('Dockerfile')
    def rootPolicyExists         = fileExists('dockerfile-conftest.rego')
    def srcrepoDockerfileExists  = fileExists('srcrepo/Dockerfile')
    def srcrepoPolicyExists      = fileExists('srcrepo/dockerfile-conftest.rego')

    if (rootDockerfileExists && rootPolicyExists) {
      echo "Found Dockerfile and policy in root"
      sh '''
        set -euxo pipefail
        conftest test --parser dockerfile --policy dockerfile-conftest.rego Dockerfile || true
      '''
    } else if (srcrepoDockerfileExists && srcrepoPolicyExists) {
      echo "Found Dockerfile and policy in srcrepo/"
      dir('srcrepo') {
        sh '''
          set -euxo pipefail
          conftest test --parser dockerfile --policy dockerfile-conftest.rego Dockerfile || true
        '''
      }
    } else {
      echo "Dockerfile or dockerfile-conftest.rego not found — skipping Conftest!"
      sh 'ls -al || true'
      sh 'ls -al srcrepo || true'
    }
  }
}
