FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
COPY --from=build /workspace/target/*.jar /app/app.jar
COPY dataset/schemes_with_embeddings.json /app/dataset/schemes_with_embeddings.json

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]