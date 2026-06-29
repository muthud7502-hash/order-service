pipeline {
    agent {
        label 'slave_group'
    }

    environment {
        DOCKER_IMAGE = "muthu2699/order-service"
        SONAR_URL = "http://52.13.79.87:9000"
        NEXUS_URL = "http://52.13.79.87:8081"
        GIT_REPO = "https://github.com/muthud7502-hash/order-service.git"
        GITOPS_REPO = "https://github.com/muthud7502-hash/gitops-repo.git"
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main', url: "${GIT_REPO}"
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Unit Test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar-new') {
                    sh """
                        mvn sonar:sonar \
                        -Dsonar.projectKey=order-service \
                        -Dsonar.host.url=${SONAR_URL}
                    """
                }
            }
        }

        stage('Upload Artifact To Nexus') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'nexus',
                        usernameVariable: 'NEXUS_USER',
                        passwordVariable: 'NEXUS_PASS'
                    )
                ]) {
                    sh """
                        mvn deploy -DskipTests \
                        -DaltDeploymentRepository=nexus::default::${NEXUS_URL}/repository/ecomm-releases/
                    """
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh """
                    docker build -t ${DOCKER_IMAGE}:${BUILD_NUMBER} .
                    docker tag ${DOCKER_IMAGE}:${BUILD_NUMBER} ${DOCKER_IMAGE}:latest
                """
            }
        }

        stage('Docker Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )
                ]) {
                   sh '''
                        echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin
                        docker push ${DOCKER_IMAGE}:${BUILD_NUMBER}
                        docker push ${DOCKER_IMAGE}:latest
                    '''
                }
            }
        }

        stage('Update GitOps Repo') {
            steps {
                withCredentials([
                    string(
                        credentialsId: 'github-token',
                        variable: 'GIT_TOKEN'
                    )
                ]) {
                    sh """
                        rm -rf gitops-repo

                        git clone https://${GIT_TOKEN}@github.com/muthud7502-hash/gitops-repo.git

                        cd gitops-repo

                        sed -i 's|image:.*|image: muthu2699/order-service:${BUILD_NUMBER}|g' order-service/dev/deployment.yaml

                        git config --global user.email "jenkins@local.com"
                        git config --global user.name "Jenkins"

                        git add .
                        git commit -m "Update order-service image ${BUILD_NUMBER}" || true
                        git push
                    """
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline SUCCESS"
        }

        failure {
            echo "Pipeline FAILED"
        }
    }
}
