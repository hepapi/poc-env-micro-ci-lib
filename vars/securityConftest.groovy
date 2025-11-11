// ───────────────────────────────────────────────────────────────
// vars/securityConftest.groovy
// Dockerfile veya manifest politikaları için OPA (Conftest) taraması
// Kullanım: securityConftest(enable: 'enable')
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {
  if (params.get('enable', '') != 'enable' && params.get('conftest', '') != 'enable') {
    echo "⏭️ Conftest scan skipped (not enabled)"
    return
  }

  container('conftest') {
    echo "🔎 Running Conftest on Dockerfile..."
    sh '''
      if [ -f Dockerfile ] && [ -f dockerfile-conftest.rego ]; then
        conftest test --parser dockerfile --policy dockerfile-conftest.rego Dockerfile || true
      else
        echo "⚠️ Dockerfile or conftest policy not found, skipping Conftest!"
      fi
    '''
  }
}
