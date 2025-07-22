FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN ./mvnw clean package -DskipTests

RUN mkdir -p target && cp $(find . -name "*.jar") target/app.jar

FROM eclipse-temurin:21-jre-jammy

ARG JAR_FILE=target/app.jar

RUN apt-get update && apt-get install -y tzdata && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/${JAR_FILE} app.jar

EXPOSE 8000

ENTRYPOINT ["java", "-jar", "app.jar"]