# Estágio 1: Build da Aplicação React
FROM node:20-alpine as build

WORKDIR /app

# Copiar arquivos de dependência
COPY package*.json ./

# Instalar dependências
RUN npm install

# Copiar o restante do código fonte
COPY . .

# Aceitar argumento de build para a URL da API
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL

# Gerar o build de produção (cria a pasta /dist)
RUN npm run build

# Estágio 2: Servidor Web Nginx para servir os estáticos
FROM nginx:alpine

# Copiar os arquivos estáticos gerados no estágio anterior
COPY --from=build /app/dist /usr/share/nginx/html

# Copiar nossa configuração customizada do Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expor a porta 80
EXPOSE 80

# Iniciar o Nginx
CMD ["nginx", "-g", "daemon off;"]
