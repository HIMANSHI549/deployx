FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN apt-get update && apt-get install -y --no-install-recommends maven && mvn -q -DskipTests package && rm -rf /var/lib/apt/lists/*

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
