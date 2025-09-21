# Use a base image with Java installed
FROM openjdk:21-jdk-slim

# Set the working directory in the container
WORKDIR /app

# Copy the Spring Boot JAR file to the container
COPY target/learn-bridge-0.0.1-SNAPSHOT.jar learn-bridge.jar

# Expose the port the app runs on
EXPOSE 8091

# Command to run the app
ENTRYPOINT ["java", "-jar", "learn-bridge.jar"]
