FROM openjdk:17-jdk-slim
WORKDIR /app

COPY target/moliya_app.jar moliya_app.jar

EXPOSE 8181

ENTRYPOINT ["java", "-jar", "moliya_app.jar"]


