pipeline {
    agent any

    environment {
        AWS_DEFAULT_REGION = 'ap-south-1'
    }

    stages {
        stage('Clone from GitHub') {
            steps {
                git url: 'https://github.com/godboleayush/rule_engine.git', branch: 'main'
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
