package com.bakban9.service;

import com.bakban9.domain.Product;
import com.bakban9.domain.ProductRepository;
import com.bakban9.util.Texts;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Public product list: brand search + pagination (spec: 6 per page, search resets to page 1). */
@Service
public class CatalogService {

    private final ProductRepository products;

    public CatalogService(ProductRepository products) {
        this.products = products;
    }

    /**
     * @param items products on the requested page
     * @param all   every matching product, in order — the Catalog preview steps through these
     */
    public record CatalogPage(String brand, List<Product> items, List<Product> all, int page, int pageCount,
                              int perPage, int offset) {
        public int total() {
            return all.size();
        }

        public boolean isEmpty() {
            return all.isEmpty();
        }

        public boolean hasPrev() {
            return page > 1;
        }

        public boolean hasNext() {
            return page < pageCount;
        }
    }

    /**
     * Shop inventory is small (one-of-a-kind second-hand bags), so the matching list is loaded
     * once and sliced; the preview dialog needs the whole matching list anyway.
     */
    @Transactional(readOnly = true)
    public CatalogPage search(String brand, int requestedPage, int perPage) {
        String q = Texts.nz(brand);
        int size = Math.max(1, Math.min(perPage, 48));
        List<Product> all = products.findPublic(Texts.containsPattern(q));
        all.forEach(p -> p.getImages().size()); // load images inside the transaction
        int pageCount = Math.max(1, (all.size() + size - 1) / size);
        int page = Math.max(1, Math.min(requestedPage, pageCount));
        int from = (page - 1) * size;
        List<Product> items = all.subList(Math.min(from, all.size()), Math.min(from + size, all.size()));
        return new CatalogPage(q, List.copyOf(items), List.copyOf(all), page, pageCount, size, from);
    }
}
