import os
import time
import datetime
import subprocess
from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

# --- KONFIGURASI ---
# GANTI INI DENGAN ID FOLDER GOOGLE DRIVE ANDA
GDRIVE_FOLDER_ID = '13_iV5s05CjCaQfIlO56jyomw-SRmPQAd' 

SCOPES = ['https://www.googleapis.com/auth/drive.file']
SERVICE_ACCOUNT_FILE = '/app/credentials.json'
BACKUP_TIME = "02:00" # Backup otomatis setiap jam 02:00 WIB

def log(msg):
    print(f"[{datetime.datetime.now()}] {msg}")

def authenticate():
    creds = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE, scopes=SCOPES)
    return build('drive', 'v3', credentials=creds)

def create_backup():
    filename = f"backup_{datetime.datetime.now().strftime('%Y%m%d_%H%M%S')}.sql"
    log("⏳ Memulai proses backup database...")
    
    # Perintah pg_dump (mengambil data dari container 'db')
    # PGPASSWORD diambil dari Environment Variable Docker
    cmd = f"pg_dump -h db -U postgres scheduler_db > {filename}"
    
    process = subprocess.run(cmd, shell=True)
    if process.returncode == 0:
        log("✅ Database berhasil didump.")
        return filename
    else:
        log("❌ Gagal melakukan dump database!")
        return None

def upload_to_drive(service, filename):
    log(f"🚀 Mengupload {filename} ke Google Drive...")
    file_metadata = {
        'name': filename,
        'parents': [GDRIVE_FOLDER_ID]
    }
    media = MediaFileUpload(filename, mimetype='application/sql')
    
    file = service.files().create(
        body=file_metadata,
        media_body=media,
        fields='id'
    ).execute()
    
    log(f"🎉 Sukses! File ID: {file.get('id')}")

def main():
    log("🤖 Backup Bot Aktif. Menunggu jam 02:00 WIB...")
    # Tes koneksi awal saat container baru nyala
    try:
        service = authenticate()
        log("✅ Koneksi Google Drive Berhasil!")
    except Exception as e:
        log(f"❌ Koneksi Google Drive GAGAL: {e}")

    while True:
        now = datetime.datetime.now()
        current_time = now.strftime("%H:%M")
        
        if current_time == BACKUP_TIME:
            log("⏰ Waktunya Backup!")
            try:
                service = authenticate()
                filename = create_backup()
                if filename:
                    upload_to_drive(service, filename)
                    os.remove(filename) # Hapus file lokal
                    log("🧹 File lokal dibersihkan.")
            except Exception as e:
                log(f"❌ ERROR BACKUP: {e}")
            
            time.sleep(61) # Tidur 1 menit agar tidak double run
        else:
            time.sleep(30) # Cek waktu setiap 30 detik

if __name__ == "__main__":
    main()