-- BAKBAN9 schema
-- The 9 tables from the spec ("โมเดลข้อมูล") plus supporting tables required by
-- "ข้อกำหนดทางเทคนิค" (admin invites, audit log).
-- Every table has id, created_at, updated_at.

CREATE TABLE media (
    id            BIGSERIAL PRIMARY KEY,
    filename      VARCHAR(255) NOT NULL,
    url           VARCHAR(1024) NOT NULL,
    storage_key   VARCHAR(512) NOT NULL,
    mime          VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    width         INTEGER,
    height        INTEGER,
    kind          VARCHAR(20)  NOT NULL CHECK (kind IN ('logo', 'product', 'other')),
    alt_text      VARCHAR(255) NOT NULL DEFAULT '',
    -- resized variants: {"400": "<url>", "1000": "<url>"}
    variant_urls  JSONB        NOT NULL DEFAULT '{}'::jsonb,
    deleted_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_media_kind ON media (kind) WHERE deleted_at IS NULL;

CREATE TABLE products (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(200) NOT NULL,
    brand        VARCHAR(120) NOT NULL DEFAULT '',
    price_thb    INTEGER CHECK (price_thb IS NULL OR price_thb >= 0),
    condition    VARCHAR(120) NOT NULL DEFAULT '',
    size         VARCHAR(120) NOT NULL DEFAULT '',
    accessories  VARCHAR(255) NOT NULL DEFAULT '',
    description  TEXT         NOT NULL DEFAULT '',
    is_visible   BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order   INTEGER      NOT NULL DEFAULT 0,
    deleted_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_public ON products (sort_order, id) WHERE deleted_at IS NULL AND is_visible;

CREATE TABLE product_images (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT  NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    media_id    BIGINT  NOT NULL REFERENCES media (id),
    position    INTEGER NOT NULL,           -- 0 = main image
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_product_images_product ON product_images (product_id, position);
CREATE INDEX idx_product_images_media ON product_images (media_id);

CREATE TABLE pages (
    id            BIGSERIAL PRIMARY KEY,
    slug          VARCHAR(20)  NOT NULL UNIQUE CHECK (slug IN ('home', 'signup', 'contact')),
    title         VARCHAR(200) NOT NULL,
    is_published  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE page_sections (
    id          BIGSERIAL PRIMARY KEY,
    page_id     BIGINT      NOT NULL REFERENCES pages (id) ON DELETE CASCADE,
    key         VARCHAR(40) NOT NULL,
    sort_order  INTEGER     NOT NULL DEFAULT 0,
    is_visible  BOOLEAN     NOT NULL DEFAULT TRUE,
    content     JSONB       NOT NULL DEFAULT '{}'::jsonb,
    media_ids   BIGINT[]    NOT NULL DEFAULT '{}',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (page_id, key)
);

CREATE TABLE site_settings (
    id             BIGSERIAL PRIMARY KEY,
    site_name      VARCHAR(200)  NOT NULL DEFAULT '',
    logo_media_id  BIGINT REFERENCES media (id),
    tagline        VARCHAR(255)  NOT NULL DEFAULT '',
    website        VARCHAR(255)  NOT NULL DEFAULT '',
    address        VARCHAR(500)  NOT NULL DEFAULT '',
    hours          VARCHAR(255)  NOT NULL DEFAULT '',
    tel            VARCHAR(40)   NOT NULL DEFAULT '',
    email          VARCHAR(255)  NOT NULL DEFAULT '',
    instagram_url  VARCHAR(500)  NOT NULL DEFAULT '',
    line_url       VARCHAR(500)  NOT NULL DEFAULT '',
    facebook_url   VARCHAR(500)  NOT NULL DEFAULT '',
    tiktok_url     VARCHAR(500)  NOT NULL DEFAULT '',
    map_url        VARCHAR(2000) NOT NULL DEFAULT '',
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT now()
);
-- single row
CREATE UNIQUE INDEX ux_site_settings_single ON site_settings ((true));

CREATE TABLE members (
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(120) NOT NULL,
    email              VARCHAR(255) NOT NULL,
    phone              VARCHAR(40)  NOT NULL DEFAULT '',
    consent_marketing  BOOLEAN      NOT NULL DEFAULT FALSE,
    consent_at         TIMESTAMPTZ,
    code_sent_at       TIMESTAMPTZ,
    deleted_at         TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_members_email ON members (lower(email));

CREATE TABLE messages (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    email       VARCHAR(255) NOT NULL,
    subject     VARCHAR(200) NOT NULL DEFAULT '',
    body        TEXT         NOT NULL,
    status      VARCHAR(10)  NOT NULL DEFAULT 'open' CHECK (status IN ('open', 'replied')),
    replied_at  TIMESTAMPTZ,
    reply_body  TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_messages_status ON messages (status, created_at DESC);

CREATE TABLE admins (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(120) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    role             VARCHAR(10)  NOT NULL CHECK (role IN ('owner', 'editor')),
    last_login_at    TIMESTAMPTZ,
    -- lockout (spec: lock temporarily after 5 wrong attempts)
    failed_attempts  INTEGER      NOT NULL DEFAULT 0,
    locked_until     TIMESTAMPTZ,
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_admins_email ON admins (lower(email));

-- supporting tables -------------------------------------------------------

CREATE TABLE admin_invites (
    id           BIGSERIAL PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    role         VARCHAR(10)  NOT NULL DEFAULT 'editor' CHECK (role IN ('owner', 'editor')),
    token_hash   VARCHAR(64)  NOT NULL UNIQUE,
    invited_by   BIGINT REFERENCES admins (id),
    expires_at   TIMESTAMPTZ  NOT NULL,
    accepted_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id           BIGSERIAL PRIMARY KEY,
    admin_id     BIGINT REFERENCES admins (id) ON DELETE SET NULL,
    admin_email  VARCHAR(255) NOT NULL DEFAULT '',
    action       VARCHAR(60)  NOT NULL,
    entity_type  VARCHAR(40)  NOT NULL,
    entity_id    BIGINT,
    summary      VARCHAR(500) NOT NULL DEFAULT '',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at DESC);
