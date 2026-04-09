FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm install

COPY . .

# Пусто = тот же origin, что и SPA (nginx проксирует /api на backend)
ARG VITE_APP_ORIGIN=
ENV VITE_APP_ORIGIN=$VITE_APP_ORIGIN

# Обход «vite: Permission denied» (битый .bin после COPY или CRLF): без shell-обёртки
RUN node ./node_modules/vite/bin/vite.js build

FROM nginx:1.27-alpine

COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

