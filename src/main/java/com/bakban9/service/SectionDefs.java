package com.bakban9.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The editable sections of each public page: which fields the admin "หน้าเว็บ" screen shows
 * and the initial copy that the seeder writes (taken from the Canvas prototype).
 */
public final class SectionDefs {

    public enum FieldType { TEXT, TEXTAREA, NUMBER, BOOLEAN }

    public record FieldDef(String key, String label, FieldType type, int maxLength, String defaultValue) {}

    /**
     * @param fixed  section keeps its place (no reorder) — e.g. the header menu, page intros
     * @param imageSlots number of images the section shows (picked from the media library)
     * @param editsMapUrl the section edits site_settings.map_url instead of its own content
     */
    public record SectionDef(String page, String key, String label, List<FieldDef> fields, int imageSlots,
                             String note, boolean fixed, boolean editsMapUrl) {

        public Map<String, String> defaults() {
            Map<String, String> m = new LinkedHashMap<>();
            fields.forEach(f -> m.put(f.key(), f.defaultValue()));
            return m;
        }

        public Optional<FieldDef> field(String key) {
            return fields.stream().filter(f -> f.key().equals(key)).findFirst();
        }
    }

    public record PageDef(String slug, String label, String path, List<SectionDef> sections) {}

    private static FieldDef text(String key, String label, String def) {
        return new FieldDef(key, label, FieldType.TEXT, 200, def);
    }

    private static FieldDef area(String key, String label, String def) {
        return new FieldDef(key, label, FieldType.TEXTAREA, 1000, def);
    }

    private static final String SIGNUP_TITLE = "สมัครสมาชิก รับโค้ดส่วนลด";
    private static final String SIGNUP_BODY = "กรอกชื่อกับอีเมล แล้วรอรับโค้ดไปช้อปใบที่ใช่ได้เลย";
    private static final String SIGNUP_PURPOSE = "เราเก็บชื่อ อีเมล และเบอร์โทร (ถ้ากรอก) เพื่อส่งโค้ดส่วนลดและติดต่อเรื่องการสั่งซื้อ "
            + "ส่วนข่าวสารและโปรโมชันจะส่งให้เฉพาะเมื่อคุณติ๊กยินยอม ขอลบข้อมูลได้ทุกเมื่อผ่านหน้าติดต่อเรา";

    public static final List<PageDef> PAGES = List.of(
            new PageDef("home", "หน้าแรก", "/", List.of(
                    new SectionDef("home", "menu", "เมนูด้านบน", List.of(
                            text("products", "เมนูที่ 1", "สินค้า"),
                            text("signup", "เมนูที่ 2", "สมัครสมาชิก"),
                            text("contact", "เมนูที่ 3", "ติดต่อเรา")),
                            0, "ใช้กับเมนูด้านบนของทุกหน้า", true, false),
                    new SectionDef("home", "hero", "Hero", List.of(
                            text("eyebrow", "ข้อความเล็กด้านบน", "กระเป๋ามือสอง คัดเกรดพรีเมียม"),
                            text("headline", "พาดหัว", "กระเป๋ามือสองสุดคิ้วท์ แบกความสดใสไปกับคุณ"),
                            area("body", "คำอธิบาย", "วัยรุ่นใส่ได้ วินเทจใส่ดี รวมงานแบรนด์เนมและงานวินเทจหายาก ราคา 300–30,000 บาท"),
                            text("cta", "ข้อความบนปุ่ม", "ช้อปกระเป๋าสุดคิ้วท์")),
                            3, "รูปแรกเป็นรูปใหญ่ อีก 2 รูปเป็นรูปเล็ก", false, false),
                    new SectionDef("home", "products", "สินค้าแนะนำ", List.of(
                            text("title", "หัวข้อ", "สินค้าแนะนำ"),
                            new FieldDef("perPage", "จำนวนสินค้าต่อหน้า", FieldType.NUMBER, 2, "6"),
                            text("searchPlaceholder", "ข้อความในช่องค้นหา", "ค้นหาแบรนด์ เช่น Dior"),
                            text("searchButton", "ข้อความบนปุ่มค้นหา", "Search Brand"),
                            text("emptyText", "ข้อความเมื่อไม่เจอแบรนด์", "ไม่เจอแบรนด์นี้ ลองคำอื่นดูนะ"),
                            text("contactCta", "ปุ่มใน Preview", "สนใจใบนี้ ทักเลย")),
                            0, "แก้ไขตัวสินค้าได้ที่เมนู สินค้า", false, false),
                    new SectionDef("home", "signup", "สมัครสมาชิก (ส่วนย่อบนหน้าแรก)", List.of(
                            text("title", "หัวข้อ", SIGNUP_TITLE),
                            area("body", "คำอธิบาย", SIGNUP_BODY),
                            text("cta", "ข้อความบนปุ่ม", "รับโค้ดส่วนลด"),
                            area("purpose", "ข้อความแจ้งวัตถุประสงค์การเก็บข้อมูล (PDPA)",
                                    "เราใช้ชื่อและอีเมลเพื่อส่งโค้ดส่วนลดให้คุณ ขอลบข้อมูลได้ทุกเมื่อผ่านหน้าติดต่อเรา"),
                            text("thanksTitle", "หัวข้อหลังสมัครสำเร็จ", "ขอบคุณที่สมัครนะ!"),
                            area("thanksBody", "ข้อความหลังสมัครสำเร็จ", "เดี๋ยวเราส่งโค้ดส่วนลดไปที่อีเมลของคุณ")),
                            0, "ข้อมูลที่สมัครจากส่วนนี้เข้าตารางสมาชิกเดียวกับหน้าสมัครสมาชิก", false, false))),
            new PageDef("signup", "สมัครสมาชิก", "/signup", List.of(
                    new SectionDef("signup", "intro", "หัวข้อและคำอธิบาย", List.of(
                            text("eyebrow", "ข้อความเล็กด้านบน", "BAKBAN Member"),
                            text("title", "หัวข้อ", SIGNUP_TITLE),
                            area("body", "คำอธิบาย", SIGNUP_BODY)),
                            0, null, true, false),
                    new SectionDef("signup", "image", "รูปประกอบ", List.of(), 1, null, false, false),
                    new SectionDef("signup", "form", "ฟอร์มสมัคร", List.of(
                            text("title", "หัวข้อฟอร์ม", "ข้อมูลสมาชิก"),
                            text("cta", "ข้อความบนปุ่ม", "รับโค้ดส่วนลด"),
                            new FieldDef("showPhone", "แสดงช่องเบอร์โทร", FieldType.BOOLEAN, 5, "true"),
                            area("consent", "ข้อความยินยอม", "ยินยอมรับข่าวสารและโปรโมชันจาก BAKBAN ทางอีเมล"),
                            area("purpose", "ข้อความแจ้งวัตถุประสงค์การเก็บข้อมูล (PDPA)", SIGNUP_PURPOSE),
                            text("thanksTitle", "หัวข้อหลังสมัครสำเร็จ", "ขอบคุณที่สมัครนะ!"),
                            area("thanksBody", "ข้อความหลังสมัครสำเร็จ", "เดี๋ยวเราส่งโค้ดส่วนลดไปที่อีเมลของคุณ"),
                            text("shopCta", "ปุ่มหลังสมัครสำเร็จ", "ไปช้อปเลย")),
                            0, null, false, false))),
            new PageDef("contact", "ติดต่อเรา", "/contact", List.of(
                    new SectionDef("contact", "intro", "หัวข้อและคำอธิบาย", List.of(
                            text("eyebrow", "ข้อความเล็กด้านบน", "Contact BAKBAN"),
                            text("title", "หัวข้อ", "ติดต่อเรา"),
                            area("body", "คำอธิบาย", "มีคำถามเรื่องสินค้า สภาพกระเป๋า หรือการสั่งซื้อ ทักมาได้เลย")),
                            0, null, true, false),
                    new SectionDef("contact", "info", "ข้อมูลติดต่อและโซเชียล", List.of(),
                            0, "ที่อยู่ เวลาเปิด และช่องทางติดต่อดึงมาจากเมนู Footer", false, false),
                    new SectionDef("contact", "map", "แผนที่", List.of(),
                            0, "วางลิงก์ Google Maps หรือโค้ดฝังแผนที่ (iframe) ของร้าน", false, true),
                    new SectionDef("contact", "form", "ฟอร์มส่งข้อความ", List.of(
                            text("title", "หัวข้อฟอร์ม", "ส่งข้อความถึงเรา"),
                            text("cta", "ข้อความบนปุ่ม", "ส่งข้อความ"),
                            area("purpose", "ข้อความแจ้งวัตถุประสงค์การเก็บข้อมูล (PDPA)",
                                    "เราใช้ชื่อและอีเมลเพื่อตอบข้อความนี้เท่านั้น"),
                            text("thanksTitle", "หัวข้อหลังส่งสำเร็จ", "ได้รับข้อความแล้ว ขอบคุณนะ!"),
                            area("thanksBody", "ข้อความหลังส่งสำเร็จ", "ทีมงานจะตอบกลับทางอีเมลโดยเร็ว")),
                            0, "ปิดการแสดงส่วนนี้ = ปิดรับข้อความจากหน้าติดต่อเรา", false, false))));

    private SectionDefs() {}

    public static Optional<PageDef> page(String slug) {
        return PAGES.stream().filter(p -> p.slug().equals(slug)).findFirst();
    }

    public static Optional<SectionDef> section(String page, String key) {
        return page(page).flatMap(p -> p.sections().stream().filter(s -> s.key().equals(key)).findFirst());
    }
}
