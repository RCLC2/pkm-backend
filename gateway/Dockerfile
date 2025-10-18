# ---------- Stage 1: Build ----------
FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app

# 의존성 캐시 최적화
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

# 소스 복사 및 빌드
COPY . .
RUN ./gradlew --no-daemon clean bootJar

# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl

# 빌드 산출물 복사
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms128m -Xmx256m -XX:MaxMetaspaceSize=128m -XX:+UseG1GC -Xlog:gc*:file=/logs/g1-gc.log:tags,uptime,time,level -XX:+HeapDumpOnOutOfMemoryError"

RUN mkdir -p /logs && chmod 777 /logs

HEALTHCHECK --interval=15s --timeout=3s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health | grep '"status":"UP"' || exit 1

# Spring Boot 실행
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
