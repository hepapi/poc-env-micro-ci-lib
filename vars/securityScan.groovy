// ───────────────────────────────────────────────────────────────
// vars/securityScan.groovy
// Çoklu güvenlik taraması (Sonar, Conftest, Trivy)
// Kullanım: securityScan(trivy: 'enable', conftest: 'enable', sonar: 'enable')
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {

  boolean runSonar    = params.get('sonar', '') == 'enable'
  boolean runConftest = params.get('conftest', '') == 'enable'
  boolean runTrivy    = params.get('trivy', '') == 'enable'

  stage('Security Scan') {
    script {
      if (!runSonar && !runTrivy && !runConftest) {
        echo "Security Scan skipped (no scanners enabled)"
        return
      }

      if (runSonar) {
        echo "Running SonarQube Scan..."
        sonarScan(enable: 'enable')
        qualityGate(enable: 'enable')
      }

      if (runConftest) {
        echo "Running Conftest (OPA policy check)..."
        securityConftest(enable: 'enable')
      }

      if (runTrivy) {
        echo "Running Trivy image scan..."
        securityTrivy(enable: 'enable')
      }
    }
  }
}
