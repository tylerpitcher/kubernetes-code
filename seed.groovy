import groovy.json.JsonSlurper

pipeline {
    agent any
    stages {
        stage('Fetch Repositories') {
            steps {
                script {
                    // Function to fetch repositories with a specific topic
                    def fetchRepositories = { githubToken, username, topic ->
                        def reposUrl = "https://api.github.com/users/${username}/repos"
                        echo "Repos URL: ${reposUrl}"
                        def connection = new URL(reposUrl).openConnection()
                        connection.setRequestProperty("Authorization", "token ${githubToken}")
                        connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                        def response = connection.inputStream.text
                        def repos = new JsonSlurper().parseText(response)
                        
                        // Filter repositories by topic
                        repos.findAll { repo ->
                            def topicsUrl = "${repo.url}/topics"
                            def topicsConnection = new URL(topicsUrl).openConnection()
                            topicsConnection.setRequestProperty("Authorization", "token ${githubToken}")
                            topicsConnection.setRequestProperty("Accept", "application/vnd.github.mercy-preview+json")
                            def topicsResponse = topicsConnection.inputStream.text
                            def topics = new JsonSlurper().parseText(topicsResponse).names
                            topics.contains(topic)
                        }.collect { repo -> repo.name }
                    }
                    
                    // Use Jenkins credentials to get the GitHub token
                    withCredentials([string(credentialsId: 'github-token', variable: 'GITHUB_TOKEN')]) {
                        def username = 'tylerpitcher'
                        def topic = 'deployed'
                        def repositories = fetchRepositories(env.GITHUB_TOKEN, username, topic)
                        
                        // Generate Job DSL scripts for each repository
                        def jobDslScripts = repositories.collect { repoName ->
                            """
                            job('hello_${repoName}') {
                                description('Job to print hello message for ${repoName}')
                                steps {
                                    shell('echo Hello ${repoName}')
                                }
                            }
                            """
                        }.join('\n')

                        // Use the Job DSL plugin to create jobs
                        jobDsl(scriptText: jobDslScripts)
                    }
                }
            }
        }
    }
}
