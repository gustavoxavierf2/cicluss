# Site único para hospedagem: o Spring Boot serve a API e o frontend já compilado.

# 1) Compila o frontend
FROM node:24-alpine AS front
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2) Empacota o backend com o frontend dentro (resources/static)
FROM maven:3.9-eclipse-temurin-21 AS back
WORKDIR /app
COPY backend/pom.xml ./
RUN mvn -q -B dependency:go-offline
COPY backend/src ./src
COPY --from=front /app/dist ./src/main/resources/static
RUN mvn -q -B package -DskipTests

# 3) Imagem final, só com o Java e o jar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=back /app/target/*.jar app.jar
# Cabe no plano gratuito (512 MB de memória).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
