FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# pom.xml copié seul d'abord : le cache Docker ne réinvalide la couche
# dependency:go-offline (la plus longue) que si les dépendances changent,
# pas à chaque modification du code source.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests -Dgitbuildhook.install.skip=true package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S sirh && adduser -S sirh -G sirh
COPY --from=build /workspace/target/*.jar app.jar
USER sirh
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
