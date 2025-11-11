// ───────────────────────────────────────────────────────────────
// vars/manualApproval.groovy
// Opsiyonel manuel onay ve e-posta bildirim adımı
// Kullanım: manualApproval(approval: 'enable')
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {

  boolean requireApproval = params.get('approval', '') == 'enable'
  String notifyEmail      = params.get('email', 'necipulusoyy@gmail.com')

  stage('Manual Approval Before Helm Push') {
    script {
      if (!requireApproval) {
        echo "🟡 Manual approval skipped (approval != 'enable')"
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

      echo "✅ Manual approval granted, proceeding..."
    }
  }
}
