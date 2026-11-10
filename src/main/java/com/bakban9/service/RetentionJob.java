package com.bakban9.service;

import com.bakban9.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Permanently removes rows that were soft-deleted more than 30 days ago (spec + PDPA). */
@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final ProductService products;
    private final MemberService members;
    private final MediaService media;
    private final AppProperties props;

    public RetentionJob(ProductService products, MemberService members, MediaService media, AppProperties props) {
        this.products = products;
        this.members = members;
        this.media = media;
        this.props = props;
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "Asia/Bangkok")
    public void purge() {
        Instant before = Instant.now().minus(Duration.ofDays(props.retention().softDeleteDays()));
        int p = products.purgeDeletedBefore(before);
        int m = members.purgeDeletedBefore(before);
        int f = media.purgeDeletedBefore(before);
        if (p + m + f > 0) {
            log.info("retention purge: {} products, {} members, {} media", p, m, f);
        }
    }
}
