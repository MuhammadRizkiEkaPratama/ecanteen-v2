# Kirim versi baru E-Canteen ke server web. Jalankan dari folder proyek:
#     .\deploy\deploy.ps1
#
# POLANYA: bangun di laptop, kirim hasilnya, restart layanan di server.
# Namanya "build once, deploy artifact", dan ini yang dipakai di industri.
#
# Kenapa TIDAK membangun di server: Maven di VM 1 GB itu lambat dan bisa
# kehabisan memori di tengah jalan. Laptop sudah punya JDK dan seluruh pustaka
# tersimpan di ~/.m2, jadi membangun di sini hitungan detik.
#
# YANG TIDAK PERNAH DISENTUH SKRIP INI, dan ini disengaja:
#
#   /opt/ecanteen/config/application.properties   sandi & alamat server database
#   /opt/ecanteen/uploads/                        gambar menu yang diunggah
#
# Keduanya milik server, bukan milik kode. Sekali skrip deploy ikut menimpanya,
# server akan mencoba menghubungi localhost:3306 dan berhenti diam-diam, atau
# seluruh gambar menu hilang tanpa ada yang sadar sampai ada yang membuka
# halamannya.

$ErrorActionPreference = "Stop"

$SERVER = "192.168.56.103"
$AKUN   = "labadmin"            # akun SSH biasa, yang punya hak sudo
$TUJUAN = "/opt/ecanteen"

# Pindah ke folder proyek (satu tingkat di atas skrip ini)
Set-Location (Split-Path $PSScriptRoot -Parent)

Write-Host "== 1/4 Membangun JAR ==" -ForegroundColor Cyan
.\mvnw.cmd -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Build gagal. Deploy dibatalkan." }

$jar = Get-ChildItem target\*.jar |
       Where-Object { $_.Name -notlike "*sources*" -and $_.Name -notlike "*javadoc*" } |
       Select-Object -First 1
if (-not $jar) { throw "JAR tidak ditemukan di target\." }
Write-Host ("   {0} ({1} MB)" -f $jar.Name, [math]::Round($jar.Length/1MB,1))

Write-Host "== 2/4 Mengirim ke $SERVER ==" -ForegroundColor Cyan
# Dikirim ke /tmp dulu, bukan langsung ke /opt: akun SSH biasa tidak boleh
# menulis ke /opt, dan memberi hak tulis ke sana hanya supaya scp lancar
# berarti melemahkan folder yang justru harus terjaga.
scp $jar.FullName "${AKUN}@${SERVER}:/tmp/ecanteen-baru.jar"
if ($LASTEXITCODE -ne 0) { throw "Pengiriman gagal. Layanan lama TIDAK disentuh." }

Write-Host "== 3/4 Memasang & menyalakan ulang ==" -ForegroundColor Cyan
# install -o -g -m sekaligus memindahkan berkas DAN menetapkan pemilik serta
# izinnya dalam satu langkah. Memakai `mv` lalu `chown` terpisah menyisakan
# jeda ketika berkasnya sudah di tempat tetapi pemiliknya masih salah.
$perintah = @(
    "sudo install -o ecanteen -g ecanteen -m 644 /tmp/ecanteen-baru.jar $TUJUAN/ecanteen.jar",
    "rm -f /tmp/ecanteen-baru.jar",
    "sudo systemctl restart ecanteen"
) -join " && "
# -t WAJIB. Tanpa itu ssh tidak mengalokasikan terminal semu, dan `sudo`
# menolak bekerja dengan pesan "A terminal is required to authenticate" —
# ia tidak punya tempat untuk menanyakan sandi.
#
# Kalau nanti ingin deploy tanpa mengetik sandi sama sekali, jalannya BUKAN
# membuang -t, melainkan memberi izin sudo tanpa sandi untuk dua perintah ini
# saja lewat /etc/sudoers.d/. Membuat seluruh sudo tanpa sandi berarti satu
# sesi SSH yang bocor langsung berarti root.
ssh -t "${AKUN}@${SERVER}" $perintah
if ($LASTEXITCODE -ne 0) { throw "Pemasangan gagal di server." }

Write-Host "== 4/4 Memeriksa hasilnya ==" -ForegroundColor Cyan
# Diberi waktu naik dulu. Spring Boot butuh beberapa detik, dan bertanya
# terlalu cepat akan selalu dijawab "activating" sehingga pemeriksaannya
# tidak membuktikan apa pun.
Start-Sleep -Seconds 6
# Tanpa -t di sini, karena `systemctl is-active` tidak butuh sudo. Justru
# lebih baik begitu: terminal semu menyisipkan karakter carriage return ke
# keluarannya, dan perbandingan teks di bawah akan gagal tanpa sebab yang
# terlihat. `.Trim()` sebagai penjagaan kedua.
$aktif = (ssh "${AKUN}@${SERVER}" "systemctl is-active ecanteen" | Out-String).Trim()

if ($aktif -eq "active") {
    # Hidup belum tentu melayani. Yang membuktikan aplikasinya benar-benar
    # menjawab adalah permintaan HTTP sungguhan, bukan status systemd.
    try {
        $r = Invoke-WebRequest "http://${SERVER}:8080/login" -TimeoutSec 10 -UseBasicParsing
        Write-Host "   BERHASIL. Layanan aktif, /login menjawab $($r.StatusCode)." -ForegroundColor Green
        Write-Host "   Buka: http://${SERVER}:8080/"
    } catch {
        Write-Host "   Layanan aktif TAPI HTTP belum menjawab: $_" -ForegroundColor Yellow
        Write-Host "   Lihat lognya: ssh ${AKUN}@${SERVER} 'sudo journalctl -u ecanteen -n 50 --no-pager'"
    }
} else {
    Write-Host "   GAGAL. Status layanan: $aktif" -ForegroundColor Red
    Write-Host "   Lihat lognya: ssh ${AKUN}@${SERVER} 'sudo journalctl -u ecanteen -n 50 --no-pager'"
    exit 1
}
