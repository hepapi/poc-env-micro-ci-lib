// ───────────────────────────────────────────────────────────────
// vars/notifyStatus.groovy
// Pipeline başarı/başarısızlık bildirimleri
// Kullanım: notifyStatus(status: 'success') veya notifyStatus(status: 'failure')
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {
  String status = params.get('status', '')
  String notifyEmail = params.get('email', 'necipulusoyy@gmail.com')

  if (status == 'success') {
    emailext(
      to: notifyEmail,
      subject: "✅ SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
      body: """\
Build başarılı 🎉

Project: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
URL: ${env.BUILD_URL}
"""
    )
  } else if (status == 'failure') {
    emailext(
      to: notifyEmail,
      subject: "❌ FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
      body: """\
Build FAILED ❗

Project: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
Loglar: ${env.BUILD_URL}
"""
    )
  } else {
    echo "ℹ️ No notification sent (unknown status)"
  }
}
