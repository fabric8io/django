#!/usr/bin/groovy
def stage(){
  return stageProject{
    project = 'fabric8io/django'
    useGitTagForNextVersion = true
  }
}

def release(project){
  releaseProject{
    stagedProject = project
    useGitTagForNextVersion = true
    helmPush = false
    groupId = 'io.fabric8.django'
    githubOrganisation = 'fabric8io'
    artifactIdToWatchInCentral = 'components'
    artifactExtensionToWatchInCentral = 'pom'
    promoteToDockerRegistry = 'docker.io'
    dockerOrganisation = 'fabric8'
    imagesToPromoteToDockerHub = []
    extraImagesToTag = null
  }
}

return this;
