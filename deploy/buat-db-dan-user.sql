-- Penyiapan basis data E-Canteen di SERVER DATABASE (192.168.56.101).
--
-- Dijalankan SEKALI, dari mesin .101 itu sendiri, sebagai root MySQL:
--     sudo mysql < buat-db-dan-user.sql
--
-- Berkas ini TIDAK memuat tabel. Struktur tabelnya ada di schema.sql milik
-- proyek, dan diimpor setelah berkas ini selesai. Dipisah karena keduanya
-- punya sifat berbeda: yang ini soal siapa yang boleh masuk, yang itu soal
-- bentuk datanya.

-- ---------------------------------------------------------------------------
-- 1. Basis datanya
-- ---------------------------------------------------------------------------
-- utf8mb4 disebut eksplisit, bukan dibiarkan ikut bawaan server. Nama menu dan
-- ulasan bisa memuat emoji atau aksara non-Latin, dan utf8 lama (tiga byte) di
-- MySQL akan menolaknya dengan galat yang sulit ditebak asalnya.
CREATE DATABASE IF NOT EXISTS ecanteen
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 2. Akun aplikasi
-- ---------------------------------------------------------------------------
-- BUKAN root, dan bukan '%'.
--
-- Dua pembatasan yang dipasang di sini, dan keduanya penting:
--
--   a) Alamat asalnya dikunci ke 192.168.56.103, yaitu server web. Akun ini
--      tidak bisa dipakai dari mesin lain, termasuk dari laptop, sehingga
--      sandinya yang bocor pun tidak bisa langsung dipakai dari sembarang
--      tempat di jaringan.
--
--   b) Haknya cuma atas basis data `ecanteen`. Kalau aplikasinya kelak
--      disusupi, yang bisa disentuh penyusup hanya data kantin, bukan seluruh
--      basis data di server ini.
--
-- GANTI SANDINYA. Jangan pakai yang tertulis di sini, dan jangan pakai sandi
-- yang sama dengan akun lain di lab ini. Sandi yang dipakai ulang berarti satu
-- kebocoran membuka semuanya sekaligus.
CREATE USER IF NOT EXISTS 'ecanteen_app'@'192.168.56.103'
    IDENTIFIED BY 'GANTI_SANDI_INI';

-- Hak seperlunya saja. Tanpa DROP, tanpa ALTER, tanpa CREATE.
--
-- Aplikasi ini tidak pernah mengubah bentuk tabel saat berjalan; perubahan
-- struktur dilakukan manusia lewat schema.sql. Jadi memberikan hak mengubah
-- struktur berarti menyediakan kemampuan yang tidak pernah dipakai untuk
-- keperluan sah, tetapi sangat berguna bagi penyalahgunaan.
GRANT SELECT, INSERT, UPDATE, DELETE
    ON ecanteen.*
    TO 'ecanteen_app'@'192.168.56.103';

FLUSH PRIVILEGES;

-- ---------------------------------------------------------------------------
-- 3. Periksa hasilnya
-- ---------------------------------------------------------------------------
SELECT user, host FROM mysql.user WHERE user = 'ecanteen_app';
SHOW GRANTS FOR 'ecanteen_app'@'192.168.56.103';

-- ---------------------------------------------------------------------------
-- Kalau perlu mengulang dari awal (HATI-HATI, membuang seluruh data kantin):
--
--   DROP DATABASE ecanteen;
--   DROP USER 'ecanteen_app'@'192.168.56.103';
--
-- Catatan: schema.sql TIDAK memakai "CREATE TABLE IF NOT EXISTS", jadi
-- mengimpornya dua kali akan berhenti dengan galat "table already exists".
-- Itu bukan kerusakan, hanya tanda bahwa tabelnya sudah ada.
-- ---------------------------------------------------------------------------
