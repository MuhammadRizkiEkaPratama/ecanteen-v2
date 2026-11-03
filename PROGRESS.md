# E-CANTEEN v2 — Progress Proyek

Aplikasi web pemesanan kantin kampus berbasis **Spring Boot + MyBatis**.  
Dibangun ulang dari nol mengikuti materi kuliah J2EE/MyBatis, tanpa design pattern.

---

## Status Keseluruhan: SELESAI ✅

---

## 1. Ringkasan Proyek

E-Canteen adalah sistem pemesanan makanan kantin kampus secara online.  
Versi ini menggunakan arsitektur sederhana yang mudah dibaca dan dimodifikasi oleh mahasiswa,  
sesuai gaya pengajaran dosen J2EE/MyBatis.

**Prinsip utama:**
- Tidak ada design pattern sama sekali (Factory, Strategy, Decorator, dll. → tidak dipakai)
- SQL ditulis manual di file XML (bukan auto-generate ORM)
- Kode sesederhana mungkin, mudah dipahami

---

## 2. Tech Stack

| Komponen | Detail |
|----------|--------|
| Bahasa | Java 17 |
| Framework | Spring Boot 3.5.x |
| ORM | MyBatis (SQL di XML) |
| Database | MySQL — db `ecanteen` |
| View | Thymeleaf + Tailwind CSS (CDN) |
| Auth | BCrypt + HttpSession + LoginInterceptor |
| Build | Maven |
| Port | 8080 |
| Base Package | `com.canteen` |

---

## 3. Arsitektur

```
Controller → Service → Mapper (interface + XML) → MySQL
```

- Tidak ada `dao`, tidak ada `impl`, tidak ada interface service
- Kolom DB: `snake_case` → field Java: `camelCase` (via `map-underscore-to-camel-case=true`)
- Uang menggunakan `BigDecimal`, dibulatkan 2 desimal

---

## 4. Struktur Package

```
src/main/java/com/canteen/
├── entity/          User, Stall, MenuItem, CartItem, CartView
│                    Order, OrderItem, OrderStatusLog
│                    Discount, Review, SecurityLog, VendorApplication
│                    DailySales, SalesReport
├── mapper/          UserMapper, MenuMapper, CartMapper, OrderMapper
├── service/         UserService, MenuService, CartService, OrderService
│                    AdminService, SuperadminService
├── controller/      AuthController, MenuController, CartController
│                    OrderController, AdminController, SuperadminController
├── interceptor/     LoginInterceptor
└── config/          WebConfig, PasswordConfig

src/main/resources/
├── mapper/          UserMapper.xml, MenuMapper.xml, CartMapper.xml, OrderMapper.xml
└── templates/       login.html, register.html, menu.html, cart.html
                     checkout.html, order-confirmation.html, order-history.html
                     admin/  (dashboard, menu-manage, order-manage, dll.)
                     superadmin/  (dashboard, stalls, users, discounts, dll.)
```

---

## 5. Database — 11 Tabel

Lihat `schema.sql` untuk DDL lengkap.

| Tabel | Fungsi |
|-------|--------|
| `users` | Semua akun login (student / admin / superadmin) |
| `stalls` | Data lapak vendor kantin |
| `menu_items` | Item makanan/minuman per stall |
| `cart_items` | Keranjang belanja sementara per user |
| `orders` | Header transaksi (satu order = satu stall) |
| `order_items` | Detail baris per order, harga dibekukan saat checkout |
| `order_status_logs` | Riwayat perubahan status order |
| `discounts` | Diskon global yang diatur admin |
| `reviews` | Ulasan menu oleh student (rating 1–5) |
| `security_logs` | Audit trail login, order, dan aksi admin |
| `vendor_applications` | Pendaftaran vendor baru → disetujui superadmin |

**Sistem diskon:**
- Diskon per-stall → kolom `stalls.discount_percent`
- Diskon global admin → tabel `discounts` (baris dengan `is_active = 1`)
- Keduanya dihitung di Service dengan aritmatika BigDecimal biasa

---

## 6. Role & Hak Akses

| Role | Hak Akses |
|------|-----------|
| `student` | Lihat menu, keranjang, checkout, riwayat order, ulasan |
| `admin` | Kelola menu stall sendiri, update status order masuk |
| `superadmin` | Kelola semua stall & user, diskon global, vendor applications, security log |

---

## 7. Status Modul

| Modul | Status |
|-------|--------|
| Setup project (pom, config, package) | ✅ Selesai |
| M1 — Auth (register, login, logout, session) | ✅ Selesai |
| M2 — Menu (tampil per stall, badge Sold Out) | ✅ Selesai |
| M3 — Cart (add/update/remove, subtotal) | ✅ Selesai |
| M4/M5 — Order + Payment (checkout, diskon, history) | ✅ Selesai |
| M6 — Admin (CRUD menu, advance status order) | ✅ Selesai |
| M7 — Superadmin (diskon, stall, user, vendor apps, log) | ✅ Selesai |
| Fitur tambahan (review, sales report, security log) | ✅ Selesai |

---

## 8. Fitur Lengkap

**Student:**
- Register & login
- Lihat menu semua stall, filter by stall
- Tambah ke keranjang, ubah quantity, hapus item
- Checkout multi-stall (satu order group, per stall terpisah)
- Pilih metode pembayaran (Cash / Transfer)
- Lihat riwayat order & detail
- Beri ulasan (rating + komentar) pada menu yang sudah dipesan

**Admin (Vendor):**
- Dashboard ringkasan penjualan
- CRUD item menu (nama, harga, deskripsi, gambar, stok)
- Buka/tutup stall
- Kelola order masuk: PENDING → PROCESSING → READY → COMPLETED
- Laporan penjualan harian

**Superadmin:**
- Kelola semua user (aktif/nonaktif, ubah role)
- Kelola semua stall (aktif/nonaktif, diskon per-stall)
- Kelola diskon global
- Review & approve/reject vendor applications
- Lihat security log (audit trail seluruh sistem)

---

## 9. Checklist Sebelum Demo / Submit

- [ ] Pastikan DB `ecanteen` sudah dijalankan (`schema.sql`)
- [ ] Buat akun superadmin manual di DB
- [ ] Cek `application.properties` — sesuaikan user/password MySQL
- [ ] Alur demo: login → menu → cart → checkout → admin advance status → superadmin dashboard
- [ ] Pastikan folder `uploads/` ada dan bisa ditulis (untuk upload gambar menu)
