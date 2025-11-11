// ───────────────────────────────────────────────────────────────
// vars/manualApproval.groovy
// Opsiyonel manuel onay ve e-posta bildirim adımı
// Kullanım: manualApproval(approval: 'enable')
// ───────────────────────────────────────────────────────────────

// vars/manualApproval.groovy

def call(Map params = [:]) {
  boolean requireApproval = (params.get('approval', '') == 'enable') || (params.get('status', '') == 'enable')
  String notifyEmail      = params.get('email', 'necipulusoyy@gmail.com')

  stage('Manual Approval Before Helm Push') {
    script {
      if (!requireApproval) {
        echo "Manual approval skipped (approval/status != 'enable')"
        return
      }

      // version.txt hem root’ta hem srcrepo’da olabilir
      String versionFile = null
      if (fileExists('version.txt')) {
        versionFile = 'version.txt'
      } else if (fileExists('srcrepo/version.txt')) {
        versionFile = 'srcrepo/version.txt'
      }

      String imageTag = 'N/A'
      if (versionFile) {
        imageTag = sh(script: "cat ${versionFile}", returnStdout: true).trim()
      } else {
        echo "version.txt not found in root or srcrepo — continuing without tag info"
      }

      echo "Sending approval request email..."
      emailext(
        to: notifyEmail,
        subject: "Approval Required: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: """\
Yeni versiyon hazır, deployment onayı bekleniyor.

Proje: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
Image Tag: ${imageTag}
Jenkins URL: ${env.BUILD_URL}

Lütfen Jenkins arayüzünden onay verin.
"""
      )

      timeout(time: 15, unit: 'MINUTES') {
        input message: "Helm chart Nexus'a push edilsin ve ArgoCD tarafından deployment başlatılsın mı?"
      }

      echo "Manual approval granted, proceeding..."
    }
  }
}
