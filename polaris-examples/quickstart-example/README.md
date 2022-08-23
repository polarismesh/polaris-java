# QuickStart Example

## Example Instruction

This example illustrates how to use polaris-java for consumer or provider applications to connect to polaris, and complete the service invocation.

## Example

### Connect to polaris

1.Make sure you have correctly config `dependencyManagement`，refer：[add-maven-dependency](https://github.com/polarismesh/polaris-java#add-maven-dependency)

2. Add dependency polaris-all in the pom.xml file in your project.
```
<dependency>
    <groupId>com.tencent.polaris</groupId>
    <artifactId>polaris-all</artifactId>
</dependency>
```

3. Add polaris server address configurations to file /src/main/resources/polaris.yml.
```
global:
  serverConnector:
    addresses:
    - 127.0.0.1:8091
```

### Start Example

1. Start in IDE:

- as provider: Find main class `Provider` in project `quickstart-example-provider`, then execute the main method.
- as consumer: Find main class `Consumer` in project `quickstart-example-consumer`, then execute the main method.

2. Build a jar:
- as provider: Execute command `mvn clean package` in project `quickstart-example-provider` to build a jar, then execute the jar with `java -jar ${jar-file}`
- as consumer: Execute command `mvn clean package` in project `quickstart-example-consumer` to build a jar, then execute the jar with `java -jar ${jar-file}`