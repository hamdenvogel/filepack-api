# Stage 1: compila o JAR (Cloud Build / build local sem mvn prévio)
FROM amazoncorretto:25-alpine AS build
WORKDIR /src
RUN apk add --no-cache maven
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package \
	&& mv target/filepack-api-*.jar /src/app.jar

# Stage 2: imagem final enxuta (mesma ideia do lab AWS)
FROM amazoncorretto:25-alpine
WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
	adduser -u 1001 -S appuser -G appgroup

COPY --from=build /src/app.jar app.jar
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
