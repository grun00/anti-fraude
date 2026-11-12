# syntax=docker/dockerfile:1
#
# Uma imagem por serviço. O docker-compose.yml escolhe qual com MODULO=transacoes ou MODULO=antifraude.

# 1º estágio: compila com Maven e Java 21.
FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MODULO
WORKDIR /projeto
COPY . .
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -pl ${MODULO} -am package -DskipTests

# 2º estágio: só o Java, para rodar o .jar.
FROM eclipse-temurin:21-jre
ARG MODULO
WORKDIR /app
COPY --from=build /projeto/${MODULO}/target/${MODULO}-*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
