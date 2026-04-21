# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
# Copy only the pom first to leverage Docker caching for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline
# Copy source and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create the lightweight production image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copy only the built JAR from the first stage
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]