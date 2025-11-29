// ───────────────────────────────────────────────────────────────
// vars/qualityGate.groovy
// SonarQube Quality Gate kontrolü
// Kullanım: sonar: 'enable'
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {
  if (params.get('enable', '') != 'enable' && params.get('sonar', '') != 'enable') {
    echo "⏭Quality Gate check skipped (not enabled)"
    return
  }

  timeout(time: 5, unit: 'MINUTES') {
    waitForQualityGate abortPipeline: true
  }
}
