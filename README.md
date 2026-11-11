# 3D Visual Realism Suite

Aplikasi praktikum Grafika Komputer Minggu 11 untuk mengamati rendering permukaan 3D. Project ini melanjutkan struktur project Minggu 10 dan menggunakan Java 17, LWJGL 3, JOML, OpenGL 3.3 Core, serta GLSL.

## Menjalankan

Persyaratan: JDK 17 atau lebih baru, Maven 3.8 atau lebih baru, dan GPU/driver dengan dukungan OpenGL 3.3. Di NetBeans, buka folder ini sebagai Maven project, atur Java Platform ke JDK 17+, lalu jalankan goal `exec:java`. Dari terminal Windows, jalankan `run.bat`. Pada laptop pengembangan yang hanya menyediakan JDK 11, source pernah diverifikasi pada kompatibilitas compiler release 11; project tetap menetapkan Java 17 sehingga menjalankan konfigurasi standar memerlukan JDK 17+.

Project mengunduh LWJGL 3.4.3 dan JOML 1.10.9 dari Maven Central pada build pertama. Profil native dipilih untuk Windows, Linux, dan macOS.

## Kontrol

| Tombol / input | Fungsi |
| --- | --- |
| B | Beralih antara Phong dan Blinn-Phong |
| F | Beralih antara flat shading dan smooth shading |
| Z | Aktif/nonaktifkan Z-buffer depth test |
| C | Aktif/nonaktifkan back-face culling |
| T | Aktif/nonaktifkan pemetaan tekstur |
| M | Ganti nearest dan bilinear mipmapped filtering |
| S | Aktif/nonaktifkan bayangan shadow map |
| A | Aktif/nonaktifkan rotasi otomatis untuk perbandingan statis |
| Seret mouse kiri | Orbit kamera |
| Roda mouse | Zoom kamera |
| R | Reset kamera |
| Esc | Tutup aplikasi |

Mode saat ini dan FPS ditampilkan di judul jendela. Tekstur batu bata dibuat sebagai aset PNG berulang dan dimuat ke GPU menggunakan STBImage.

## Indikator dan implementasi

1. **Shading model:** fragment shader memiliki komponen ambient, diffuse Lambert, dan specular; tombol B memilih persamaan Phong atau Blinn-Phong.
2. **Flat dan smooth shading:** sphere smooth memakai vertex normal unit-sphere yang diinterpolasi; sphere flat menduplikasi vertex per triangle dan menggunakan normal geometri per face.
3. **Hidden surface removal:** `HSRManager` mengatur `GL_DEPTH_TEST`/`GL_LESS` dan `GL_CULL_FACE`/`GL_BACK` dengan front-face `GL_CCW`.
4. **Texture mapping:** sphere, kubus, dan lantai memiliki UV; `Texture2D` memuat PNG lewat STBImage, membangkitkan mipmap, dan membandingkan nearest dengan bilinear mipmapped.
5. **Shadow mapping:** pass pertama merender depth scene dari directional light ke depth-only FBO 2048 × 2048. Pass kedua membandingkan depth dengan slope-scaled bias dan 3 × 3 PCF.

## Struktur sumber

- `VisualRealismSuite.java` mengatur window GLFW, kontrol, kamera, dan dua render pass.
- `GpuMesh.java` membuat sphere smooth/flat, kubus, dan ground plane.
- `ShaderProgram.java`, `HSRManager.java`, `Texture2D.java`, dan `ShadowMapFBO.java` memisahkan resource OpenGL.
- `src/main/resources/shaders/` berisi shader pencahayaan dan depth.
- `src/main/resources/textures/brick.png` adalah tekstur UV yang digunakan aplikasi.
- `ThreeDViewing.java` dan kelas pendukung Minggu 10 tetap disimpan sebagai project awal.

## Catatan pengamatan

Untuk melihat perbedaan satu indikator dengan jelas, ubah satu tombol dalam satu waktu dan amati siluet sphere, kilau permukaan, detail tekstur, serta bayangan di lantai. Matikan depth test atau culling sebentar untuk melihat artefak yang dicegah oleh masing-masing teknik. Shadow bias dapat disesuaikan pada fungsi `shadowVisibility` di `phong.frag`; nilai terlalu rendah menimbulkan shadow acne dan nilai terlalu tinggi dapat menyebabkan peter panning.

## Repository dan laporan

Repository publik: https://github.com/yuzaaa201-dev/visual-realism-minggu11

Laporan DOCX dan PDF tersimpan lokal di folder `hasil_akhir/` dan tidak disertakan di repository. Source publik berisi aplikasi, konfigurasi Maven, aset runtime, skrip peluncuran Windows, dan dokumentasi penggunaan.
