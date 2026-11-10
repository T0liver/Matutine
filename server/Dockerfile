FROM eclipse-temurin:17-jdk AS build
RUN apt-get update \
    && apt-get install -y --no-install-recommends libatomic1 \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /src
COPY . .
RUN ./gradlew :server:buildFatJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/server/build/libs/*-all.jar /app/server.jar
ENV MATUTINE_DATA=/data
EXPOSE 8080
VOLUME /data
ENTRYPOINT ["java", "-jar", "/app/server.jar"]
