# Проверяем prepared statements в Odyssey от Yandex

При работе через механизм prepared statements с базой Postgresql через пулер pgbouncer в транзакционном режиме мы cталкиваемся с ошибкой вида:
```java
org.postgresql.util.PSQLException: ERROR: prepared statement "S_1" already exists
	at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2713) ~[postgresql-42.6.0.jar:42.6.0]
	at org.postgresql.core.v3.QueryExecutorImpl.processResults(QueryExecutorImpl.java:2401) ~[postgresql-42.6.0.jar:42.6.0]
	at org.postgresql.core.v3.QueryExecutorImpl.execute(QueryExecutorImpl.java:368) ~[postgresql-42.6.0.jar:42.6.0]
	at org.postgresql.core.v3.QueryExecutorImpl.execute(QueryExecutorImpl.java:327) ~[postgresql-42.6.0.jar:42.6.0]
	at org.postgresql.jdbc.PgConnection.executeTransactionCommand(PgConnection.java:965) ~[postgresql-42.6.0.jar:42.6.0]
	at org.postgresql.jdbc.PgConnection.commit(PgConnection.java:987) ~[postgresql-42.6.0.jar:42.6.0]
	at org.example.App.main(App.java:29) [odyssey-prepared-1.0-SNAPSHOT.jar:?]
```
в логах Postgresql получаем:
```java
2023-05-04 10:45:08.915 MSK [62] DETAIL:  parameters: $1 = '1'
2023-05-04 10:45:08.915 MSK [62] LOG:  duration: 0.011 ms
2023-05-04 10:45:09.922 MSK [62] ERROR:  prepared statement "S_1" already exists
2023-05-04 10:45:09.922 MSK [62] STATEMENT:  COMMIT
```
чтобы решить проблему приходится, на уровне драйвера, выставлять параметр
```properties
prepareThreshold = 0
```
<p>в результате чего, все преимущество отсутствия повторных разборов запросов теряется.
На каждый запрос Postgresql делает полный разбор.</p> 

<p>Решением данной проблемы станет фича от пулера Odyssey от Яндекса. 
<br>В основу решения вошло использование двух хеш таблиц, где сопостовляются существующие prepared statements в установленных соединениях от пулера к базе, вновь приходящим сессиям, где в транзакциях есть prepared statement. 
Также существуют механизм удаления устаревших малоиспользуемых запросов. 
Как проверить, что механизм работает:</p>

---

### Готовим миникуб и окружение

```
minikube start --cpus=6 --memory=8192m
wget https://github.com/dimarudik/odyssey-prepared.git
cd odyssey-prepared
helm install app k8s/HelmChart
```
**Как результат, получаем одну базу и три пулера**

Два pgbouncer'а (транзакционный и сессионный) и один odyssey (транзакционный). Отрываем для них порты с хоста в отдельном терминале.

```
kubectl port-forward service/pgbouncer-transaction 6432:6432
kubectl port-forward service/pgbouncer-session 7432:7432
kubectl port-forward service/odyssey-transaction 8432:8432
```

**Собираем проект**

Это простое приложение, которое в цикле открывает новое соединение и делает одну транзакцию с prepared statement.
Есть задержка как внутри транзакции, так и после комита.

```
mvn clean package -DskipTests
```

### Проводим тесты

**PGBOUNCER TRANSACTIONAL**

```
java -jar target/odyssey-prepared-1.0-SNAPSHOT.jar "jdbc:postgresql://localhost:6432/postgres?user=test&password=test"
```
На втором цикле пытаемся переиспользовать prepared statement с новым подключением к pgbouncer. 
<br>И, хотя мы попадем на тот же бэкенд, соединение у нас новое и в сессии информации о prepared statement для этого соединения не осталось.
Получаем ошибку и PGBOUNCER перекидывает нас на новый backend при новом подключении. 

```java
12:33:52.918 [main] INFO  org.example.App - BEGIN
12:33:52.933 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
12:33:52.933 [main] INFO  org.example.App - 290
12:33:52.933 [main] INFO  org.example.App - Sleeping in transaction...
12:33:53.937 [main] INFO  org.example.App - COMMIT
12:33:53.937 [main] INFO  org.example.App - Sleeping out of transaction...
12:33:56.965 [main] INFO  org.example.App - BEGIN
12:33:56.972 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
12:33:56.972 [main] INFO  org.example.App - 290
12:33:56.972 [main] INFO  org.example.App - Sleeping in transaction...
12:33:57.978 [main] ERROR org.example.App - {}
org.postgresql.util.PSQLException: ERROR: prepared statement "S_1" already exists
	at org.postgresql.core.v3.QueryExecutorImpl.receiveErrorResponse(QueryExecutorImpl.java:2713) ~[postgresql-42.6.0.jar:42.6.0]
	at org.postgresql.core.v3.QueryExecutorImpl.processResults(QueryExecutorImpl.java:2401) ~[postgresql-42.6.0.jar:42.6.0]
	at org.postgresql.core.v3.QueryExecutorImpl.execute(QueryExecutorImpl.java:368) ~[postgresql-42.6.0.jar:42.6.0]
	at org.postgresql.core.v3.QueryExecutorImpl.execute(QueryExecutorImpl.java:327) ~[postgresql-42.6.0.jar:42.6.0]
	at org.postgresql.jdbc.PgConnection.executeTransactionCommand(PgConnection.java:965) ~[postgresql-42.6.0.jar:42.6.0]
	at org.postgresql.jdbc.PgConnection.commit(PgConnection.java:987) ~[postgresql-42.6.0.jar:42.6.0]
	at org.example.App.main(App.java:29) [odyssey-prepared-1.0-SNAPSHOT.jar:?]
12:33:58.007 [main] INFO  org.example.App - BEGIN
12:33:58.013 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
12:33:58.013 [main] INFO  org.example.App - 291
12:33:58.013 [main] INFO  org.example.App - Sleeping in transaction...
12:33:59.023 [main] INFO  org.example.App - COMMIT
12:33:59.023 [main] INFO  org.example.App - Sleeping out of transaction...
```

**ODYSSEY TRANSACTIONAL (одно приложение)**

```
java -jar target/odyssey-prepared-1.0-SNAPSHOT.jar "jdbc:postgresql://localhost:8432/postgres?user=test&password=test"
```
Ошибка не воспроизводится
```
12:44:26.575 [main] INFO  org.example.App - BEGIN
12:44:26.589 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
12:44:26.589 [main] INFO  org.example.App - 315
12:44:26.589 [main] INFO  org.example.App - Sleeping in transaction...
12:44:27.597 [main] INFO  org.example.App - COMMIT
12:44:27.597 [main] INFO  org.example.App - Sleeping out of transaction...
12:44:30.629 [main] INFO  org.example.App - BEGIN
12:44:30.634 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
12:44:30.634 [main] INFO  org.example.App - 315
12:44:30.634 [main] INFO  org.example.App - Sleeping in transaction...
12:44:31.645 [main] INFO  org.example.App - COMMIT
12:44:31.645 [main] INFO  org.example.App - Sleeping out of transaction...
```

**ODYSSEY TRANSACTIONAL (два приложения)**

Тут будут два варианта. 
1. Когда второе приложение попытается переиспользовать prepared statement, которое уже переиспользуется в открытой транзакции.
2. Когда второе приложение попытается переиспользовать prepared statement, которое никто не использует.

**Вариант 1**

В двух терминалах запускаем
```java
java -jar target/odyssey-prepared-1.0-SNAPSHOT.jar "jdbc:postgresql://localhost:8432/postgres?user=test&password=test"
```

Логи первого
```java
13:03:01.679 [main] INFO  org.example.App - BEGIN
13:03:01.684 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
13:03:01.684 [main] INFO  org.example.App - 315
13:03:01.684 [main] INFO  org.example.App - Sleeping in transaction...
13:03:02.689 [main] INFO  org.example.App - COMMIT
13:03:02.689 [main] INFO  org.example.App - Sleeping out of transaction...
13:03:05.720 [main] INFO  org.example.App - BEGIN
13:03:05.724 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
13:03:05.724 [main] INFO  org.example.App - 353
13:03:05.724 [main] INFO  org.example.App - Sleeping in transaction...
13:03:06.733 [main] INFO  org.example.App - COMMIT
```

Логи второго
```java
13:03:02.542 [main] INFO  org.example.App - BEGIN
13:03:02.554 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
13:03:02.554 [main] INFO  org.example.App - 353
13:03:02.555 [main] INFO  org.example.App - Sleeping in transaction...
13:03:03.565 [main] INFO  org.example.App - COMMIT
13:03:03.565 [main] INFO  org.example.App - Sleeping out of transaction...
13:03:06.597 [main] INFO  org.example.App - BEGIN
13:03:06.602 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
13:03:06.602 [main] INFO  org.example.App - 315
13:03:06.602 [main] INFO  org.example.App - Sleeping in transaction...
13:03:07.608 [main] INFO  org.example.App - COMMIT
```

Мы пытаемся переиспользовать prepared statement, но не можем, так как prepared statement уже используется в сессии с открытой транзакцией.
Поэтому получаем новый backend.

**Вариант 2**

В двух терминалах запускаем
```java
java -jar target/odyssey-prepared-1.0-SNAPSHOT.jar "jdbc:postgresql://localhost:8432/postgres?user=test&password=test"
```

Логи первого
```java
12:57:10.008 [main] INFO  org.example.App - BEGIN
12:57:10.019 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
12:57:10.020 [main] INFO  org.example.App - 315
12:57:10.020 [main] INFO  org.example.App - Sleeping in transaction...
12:57:11.028 [main] INFO  org.example.App - COMMIT
12:57:11.028 [main] INFO  org.example.App - Sleeping out of transaction...
12:57:14.059 [main] INFO  org.example.App - BEGIN
12:57:14.064 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
12:57:14.064 [main] INFO  org.example.App - 315
12:57:14.064 [main] INFO  org.example.App - Sleeping in transaction...
12:57:15.072 [main] INFO  org.example.App - COMMIT
12:57:15.072 [main] INFO  org.example.App - Sleeping out of transaction...
```
Логи второго
```java
12:57:12.372 [main] INFO  org.example.App - BEGIN
12:57:12.385 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
12:57:12.385 [main] INFO  org.example.App - 315
12:57:12.385 [main] INFO  org.example.App - Sleeping in transaction...
12:57:13.397 [main] INFO  org.example.App - COMMIT
12:57:13.397 [main] INFO  org.example.App - Sleeping out of transaction...
12:57:16.429 [main] INFO  org.example.App - BEGIN
12:57:16.432 [main] INFO  org.example.App - execute <unnamed>: select pg_backend_pid() where 1 = $1
12:57:16.432 [main] INFO  org.example.App - 315
12:57:16.432 [main] INFO  org.example.App - Sleeping in transaction...
12:57:17.441 [main] INFO  org.example.App - COMMIT
12:57:17.441 [main] INFO  org.example.App - Sleeping out of transaction...
```

Prepared statement переиспользуется на одном и том же бэкенде.

---