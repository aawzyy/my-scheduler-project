# HANYA SATU TAHAP: Production
FROM nginx:stable-alpine

# 1. Copy hasil build Vue (dist)
COPY dist /usr/share/nginx/html

# 2. Copy konfigurasi Nginx custom kita (INI BARU)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port 80
EXPOSE 80

# Jalankan Nginx
CMD ["nginx", "-g", "daemon off;"]