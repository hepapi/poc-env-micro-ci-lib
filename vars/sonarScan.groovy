def call(Map params = [:]) {
  def projectKey   = params.get('projectKey', "default-project")
  def projectName  = params.get('projectName', projectKey)

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
