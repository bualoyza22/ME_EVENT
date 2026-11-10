package com.bakban9.web.pub;

import com.bakban9.domain.Media;
import com.bakban9.domain.Product;
import com.bakban9.util.Texts;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.json.JsonMapper;

/** Product data for the Catalog preview, embedded as JSON that is safe inside a script tag. */
final class CatalogJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private CatalogJson() {}

    static String of(List<Product> products) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("name", p.getName());
            m.put("brand", p.getBrand());
            m.put("price", Texts.formatPrice(p.getPriceThb()));
            m.put("condition", p.getCondition());
            m.put("size", p.getSize());
            m.put("accessories", p.getAccessories());
            m.put("description", p.getDescription());
            List<Map<String, String>> images = new ArrayList<>();
            for (Media media : p.getImageMedia()) {
                Map<String, String> img = new LinkedHashMap<>();
                img.put("src", media.getUrl());
                img.put("srcset", media.getSrcset() == null ? "" : media.getSrcset());
                img.put("alt", media.getAltText().isBlank() ? p.getName() : media.getAltText());
                images.add(img);
            }
            m.put("images", images);
            out.add(m);
        }
        return MAPPER.writeValueAsString(out)
                .replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026")
                .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
    }
}
