package com.bakban9.web.admin;

import com.bakban9.domain.Media;
import com.bakban9.domain.MediaRepository;
import com.bakban9.domain.Product;
import com.bakban9.service.MediaService;
import com.bakban9.service.ProductService;
import com.bakban9.service.ProductService.ProductForm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminProductsController {

    private final ProductService products;
    private final MediaService mediaService;
    private final MediaRepository mediaRepo;

    public AdminProductsController(ProductService products, MediaService mediaService, MediaRepository mediaRepo) {
        this.products = products;
        this.mediaService = mediaService;
        this.mediaRepo = mediaRepo;
    }

    @GetMapping("/admin/products")
    public String list(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "all") String status,
                       Model model) {
        List<Product> rows = products.list(q, status);
        model.addAttribute("nav", "products");
        model.addAttribute("rows", rows);
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("total", products.countAll());
        return "admin/products";
    }

    @GetMapping("/admin/products/new")
    public String create(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ProductForm());
        }
        return edit(null, model);
    }

    @GetMapping("/admin/products/{id}")
    public String edit(@PathVariable Long id, Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", ProductForm.of(products.get(id)));
        }
        ProductForm form = (ProductForm) model.getAttribute("form");
        Map<Long, Media> byId = mediaRepo.findActiveByIds(form.getImageIds().isEmpty() ? List.of(-1L) : form.getImageIds())
                .stream().collect(Collectors.toMap(Media::getId, Function.identity()));
        model.addAttribute("nav", "products");
        model.addAttribute("productId", id);
        model.addAttribute("images", form.getImageIds().stream().map(byId::get).filter(m -> m != null).toList());
        model.addAttribute("library", mediaRepo.findActive());
        return "admin/product-edit";
    }

    @PostMapping("/admin/products")
    public String createSubmit(@ModelAttribute("form") ProductForm form,
                               @RequestParam(name = "newImages", required = false) List<MultipartFile> files,
                               RedirectAttributes ra) {
        return save(null, form, files, ra);
    }

    @PostMapping("/admin/products/{id}")
    public String updateSubmit(@PathVariable Long id, @ModelAttribute("form") ProductForm form,
                               @RequestParam(name = "newImages", required = false) List<MultipartFile> files,
                               RedirectAttributes ra) {
        products.get(id); // 404 for unknown / deleted products
        return save(id, form, files, ra);
    }

    private String save(Long id, ProductForm form, List<MultipartFile> files, RedirectAttributes ra) {
        String back = id == null ? "redirect:/admin/products/new" : "redirect:/admin/products/" + id;
        Map<String, String> errors = products.validate(form);
        if (!errors.isEmpty()) {
            ra.addFlashAttribute("form", form);
            Flash.errors(ra, errors);
            return back;
        }
        List<Media> uploaded = new ArrayList<>();
        try {
            for (MultipartFile f : files == null ? List.<MultipartFile>of() : files) {
                if (!f.isEmpty()) {
                    uploaded.add(mediaService.upload(f.getOriginalFilename(), f.getBytes(), Media.KIND_PRODUCT, form.getName()));
                }
            }
        } catch (MediaService.UploadException | IOException e) {
            ra.addFlashAttribute("form", form);
            Flash.error(ra, "อัปโหลดรูปไม่สำเร็จ: " + e.getMessage());
            return back;
        }
        Product p = products.save(id, form, uploaded);
        Flash.ok(ra, "บันทึกสินค้า \"" + p.getName() + "\" แล้ว");
        return "redirect:/admin/products/" + p.getId();
    }

    @PostMapping("/admin/products/{id}/visibility")
    public String visibility(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean visible,
                             @RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "all") String status,
                             RedirectAttributes ra) {
        Product p = products.setVisible(id, visible);
        Flash.ok(ra, "\"" + p.getName() + "\" " + (visible ? "แสดงบนหน้าเว็บแล้ว" : "ซ่อนจากหน้าเว็บแล้ว"));
        ra.addAttribute("q", q);
        ra.addAttribute("status", status);
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        Product p = products.softDelete(id);
        Flash.ok(ra, "ลบ \"" + p.getName() + "\" แล้ว (กู้คืนได้ภายใน 30 วันโดยติดต่อผู้พัฒนา)");
        return "redirect:/admin/products";
    }
}
