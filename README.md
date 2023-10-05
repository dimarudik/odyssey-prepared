# Тестируем производительность разных пулеров на одной и той же задаче

Как собрать:
```java
mvn clean package -DskipTests
```
Как запустить:
```java
java -jar target/odyssey-prepared-1.0-SNAPSHOT.jar "jdbc:postgresql://10.0.0.4:5432,10.0.0.4:5432/postgres?targetServerType=primary?user=test&password=test" 1 30
```

