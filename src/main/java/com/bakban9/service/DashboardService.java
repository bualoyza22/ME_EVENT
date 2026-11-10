package com.bakban9.service;

import com.bakban9.domain.MemberRepository;
import com.bakban9.domain.Product;
import com.bakban9.domain.ProductRepository;
import com.bakban9.domain.SiteSettings;
import com.bakban9.util.MapEmbed;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Numbers and to-dos for the admin overview, computed from the data (nothing stored separately). */
@Service
public class DashboardService {

    private final ProductRepository products;
    private final MemberRepository members;
    private final MessageService messages;
    private final SettingsService settings;
    private final ContentService content;

    public DashboardService(ProductRepository products, MemberRepository members, MessageService messages,
                            SettingsService settings, ContentService content) {
        this.products = products;
        this.members = members;
        this.messages = messages;
        this.settings = settings;
        this.content = content;
    }

    public record Stat(String label, String value, String note, String href) {}

    public record Todo(String label, String where, String href) {}

    @Transactional(readOnly = true)
    public List<Stat> stats() {
        List<Product> all = products.findActive();
        long visible = all.stream().filter(Product::isVisible).count();
        long noPrice = all.stream().filter(p -> p.isVisible() && p.getPriceThb() == null).count();
        long publishedPages = content.allPages().stream().filter(p -> p.isPublished()).count();
        long open = messages.countOpen();
        return List.of(
                new Stat("สินค้าที่แสดงอยู่", String.valueOf(visible),
                        noPrice > 0 ? "ยังไม่มีราคา " + noPrice + " ใบ" : "ราคาครบแล้ว", "/admin/products"),
                new Stat("หน้าเว็บ", String.valueOf(publishedPages), "หน้าแรก · สมัครสมาชิก · ติดต่อเรา", "/admin/pages"),
                new Stat("สมาชิก", String.valueOf(members.countActive()), "สมัครผ่านหน้าเว็บ", "/admin/members"),
                new Stat("ข้อความใหม่", String.valueOf(open), open > 0 ? "รอตอบกลับ" : "ตอบครบแล้ว", "/admin/messages?filter=open"));
    }

    @Transactional(readOnly = true)
    public List<Todo> todos() {
        List<Todo> out = new ArrayList<>();
        List<Product> all = products.findActive();
        all.forEach(p -> p.getImages().size());
        long noPrice = all.stream().filter(p -> p.getPriceThb() == null).count();
        if (noPrice > 0) {
            out.add(new Todo("ใส่ราคาสินค้า " + noPrice + " ใบ", "สินค้า", "/admin/products"));
        }
        long noBrand = all.stream().filter(p -> p.getBrand().isBlank()).count();
        if (noBrand > 0) {
            out.add(new Todo("ยืนยันแบรนด์ของ " + noBrand + " ใบที่ยังไม่ระบุ", "สินค้า", "/admin/products"));
        }
        long noDetails = all.stream()
                .filter(p -> p.getCondition().isBlank() || p.getSize().isBlank() || p.getAccessories().isBlank()).count();
        if (noDetails > 0) {
            out.add(new Todo("ใส่สภาพ ขนาด และอุปกรณ์ของ " + noDetails + " ใบ", "สินค้า", "/admin/products"));
        }
        long oneImage = all.stream().filter(p -> p.getImages().size() < 2).count();
        if (oneImage > 0) {
            out.add(new Todo("เพิ่มรูปมุมอื่นของ " + oneImage + " ใบ", "สินค้า", "/admin/products"));
        }
        SiteSettings s = settings.get();
        List<String> socials = new ArrayList<>();
        if (s.getInstagramUrl().isBlank()) socials.add("Instagram");
        if (s.getLineUrl().isBlank()) socials.add("LINE");
        if (s.getFacebookUrl().isBlank()) socials.add("Facebook");
        if (s.getTiktokUrl().isBlank()) socials.add("TikTok");
        if (!socials.isEmpty()) {
            out.add(new Todo("ใส่ลิงก์ " + String.join(", ", socials), "Footer", "/admin/footer"));
        }
        if (s.getTel().isBlank()) {
            out.add(new Todo("ใส่เบอร์โทรร้าน", "Footer", "/admin/footer"));
        }
        if (s.getWebsite().isBlank()) {
            out.add(new Todo("ใส่โดเมนเว็บไซต์ที่ยืนยันแล้ว", "Footer", "/admin/footer"));
        }
        if (s.getEmail().isBlank()) {
            out.add(new Todo("ใส่อีเมลร้าน (ใช้รับแจ้งเตือนข้อความ)", "Footer", "/admin/footer"));
        }
        if (MapEmbed.resolve(s.getMapUrl()).isEmpty()) {
            out.add(new Todo("ใส่แผนที่ร้านในหน้าติดต่อเรา", "หน้าเว็บ", "/admin/pages?page=contact&section=map"));
        }
        return out;
    }
}
