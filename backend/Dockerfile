FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY build/libs/sweetspotadmin-*.jar app.jar
EXPOSE 5007
ENTRYPOINT ["java", "-jar", "/app/app.jar"]