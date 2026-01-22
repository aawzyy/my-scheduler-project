FROM python:3.9-slim

# Install PostgreSQL Client (untuk perintah pg_dump)
RUN apt-get update && apt-get install -y postgresql-client && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Library Google Drive
RUN pip install --no-cache-dir google-api-python-client google-auth-httplib2 google-auth-oauthlib

# Copy Script & Credentials
COPY backup.py .
COPY credentials.json .

# Jalankan Script tanpa buffering (agar log langsung muncul di Portainer)
CMD ["python", "-u", "backup.py"]