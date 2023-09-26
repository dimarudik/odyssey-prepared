# Тестируем время подключения к бд через пулер и без

Как собрать:
```java
mvn clean package -DskipTests
```
Как запустить:
```java
java -jar target/odyssey-prepared-1.0-SNAPSHOT.jar "jdbc:postgresql://10.0.0.4:5432/postgres?user=test&password=test" 60
```
