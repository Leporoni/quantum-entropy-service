#!/bin/sh

# Verifica se estamos rodando no Render (ou se as variáveis de DB foram injetadas)
if [ -n "$DB_HOST" ] && [ "$SPRING_PROFILES_ACTIVE" != "memory" ]; then
    echo "Running in Cloud Environment. Configuring JDBC URL..."
    
    # Monta a URL JDBC correta (sobrescreve qualquer SPRING_DATASOURCE_URL incorreta que venha do ambiente)
    SSL_PARAMS=""
    if [ "$DB_SSL_ENABLED" = "true" ]; then
        SSL_PARAMS="?ssl=true&sslmode=require"
    fi
    export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}${SSL_PARAMS}"
    
    # Garante que usuário e senha também estejam setados
    export SPRING_DATASOURCE_USERNAME="${DB_USER}"
    export SPRING_DATASOURCE_PASSWORD="${DB_PASSWORD}"
    
    echo "JDBC URL configured (Postgres): ${SPRING_DATASOURCE_URL}"
elif [ "$SPRING_PROFILES_ACTIVE" = "memory" ]; then
    echo "Memory Profile Active. Forcing H2 Configuration..."
    # Sobrescreve variáveis de ambiente injetadas pelo Render para garantir uso do H2
    export SPRING_DATASOURCE_URL="jdbc:h2:mem:quantumdb;DB_CLOSE_DELAY=-1"
    export SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.h2.Driver"
    export SPRING_DATASOURCE_USERNAME="sa"
    export SPRING_DATASOURCE_PASSWORD="password"
    echo "JDBC URL configured (H2): ${SPRING_DATASOURCE_URL}"
else
    echo "Running in Local Environment. Using default application.yaml settings."
fi

# Executa o comando passado para o container (inicia a aplicação Java)
exec "$@"
