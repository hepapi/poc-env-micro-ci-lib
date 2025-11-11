def call(Map params = [:]) {

  // Defaults
  boolean runSonar     = params.get('sonar', false)
  boolean runTrivy     = params.get('trivy', false)
  boolean runConftest  = params.get('conftest', false)

  stage("Security Scan") {
    script {

      if (!runSonar && !runTrivy && !runConftest) {
        echo "🔒 Security scan skipped (no scanners enabled)"
        return
      }

      if (runSonar) {
        echo "▶️ Running SonarQube Scan..."
        sonarScan()
        qualityGate()
      }

      if (runConftest) {
        echo "▶️ Running Conftest (OPA policy check)..."
        securityConftest()
      }

      if (runTrivy) {
        echo "▶️ Running Trivy image scan..."
        securityTrivy()
      }
    }
  }
}
