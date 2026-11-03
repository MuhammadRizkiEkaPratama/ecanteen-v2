# E-Canteen v2: menempatkan ke server lab

> Ditulis 29 Juli 2026. Seluruh angka di sini hasil pemeriksaan langsung ke
> proyek dan ke kedua VM, bukan perkiraan.
>
> **✅ SUDAH DIKERJAKAN 29 Juli 2026.** Seluruh langkah 1–8 di bawah selesai
> dan terbukti: aplikasi naik dalam **6,6 detik**, `/login` menjawab **200**,
> dan NSM memantaunya sebagai layanan **#5**. Berkas ini sekarang berfungsi
> sebagai catatan cara memasang ulang dari nol, bukan lagi rencana.
>
> **Tiga hal yang memakan waktu saat pertama dikerjakan**, dicatat supaya tidak
> terulang:
>
> 1. `ssh host "sudo ..."` gagal dengan `A terminal is required to
>    authenticate`. Sudah diperbaiki di `deploy.ps1` dengan `ssh -t`.
> 2. Mendaftarkan layanan ke NSM lewat **Git Bash** gagal dua kali karena
>    `--path /login` diubah jadi `C:/Program Files/Git/login`. Jalankan dari
>    PowerShell, atau awali dengan `MSYS_NO_PATHCONV=1`.
> 3. Maven gagal membangun dengan "Could not transfer artifact" padahal
>    internet hidup. Penyebabnya penanda kegagalan lama di `~/.m2`. Obatnya
>    `mvnw -U`.

Susunannya dua lapis, seperti aplikasi web sungguhan:

```
Laptop (peramban)  ->  192.168.56.103 : 8080   E-Canteen (Spring Boot)
                                   |
                                   v
                       192.168.56.101 : 3306   MySQL, basis data `ecanteen`
```

Untuk membuka E-Canteen, **kedua VM harus hidup**. Server web mati berarti
halamannya tidak ada; server database mati berarti halamannya terbuka tetapi
tidak ada data. Itu bukan kelemahan susunan ini, itu memang sifat aplikasi
berlapis, dan itulah yang dipelajari dari menempatkannya begini.

**NSM tetap berjalan di laptop, di luar keduanya.** Itu disengaja: alat
pemantau tidak boleh mati bersama yang dipantaunya. Kalau NSM ikut ditaruh di
`.103`, crash VirtualBox 29 Juli pukul 07.44 akan menghapus pemantau dan yang
dipantau sekaligus, dan tidak akan ada satu baris pun yang mencatat kejadiannya.

---

## Yang sudah diperiksa dan ternyata TIDAK jadi masalah

Beberapa kekhawatiran awal ternyata tidak berlaku. Ditulis supaya tidak ada
yang membuang waktu mengatasinya:

| Dugaan | Kenyataan |
|---|---|
| Butuh Tomcat 9 karena `javax.servlet` | **Tidak.** Proyek ini memakai `jakarta.servlet` di 11 berkas, `javax` nol. Spring Boot **3.5.15**, bukan J2EE lama |
| Perlu memasang Tomcat di server | **Tidak.** `spring-boot-starter-web` membawa Tomcat di dalam JAR. Satu berkas 54,6 MB, dijalankan dengan `java -jar` |
| MySQL di `.101` perlu dibuka ke jaringan | **Sudah terbuka.** `bind-address = 192.168.56.101`, dan sudah mendengarkan di `192.168.56.101:3306` |
| Perlu membuat hash bcrypt untuk akun pertama | **Tidak.** Ada halaman `/register`; aplikasi yang menghitung hash-nya |

**Yang perlu diperhatikan:**

- `schema.sql` **tidak** memakai `CREATE TABLE IF NOT EXISTS`. Impor sekali
  saja; impor kedua berhenti dengan galat "table already exists".
- Akun bawaan di `schema.sql` **dikomentari**, jadi setelah impor tabel `users`
  kosong. Akun pertama dibuat lewat `/register` (langkah 7).
- Folder `uploads/` sudah berisi gambar sungguhan yang pernah diunggah. Itu
  data, bukan kode, dan harus ikut pindah sekali (langkah 5).

---

## Persiapan: RAM server web

`.103` sekarang punya **956 MB** (jatah 1024 MB), terpakai 290 MB setelah GUI
dimatikan. E-Canteen dengan `-Xmx384m` memakan sekitar 400–500 MB, ditambah
Apache dan sistem. Muat, tetapi tanpa sisa.

Laptop punya 3,24 GB bebas, jadi naikkan jatahnya. VM harus mati dulu:

```powershell
& "C:\Program Files\Oracle\VirtualBox\VBoxManage.exe" modifyvm testUbuntu --memory 1536
```

---

## Langkah 1 — 🔴 KAMU, di server database `.101`

Salin `deploy/buat-db-dan-user.sql` dan `schema.sql` ke sana, lalu:

```bash
# GANTI dulu sandi di dalam buat-db-dan-user.sql sebelum dijalankan
sudo mysql < buat-db-dan-user.sql
sudo mysql ecanteen < schema.sql
```

Periksa hasilnya:

```bash
sudo mysql -e "USE ecanteen; SHOW TABLES;"
```

Harus muncul 11 tabel: `stalls`, `users`, `menu_items`, `cart_items`, `orders`,
`order_items`, `order_status_logs`, `discounts`, `reviews`, `security_logs`,
`vendor_applications`.

---

## Langkah 2 — 🔴 KAMU, di server web `.103`: pasang Java

Aplikasi ini menargetkan Java 17; versi lebih baru tetap menjalankannya.

```bash
sudo apt update && sudo apt install -y openjdk-21-jre-headless
java -version
```

`-jre-headless` cukup, bukan JDK penuh: server ini hanya **menjalankan**, tidak
membangun. Bedanya sekitar 200 MB disk yang tidak perlu.

---

## Langkah 3 — 🔴 KAMU, di `.103`: akun dan folder

```bash
# Akun sistem tanpa shell dan tanpa folder rumah. Ia tidak dipakai untuk login,
# hanya untuk menjalankan satu proses.
sudo useradd --system --no-create-home --shell /usr/sbin/nologin ecanteen

sudo mkdir -p /opt/ecanteen/config /opt/ecanteen/uploads
sudo chown -R ecanteen:ecanteen /opt/ecanteen
```

---

## Langkah 4 — 🔴 KAMU, di `.103`: konfigurasi server

Salin `deploy/application.properties.contoh` ke `.103`, **isi sandinya**
(sandi `ecanteen_app` dari langkah 1), lalu:

```bash
sudo install -o root -g ecanteen -m 640 application.properties /opt/ecanteen/config/application.properties
```

`640` berarti: root boleh mengubah, grup `ecanteen` boleh membaca, pengguna
lain di mesin itu tidak bisa membacanya sama sekali. Sandi basis data tidak
boleh bisa dibaca siapa pun yang kebetulan punya akun di server itu.

**Berkas ini tidak pernah disentuh skrip deploy.** Ia milik server, bukan milik
kode.

---

## Langkah 5 — sekali saja: pindahkan gambar yang sudah diunggah

Dari laptop, di folder proyek:

```bash
scp -r uploads/* redion@192.168.56.103:/tmp/uploads-lama/
```

Lalu di `.103`:

```bash
sudo cp /tmp/uploads-lama/* /opt/ecanteen/uploads/
sudo chown ecanteen:ecanteen /opt/ecanteen/uploads/*
rm -rf /tmp/uploads-lama
```

Hanya sekali. Setelah ini gambar baru diunggah langsung ke server.

---

## Langkah 6 — 🔴 KAMU, di `.103`: daftarkan layanannya

Salin `deploy/ecanteen.service`, lalu:

```bash
sudo install -m 644 ecanteen.service /etc/systemd/system/ecanteen.service
sudo systemctl daemon-reload
sudo systemctl enable ecanteen
```

`enable` yang membuatnya naik lagi sendiri setelah server di-reboot. Tanpa itu,
setiap kali VM dinyalakan kamu harus masuk dan menjalankannya manual — dan itu
bukan layanan, itu program yang kebetulan sedang berjalan.

Belum dinyalakan; JAR-nya belum ada. Langkah berikutnya yang mengirimnya.

---

## Langkah 7 — 🟢 kirim JAR pertama

Dari laptop, di folder proyek:

```powershell
.\deploy\deploy.ps1
```

Skrip itu membangun, mengirim, memasang, menyalakan ulang, lalu **membuktikan
aplikasinya benar-benar menjawab** dengan meminta `/login` lewat HTTP. Status
systemd "active" saja tidak cukup: proses bisa hidup sambil gagal menyambung ke
basis data.

Kalau berhasil, buka **http://192.168.56.103:8080/**

Lalu daftar akun pertama lewat `/register`, dan naikkan perannya jadi
superadmin dari `.101`:

```bash
sudo mysql -e "USE ecanteen; UPDATE users SET role='superadmin' WHERE username='NAMA_YANG_KAMU_DAFTARKAN';"
```

---

## Langkah 8 — daftarkan ke NSM

Inilah gunanya semua ini bagi laporan magangmu: NSM akhirnya memantau aplikasi
sungguhan, bukan dirinya sendiri.

Dari laptop, di `C:\Users\riski\NSM\backend`:

```bash
python set_layanan.py --daftar
```

Lihat bentuk perintah penambahannya di keluaran itu, lalu daftarkan dua layanan
baru: **HTTP di `192.168.56.103:8080`** (dengan path `/login`, karena `/` bisa
saja mengalihkan) dan **MySQL di `192.168.56.101:3306`**.

Nol perubahan kode di NSM. Menambah layanan memang cukup menambah baris data,
dan itu aturan arsitektur yang sudah dipegang sejak awal.

Setelah itu halaman **Lalu Lintas** NSM berhenti menampilkan dirinya sendiri.
Sampai sekarang 360 dari 369 permintaan di sana berasal dari NSM; dengan
E-Canteen berjalan, isinya jadi klien sungguhan dan berkas sungguhan.

---

## Sejak sekarang, mengubah kode

```powershell
.\deploy\deploy.ps1
```

Satu perintah, sekitar 1–2 menit. Bandingkan dengan `mvnw spring-boot:run` di
laptop yang hitungannya detik.

**Karena itu laptop tetap jadi tempat mengembangkan, server jadi tempat
berjalan.** Uji dulu di laptop sampai benar, baru deploy. Jangan mengedit kode
langsung di server — di situ tidak ada Git, tidak ada IDE, dan tidak ada cara
mengembalikan kalau salah.

---

## Kalau bermasalah

| Gejala | Periksa |
|---|---|
| `systemctl is-active` berbunyi `failed` | `sudo journalctl -u ecanteen -n 50 --no-pager` |
| Log berbunyi `Communications link failure` | MySQL `.101` mati, atau sandi di `config/application.properties` salah |
| Log berbunyi `Access denied for user 'ecanteen_app'` | Akun MySQL-nya dibuat untuk host yang salah. Harus `@'192.168.56.103'`, bukan `@'localhost'` |
| Halaman terbuka, gambar menu kosong | `WorkingDirectory=/opt/ecanteen` hilang dari unit systemd, atau folder `uploads/` kosong |
| Gambar hilang setelah deploy | Skrip deploy menyentuh `uploads/`. Seharusnya tidak pernah |
| Aplikasi mati sendiri beberapa menit setelah naik | Kehabisan memori. Cek `free -m`; naikkan jatah RAM VM atau turunkan `-Xmx` |
| Halaman lambat sekali saat pertama dibuka | Wajar. JVM memuat kelas saat permintaan pertama |
