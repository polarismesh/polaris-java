############################################################
# Dockerfile to build polaris-java quickstart example provider

# 1. You need to build the binary from the source code,
# use `mvn clean install` to build the binary.
# 2. You need to copy the quickstart-example-provider-*.jar to this directory
# 3. Replace the ${VERSION} to the real version of the project

############################################################

FROM java:8

ADD ./quickstart-example-provider-${VERSION}.jar /root/app.jar

ENTRYPOINT  ["java","-jar","/root/app.jar"]