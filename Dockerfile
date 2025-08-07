FROM dtr.us.aegon.io/tt-web-service/amazonlinux2023-corretto17:latest

COPY target/amazonq-test-repo.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
