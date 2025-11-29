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
      subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
      body: """\
Pipeline başarıyla tamamlandı

Project: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
URL: ${env.BUILD_URL}
"""
    )
  } else if (status == 'failure') {
    emailext(
      to: notifyEmail,
      subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
      body: """\
Pipeline başarısız oldu!

Project: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
Loglar: ${env.BUILD_URL}
"""
    )
  } else {
    echo "No notification sent (unknown status)"
  }
}
