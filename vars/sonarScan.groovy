// ───────────────────────────────────────────────────────────────
// vars/sonarScan.groovy
// SonarQube kod analizi — SERVICE değişkenini otomatik algılar
// ───────────────────────────────────────────────────────────────

def call(Map params = [:]) {
  if (params.get('enable', '') != 'enable' && params.get('sonar', '') != 'enable') {
    echo "SonarQube scan skipped (not enabled)"
    return
  }

  // Jenkinsfile'daki SERVICE değişkenini otomatik al
  def serviceName = env.SERVICE ?: params.get('service', '')
  if (!serviceName?.trim()) {
    echo "SERVICE environment variable not found! Using JOB_NAME as fallback."
    serviceName = env.JOB_NAME
  }

  // Otomatik Project Key ve Name
  def projectKey  = "${serviceName}".toLowerCase()
  def projectName = "${serviceName}-${env.ENV ?: 'default'}"


  def sourceDir = fileExists("srcrepo/src/${serviceName}/pom.xml") 
    ? "srcrepo/src/${serviceName}" 
    : "srcrepo"

  container('maven') {
    dir(sourceDir) {
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

  echo "SonarQube analysis completed for project: ${projectName}"
}
