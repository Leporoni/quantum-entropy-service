FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Copiar o jar gerado do estágio de build
COPY --from=build /app/target/*.jar app.jar

# Copiar e configurar o script de entrypoint
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Expor a porta da aplicação (Porta dinâmica no Render)
# EXPOSE 8082

# Definir o entrypoint e o comando padrão
ENTRYPOINT ["/entrypoint.sh"]
CMD ["java", "-jar", "app.jar"]
