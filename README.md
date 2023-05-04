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
чтобы решить проблему приходится выставлять параметр
```properties
prepareThreshold = 0
```
в результате чего, все преимущество избегания повторных разборов запросов теряется.

**Готовим миникуб**

1. How to start:

```
minikube start --cpus=6 --memory=8192m
wget https://github.com/dimarudik/odyssey-prepared.git
cd odyssey-prepared
helm install app k8s/HelmChart
```

2. Check prepared statements:

```
kubectl get pods

NAME                                 READY   STATUS    RESTARTS      AGE
app-db-689c4c8486-pk6xc              4/4     Running   0             47s
odyssey-prepared1-55bb4d564d-kggg6   1/1     Running   2 (32s ago)   47s
odyssey-prepared2-7ff4676684-w7xwb   1/1     Running   2 (31s ago)   47s
```

---
3. Build project:

```
mvn clean package -DskipTests
docker build -t dimarudik/odyssey-prepared .
```

2. Create image:

```
docker build . -t dimarudik/shard:latest
```


4. Upload image to Docker Hub:

```
docker push dimarudik/shard:latest
```


**Minikube**



---
