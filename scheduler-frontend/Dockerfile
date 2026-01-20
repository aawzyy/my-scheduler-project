# HANYA SATU TAHAP: Serve dengan Nginx
FROM nginx:stable-alpine

# Copy folder dist yang sudah Anda build di laptop
COPY dist /usr/share/nginx/html

# Copy konfigurasi Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]