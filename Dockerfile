# Build stage
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/target/reservahub-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
