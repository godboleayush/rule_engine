pipeline {
    agent any

    environment {
        AWS_DEFAULT_REGION = 'ap-south-1'
        JAVA_HOME = '/usr/lib/jvm/java-21'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
    }

    stages {
        stage('Clone from GitHub') {
            steps {
                git url: 'https://github.com/godboleayush/rule_engine.git', branch: 'main'
            }
        }

        stage('Build with Maven') {
            steps {
                sh 'mvn clean install'
            }
        }

        stage('Terraform Init') {
            steps {
                sh 'terraform init'
            }
        }

        stage('Terraform Plan') {
            steps {
                sh 'terraform plan'
            }
        }

        stage('Terraform Apply') {
            steps {
                input message: "Apply Terraform changes?"
                sh 'terraform apply -auto-approve'
            }
        }
    }
}
