from node:24-alpine as build
workdir /app

copy package.json package-lock.json ./
run npm ci

copy . .
run npm run build-only

from nginx:alping
copy nginx.conf /etc/nginx/conf.d/default.conf
copy --from=build /app/dist /usr/share/nginx/html
expose 80