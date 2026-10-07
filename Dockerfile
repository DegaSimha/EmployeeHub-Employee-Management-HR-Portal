FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B verify

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S employeehub && adduser -S employeehub -G employeehub
COPY --from=build --chown=employeehub:employeehub /workspace/target/employeehub-1.0.0.jar app.jar
USER employeehub
EXPOSE 8081
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
