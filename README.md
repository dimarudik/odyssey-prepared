# Проверяем prepared statements в yandex odyssey
---

**Готовим миникуб**

1. Preparations:

```
minikube start --cpus=6 --memory=8192m
wget 
```

2. Build project:

```
mvn clean package -DskipTests
```

2. Create image:

```
docker build . -t dimarudik/shard:latest
```

3. Login with your Docker ID to push images:

```
docker login
```

4. Upload image to Docker Hub:

```
docker push dimarudik/shard:latest
```


**Minikube**



---
