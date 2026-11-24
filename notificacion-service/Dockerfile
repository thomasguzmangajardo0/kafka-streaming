# --- build: compila el jar con Maven ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# --- runtime: solo JRE, usuario sin privilegios ---
FROM eclipse-temurin:17-jre
RUN useradd --system --create-home app
USER app
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
# Heap acotado para convivir con Kafka y RabbitMQ en una sola instancia EC2
ENV JAVA_OPTS="-Xms128m -Xmx256m"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
