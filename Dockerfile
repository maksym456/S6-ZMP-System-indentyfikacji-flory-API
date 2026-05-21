FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/target/S6-ZMP-System-indentyfikacji-flory-API-1.0.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]