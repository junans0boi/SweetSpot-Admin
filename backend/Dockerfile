FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY build/libs/sweetspotadmin-*.jar app.jar
EXPOSE 5002
ENTRYPOINT ["java", "-jar", "/app/app.jar"]