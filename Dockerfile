# Versão de Java fixada e única em todo o monorepo (workbox-api, budget-service e esta
# imagem) — ver java.toolchain.languageVersion em build.gradle. Mude nos três lugares
# juntos se atualizar.
ARG JAVA_VERSION=26

FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS build
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies || true
COPY . .
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/build/libs/*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
