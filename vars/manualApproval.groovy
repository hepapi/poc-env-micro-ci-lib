// ───────────────────────────────────────────────────────────────
// vars/manualApproval.groovy
// Opsiyonel manuel onay ve e-posta bildirim adımı
// Default: approval = false
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {

  boolean requireApproval = params.get('approval', false)
  String notifyEmail       = params.get('email', 'necipulusoyy@gmail.com')

  stage('Manual Approval Before Helm Push') {
    script {
      if (!requireApproval) {
        echo "🟡 Manual approval skipped (approval=false or not set)"
        return
      }

      echo "✉️ Sending approval request email..."

      def IMAGE_TAG = sh(script: "cat version.txt", returnStdout: true).trim()
      emailext(
        to: notifyEmail,
        subject: "Approval Required: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: """\
Yeni versiyon hazır, deployment onayı bekleniyor.

Proje: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
Image Tag: ${IMAGE_TAG}
Jenkins URL: ${env.BUILD_URL}

Lütfen Jenkins arayüzünden onay verin.
"""
      )

      timeout(time: 15, unit: 'MINUTES') {
        input message: "Helm chart Nexus'a push edilsin ve ArgoCD tarafından deployment başlatılsın mı?"
      }
    }
  }

  // Post pipeline notifications
  post {
    success {
      echo "✅ Pipeline başarıyla tamamlandı — Sonar geçti, image & chart Nexus’a gönderildi!"
      emailext(
        to: notifyEmail,
        subject: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: """\
Build başarılı 🎉

Project: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
URL: ${env.BUILD_URL}

Jenkins Bot
"""
      )
    }

    failure {
      echo "❌ Pipeline hata verdi, lütfen logları kontrol et."
      emailext(
        to: notifyEmail,
        subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: """\
Build FAILED ❗

Project: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
Loglar: ${env.BUILD_URL}

Acil kontrol gerekli!
"""
      )
    }
  }
}
