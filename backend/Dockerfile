# Image générique d'un microservice CSE : docker build --build-arg SERVICE=ms-suivi -f backend/Dockerfile backend
# Étape 1 : compilation Maven (cache des dépendances), étape 2 : JRE minimal, couches Spring Boot, non-root.
FROM maven:3.9-eclipse-temurin-17 AS build
ARG SERVICE
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl ${SERVICE} -am package -DskipTests -Dspotless.check.skip=true \
 && java -Djarmode=tools -jar ${SERVICE}/target/${SERVICE}-*.jar extract --layers --launcher --destination /extract

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S cse && adduser -S cse -G cse && apk add --no-cache wget
WORKDIR /app
COPY --from=build /extract/dependencies/ ./
COPY --from=build /extract/spring-boot-loader/ ./
COPY --from=build /extract/snapshot-dependencies/ ./
COPY --from=build /extract/application/ ./
USER cse
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://localhost:${SERVER_PORT:-8080}/actuator/health/liveness >/dev/null || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
