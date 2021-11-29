# QuickStart Example

## Example Instruction

This example illustrates how to use polaris-java for consumer or provider applications to connect to polaris, and complete the service invocation.

## Example

### Connect to polaris

1. Add dependency polaris-all in the pom.xml file in your project.
```
<dependency>
    <groupId>com.tencent.polaris</groupId>
    <artifactId>polaris-all</artifactId>
</dependency>
```

2. Add polaris server address configurations to file /src/main/resources/polaris.yml.
```
global:
  serverConnector:
    addresses:
    - 127.0.0.1:8091
```

### Start Example

1. Start in IDE:
Find main class QuickStartExample in project quickstart-example, and execute the main method with parameters '-namespace \<namespace\> -service \<service_name\>'.

2. Build a jar:
Execute command mvn clean package in project quickstart-example to build a jar，and run command 'java -jar quickstart-example.jar  -namespace \<namespace\> -service \<service_name\>' to start the application.