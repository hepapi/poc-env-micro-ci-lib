@Library('poc-env-micro-ci-lib') _

def SERVICE = 'shippingservice'
def ENV = 'dev'

pipeline {
  agent {
    kubernetes {
      label 'k8s-agent-multi'
    }
  }

  stages {

    stage('Docker Build & Push') {
      steps {
        dockerBuildPush(
          service: SERVICE,
          environment: ENV,
          gitRepo: 'https://github.com/hepapi/poc-env-microservices-demo.git',
          gitBranch: 'main',
          dockerfileName: 'Dockerfile',
          contextPath: "src/${SERVICE}"
        )
      }
    }

    stage('Security Scan') {
      steps {
        securityScan(
          trivy: 'enable',
          conftest: 'enable',
          sonar: 'enable'
        )
      }
    }

    stage('Manual Approval') {
      steps {
        manualApproval(status: 'enable')
      }
    }

    stage('Helm Package & Push') {
      steps {
        helmPackagePush(
          service: SERVICE,
          environment: ENV,
          helmValuesFile: "non-prod/${ENV}/${SERVICE}-values.yaml",
          chartName: "${SERVICE}-${ENV}"
        )
      }
    }
  }

  post {
    success {
      notifyStatus(status: 'success')
    }
    failure {
      notifyStatus(status: 'failure')
    }
  }
}
