//Docker 镜像仓库信息
registryServer = "192.168.1.200:8088"
projectName = "${JOB_NAME}".split('-')[0]
repoName = "${JOB_NAME}"
imageName = "${registryServer}/${projectName}/${repoName}"


try{
    if ("${runOpts}" == 'GitLab'){
       println("GitLab触发")
    }
}catch(e){
    println("+++++")
    println("手动触发")
    env.branchName = "develop"
}
 



//pipeline
pipeline{
    agent { node { label "master"}}


  //设置构建触发器
    triggers {
        GenericTrigger( causeString: 'Generic Cause', 
                        genericVariables: [   [defaultValue: '', key: 'branchName', regexpFilter: '', value: '$.ref'],
                                              [defaultValue: '', key: 'object_kind', regexpFilter: '', value: '$.object_kind'],
                                              [defaultValue: '', key: 'before', regexpFilter: '', value: '$.before'],
                                              [defaultValue: '', key: 'after', regexpFilter: '', value: '$.after'],
                                          ],        
                        genericRequestVariables: [[key: 'runOpts', regexpFilter: '']],
                        printContributedVariables: true, 
                        printPostContent: true, 
                        regexpFilterExpression: '^push\\s(?!0{40}).{40}\\s(?!0{40}).{40}$', 
                        regexpFilterText: '$object_kind $before $after', 
                        silentResponse: true, 
                        token: 'demo-hello-service')
    }


    stages{
        stage("CheckOut"){
            steps{
                script{
                    println("${branchName}")
                            srcUrl = "http://gitlab.idevops.site/demo/demo-hello-service.git"
                            branchName = branchName - "refs/heads/"
                            currentBuild.description = "Trigger by ${branchName}"
                    println("${branchName}")
                    checkout([$class: 'GitSCM', 
                              branches: [[name: "${branchName}"]], 
                              doGenerateSubmoduleConfigurations: false, 
                              extensions: [], 
                              submoduleCfg: [], 
                              userRemoteConfigs: [[credentialsId: 'gitlab-admin-user',
                                                   url: "${srcUrl}"]]])
                }
            }
        }
        
        stage("Build"){
            steps {
                echo "Build"
            }
        }
        stage("UnitTest"){
            steps {
                echo "Unit Tests"
            }
        }
        
        stage("CodeAnalysis"){
            steps {
                echo "Unit Tests"
            }
        }

        stage("Push Image "){
            steps{
                script{
                    withCredentials([usernamePassword(credentialsId: 'harbor-admin-user', passwordVariable: 'password', usernameVariable: 'username')]) {

                        sh """
                           sed -i -- "s/VER/${branchName}/g" app/index.html
                           docker login --username="${username}" -p ${password} ${registryServer}
                           docker build -t ${imageName}:${branchName}  .
                           docker push ${imageName}:${branchName}
                           docker rmi ${imageName}:${branchName}

                        """
                    }
                }
            }
        }

        stage("Trigger File"){
            steps {
                script{

                    sh """
                        echo IMAGE=${imageName}:${branchName} >trigger.properties
                        cat trigger.properties
                    """
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'trigger.properties', followSymlinks: false
                }
            }
        }

    }
}