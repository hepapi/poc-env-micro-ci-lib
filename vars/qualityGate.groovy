// ───────────────────────────────────────────────────────────────
// vars/qualityGate.groovy
// SonarQube Quality Gate kontrolü
// Kullanım: qualityGate(enable: 'enable')
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {
  if (params.get('enable', '') != 'enable' && params.get('sonar', '') != 'enable') {
    echo "⏭Quality Gate check skipped (not enabled)"
    return
  }

  timeout(time: 3, unit: 'MINUTES') {
    waitForQualityGate abortPipeline: true
  }
}
