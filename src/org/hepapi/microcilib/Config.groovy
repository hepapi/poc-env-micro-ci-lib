package org.hepapi.microcilib

class Config {
  static final String REGISTRY   = "my-nexus-repository-manager.nexus.svc.cluster.local:8082"
  static final String IMAGE_REPO = "repository/nexusimagerepository"
  static final String HELM_REPO  = "http://my-nexus-repository-manager.nexus.svc.cluster.local:8081/repository/nexushelmrepository/"
  static final String CREDS_ID   = "nexus-docker-creds"   // Jenkins Credentials ID (user/pass)
}
