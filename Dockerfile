FROM openjdk:17-jdk-alpine
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /odyssey-prepared
RUN mkdir -p /odyssey-prepared/logs && mkdir -p /odyssey-prepared/libs
RUN chown -R spring: /odyssey-prepared
USER spring:spring
WORKDIR /odyssey-prepared
COPY ./target/odyssey-prepared-1.0-SNAPSHOT.jar /odyssey-prepared/odyssey-prepared-1.0-SNAPSHOT.jar
COPY ./target/libs/* /odyssey-prepared/libs/
ENV JAR_ARGS="arg"
ENTRYPOINT java -jar /odyssey-prepared/odyssey-prepared-1.0-SNAPSHOT.jar ${JAR_ARGS}

