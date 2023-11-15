Как собрать:
```java
mvn clean package -DskipTests
```
Как запустить:
```java
java -jar performance-test-1.0-SNAPSHOT.jar "jdbc:postgresql://10.0.0.4:5432/postgres?user=test&password=test" 1 1 100000
java -jar performance-test-1.0-SNAPSHOT.jar "jdbc:postgresql://10.0.0.4:5432/postgres?user=test&password=test" 60 1 100000
java -jar performance-test-1.0-SNAPSHOT.jar "jdbc:postgresql://10.0.0.4:5432/postgres?user=test&password=test" 60 60 100000
```
