# BAKBAN9 — เว็บไซต์ร้านกระเป๋ามือสอง + ระบบแอดมิน

สร้างตามสเปก "BAKBAN9 Website & Admin Spec" และต้นแบบ Canvas BAKBAN Landing Page

- หน้าเว็บสาธารณะ: หน้าแรก (`/`), สมัครสมาชิก (`/signup`), ติดต่อเรา (`/contact`)
- ระบบแอดมิน (`/admin`): Login, ภาพรวม, หน้าเว็บ, สินค้า, แก้ไขสินค้า, สมาชิก, ข้อความ, คลังรูป, Footer, ตั้งค่า
- เนื้อหาทุกส่วนของหน้าเว็บอ่านจากฐานข้อมูลทุกครั้งที่มีการเรียกหน้า แก้ในแอดมินแล้วหน้าเว็บเปลี่ยนทันที
- ไม่มีตะกร้าและระบบชำระเงินในเฟสนี้ ปุ่ม "สนใจใบนี้ ทักเลย" จะพาไป LINE ถ้าใส่ลิงก์ไว้ ถ้าไม่ได้ใส่จะพาไปหน้าติดต่อเราและเติมหัวข้อให้

## Stack

| ส่วน | ใช้ |
|---|---|
| ภาษา / เฟรมเวิร์ก | Java 21, Spring Boot 4.1 (Spring MVC, Spring Security 7, Spring Data JPA / Hibernate 7) |
| หน้าเว็บ | Thymeleaf render ฝั่งเซิร์ฟเวอร์ (ให้ Google ค้นเจอ), JavaScript เท่าที่จำเป็น (ไม่มีเฟรมเวิร์ก) |
| ฐานข้อมูล | PostgreSQL 16 + Flyway migration |
| รูป | Thumbnailator (ย่อเป็น 400/1000 px), TwelveMonkeys (อ่านไฟล์ WEBP), ตัวล้าง SVG ที่เขียนเอง |
| ที่เก็บไฟล์ | ดิสก์ในเครื่อง (dev) หรือ S3 / Cloudflare R2 / MinIO (`STORAGE_TYPE=s3`) |
| อีเมล | Spring Mail (SMTP) ถ้าไม่ได้ตั้งค่า SMTP ระบบจะเขียนอีเมลลง log แทน |
| เทสต์ | JUnit 5, MockMvc, Spring Security Test ทดสอบกับ PostgreSQL จริง |

## เริ่มใช้งาน

ต้องมี Java 21 และ Docker (หรือ PostgreSQL 16) ไม่ต้องติดตั้ง Maven เพราะ `./mvnw` จะดาวน์โหลด Maven ที่ตรงเวอร์ชันมาให้เอง

```bash
docker compose up -d postgres          # หรือใช้ PostgreSQL ที่มีอยู่แล้ว
export OWNER_EMAIL=you@example.com OWNER_PASSWORD='อย่างน้อย-8-ตัว' SESSION_COOKIE_SECURE=false
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

เปิด http://localhost:8080 และ http://localhost:8080/admin

- ตอนเปิดครั้งแรก ระบบจะสร้างตารางและใส่ข้อมูลร้าน หน้าเว็บ 3 หน้า และสินค้า 9 ใบ (`SEED_ENABLED=true` เป็นค่าเริ่มต้น)
- บัญชีเจ้าของร้านคนแรกสร้างจาก `OWNER_EMAIL` / `OWNER_PASSWORD` ระบบจะสร้างให้เฉพาะตอนที่ยังไม่มีผู้ดูแลในระบบเลย ไม่มีรหัสผ่านใดฝังอยู่ในโค้ด
- ค่าที่ตั้งได้ทั้งหมดดูได้ใน `.env.example`

### รันเทสต์

```bash
./mvnw test      # ใช้ฐานข้อมูล bakban9_test (เปลี่ยนได้ด้วย TEST_DATABASE_URL)
```

`docker compose` จะสร้างฐานข้อมูล `bakban9_test` ให้อัตโนมัติ ส่วน CI ใน `.github/workflows/ci.yml` จะรันเทสต์กับ PostgreSQL service

## โครงสร้าง

```
src/main/java/com/bakban9/
  domain/      entity + repository (9 ตารางตามสเปก + admin_invites, audit_logs)
  service/     logic: CatalogService (ค้นหาแบรนด์ + แบ่งหน้า), ContentService + SectionDefs (ส่วนของหน้า),
               MemberService, MessageService, MediaService, ProductService, SettingsService,
               AdminAccountService, DashboardService, AuditService, RateLimiter, RetentionJob, SeedService
  storage/     ตรวจชนิดไฟล์จาก byte, ย่อรูป, ล้าง SVG, เก็บไฟล์ในเครื่อง / S3
  security/    Spring Security, ล็อกบัญชีหลังใส่รหัสผิด 5 ครั้ง, จำกัดการ login ต่อ IP, CSRF แบบ cookie
  web/pub/     controller ของหน้าสาธารณะ
  web/admin/   controller ของแอดมิน
  util/        ราคา, URL, Google Maps embed, CSV
src/main/resources/
  db/migration/V1__schema.sql
  templates/   public/, admin/, fragments/, error/
  static/css/  tokens.css (จาก Design System BAKBAN), public.css, admin.css
  static/js/   public.js (ตรวจฟอร์ม, Preview แบบ Catalog), admin.js
  seed/        โลโก้และรูปสินค้าจากต้นแบบ Canvas
```

`SectionDefs` เป็นตัวกำหนดว่าแต่ละหน้ามีส่วนอะไรบ้าง ส่วนไหนแก้ช่องอะไรได้ และข้อความเริ่มต้นคืออะไร ทั้งหน้าจอแอดมิน "หน้าเว็บ" และตัว seed ใช้ข้อมูลจากที่นี่ที่เดียว

## Design System

- สีทุกค่าอยู่ใน `static/css/tokens.css` ไฟล์อื่นอ้างผ่าน CSS variable เท่านั้น เช่น `var(--color-primary)`
- `DesignTokenLintTest` จะทำให้เทสต์ล้ม ถ้ามีค่าสี (`#hex`, `rgb()`, `hsl()`) อยู่นอก tokens.css หรือมี `style="..."` ใน template

## ความปลอดภัย การเข้าถึง และ PDPA

- **รหัสผ่าน:** hash ด้วย bcrypt (cost 12)
- **Session:** ใช้ cookie แบบ HttpOnly + Secure + SameSite=Lax หมดอายุเมื่อไม่ใช้งาน 2 ชั่วโมง
- **ล็อกบัญชี:** ใส่รหัสผ่านผิด 5 ครั้งจะถูกล็อก 15 นาที เจ้าของร้านปลดล็อกให้ได้ และจำกัดการ login ไว้ที่ 20 ครั้งต่อ 15 นาทีต่อ IP
- **สิทธิ์:** owner ทำได้ทุกอย่าง ส่วน editor แก้เนื้อหาและสินค้าได้ ดูสมาชิกและตอบข้อความได้ แต่จัดการผู้ดูแล ดาวน์โหลด CSV สมาชิก และลบสมาชิกไม่ได้
- **ฟอร์มสาธารณะ:** ตรวจข้อมูลทั้งในเบราว์เซอร์และบนเซิร์ฟเวอร์ มี honeypot กันบอท และจำกัด 5 ครั้งต่อ 10 นาทีต่อ IP โดยนับเฉพาะการส่งที่ผ่านการตรวจแล้ว
- **ไฟล์อัปโหลด:** รับเฉพาะ PNG, JPG, WEBP, SVG ไม่เกิน 5 MB และตรวจชนิดจากเนื้อไฟล์ ไม่ดูแค่นามสกุล SVG จะถูกล้าง script, event handler และลิงก์ภายนอกออกก่อนเก็บ ไฟล์ทุกไฟล์เสิร์ฟพร้อม CSP แบบ sandbox
- **การลบ:** ลบแบบ soft delete และถามยืนยันทุกครั้ง หลังครบ 30 วัน `RetentionJob` จะลบถาวรตอนตี 3 ของทุกวัน
- **PDPA:** ทุกฟอร์มแจ้งวัตถุประสงค์การเก็บข้อมูล (แอดมินแก้ข้อความได้) ช่องยินยอมไม่ติ๊กไว้ก่อน ระบบบันทึกเวลาที่ยินยอม และเจ้าของร้านลบข้อมูลสมาชิกเมื่อมีคำขอได้
- **CSV:** เข้ารหัส UTF-8 with BOM เปิดภาษาไทยใน Excel ได้ และกันการฝังสูตร Excel ในข้อมูล
- **บันทึกการแก้ไข:** ทุกการแก้ไขในแอดมินถูกบันทึกลง `audit_logs` เจ้าของร้านดูได้ที่หน้าตั้งค่า
- **การเข้าถึง:** `lang="th"` ทุกหน้า, รูปมี `alt`, ปุ่มแตะได้อย่างน้อย 44px, เส้น focus 2px, Preview ใช้ `<dialog>` แบบ modal (โฟกัสอยู่ใน popup และปิดด้วย Esc ได้), ข้อความผลลัพธ์อยู่ใน `role="status"`

## ข้อมูลที่ตั้งใจเว้นว่างไว้

ระบบไม่ใส่ค่าสมมติใดๆ ช่องที่ว่างจะไม่แสดงบนหน้าเว็บ และไปขึ้นเป็นรายการในหัวข้อ "งานที่ต้องทำ" ของหน้าภาพรวม

- ราคาสินค้าทั้ง 9 ใบ (หน้าเว็บแสดงว่า "สอบถามราคา")
- แบรนด์ของ 3 ใบที่ยังไม่ยืนยัน รวมถึงสภาพ ขนาด อุปกรณ์ และรายละเอียดของทุกใบ
- ลิงก์ Instagram / LINE / Facebook / TikTok และแผนที่ Google Maps
- เบอร์โทร: ต้นแบบใช้ `+66 90-000-0000` ซึ่งเป็นค่าสมมติ
- โดเมน: `www.Banban.com` ยังรอยืนยัน

รูปสินค้าที่ใส่ไว้ตอนนี้เป็นภาพประกอบจากต้นแบบ ซึ่งยังมีลายน้ำ Printsby.beth ต้องเปลี่ยนเป็นรูปจริงหรือขออนุญาตใช้ก่อนเปิดเว็บ

## งานที่ยังไม่ได้ทำ

- 2FA สำหรับแอดมิน (สเปกบอกว่า "แนะนำ")
- เก็บ session และตัวจำกัดจำนวนครั้งไว้ใน Redis หรือ Spring Session ถ้าจะรันหลาย instance (ตอนนี้เก็บในหน่วยความจำของ instance เดียว)
- ส่งโค้ดส่วนลดอัตโนมัติ: ตอนนี้แอดมินกดบันทึกเองว่า "ส่งแล้ว" ในหน้าสมาชิก (สเปกยังไม่ตัดสินใจเรื่องนี้)
