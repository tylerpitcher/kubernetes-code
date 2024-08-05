pipeline {
  agent any
  stages {
    stage('SCM') {
      steps {
        git url: "${params.repository}", branch: "main"
      }
    }
    stage('Build Image(s)') {
      steps {
        script {
          def dockerfiles = sh(script: 'find . -name Dockerfile', returnStdout: true).trim().split('\n')
          for (dockerfile in dockerfiles) {
            def dir = dockerfile.replaceFirst(/\/Dockerfile$/, '')
            def imageName = dir
              .replaceFirst(/^\.\//, "${params.serviceName}-")
              .replaceFirst(/^\./, "${params.serviceName}")

            echo "Building Image: ${imageName}"
            
            withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
              def app = docker.build("${USERNAME}/${imageName}", "${dir}")

              docker.withRegistry('https://registry.hub.docker.com', 'dockerhub') {
                app.push("${env.BUILD_NUMBER}")
              }
            }
          }
        }
      }
    }
  }
  post {
    always {
      cleanWs()
    }
  }
}
