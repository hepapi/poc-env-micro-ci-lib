// ───────────────────────────────────────────────────────────────
// vars/sonarScan.groovy
// SonarQube kod analizi
// Kullanım: sonarScan(enable: 'enable')
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {
  if (params.get('enable', '') != 'enable' && params.get('sonar', '') != 'enable') {
    echo "⏭️ SonarQube scan skipped (not enabled)"
    return
  }

  def projectKey  = params.get('projectKey', env.JOB_NAME)
  def projectName = params.get('projectName', projectKey)

  container('maven') {
    withSonarQubeEnv('sonar-server') {
      withCredentials([string(credentialsId: 'sonar-hepapi', variable: 'SONAR_TOKEN')]) {
        sh """
          mvn clean verify sonar:sonar \
            -DskipTests=false \
            -Ddependency-check.skip=true \
            -Dsonar.projectKey=${projectKey} \
            -Dsonar.projectName=${projectName} \
            -Dsonar.host.url=$SONAR_HOST_URL \
            -Dsonar.login=$SONAR_TOKEN
        """
      }
    }
  }
}
