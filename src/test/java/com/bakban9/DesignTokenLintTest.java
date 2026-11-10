package com.bakban9;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Spec: colors come only from the Design System CSS variables; tokens.css is the one place with values. */
class DesignTokenLintTest {

    private static final Pattern COLOR_LITERAL = Pattern.compile("#[0-9a-fA-F]{3,8}\\b|rgba?\\(|hsla?\\(");
    private static final Path RES = Path.of("src/main/resources");

    @Test
    void noColorLiteralsOutsideTokens() throws IOException {
        List<Path> files = files(RES.resolve("static/css"), ".css");
        files.addAll(files(RES.resolve("templates"), ".html"));
        files.addAll(files(RES.resolve("static/js"), ".js"));
        for (Path f : files) {
            if (f.getFileName().toString().equals("tokens.css")) {
                continue;
            }
            String text = Files.readString(f).replaceAll("&#\\d+;", "");
            assertThat(COLOR_LITERAL.matcher(text).find()).as("color literal in " + f).isFalse();
        }
    }

    @Test
    void templatesHaveNoInlineStyles() throws IOException {
        for (Path f : files(RES.resolve("templates"), ".html")) {
            assertThat(Files.readString(f)).as("inline style in " + f).doesNotContain(" style=\"");
        }
    }

    @Test
    void tokensFileDefinesEverySpecToken() throws IOException {
        String tokens = Files.readString(RES.resolve("static/css/tokens.css"));
        for (String t : new String[] {"--bg-primary: #f4efe6", "--bg-secondary: #e7ddd0", "--color-primary: #84563c",
                "--color-sub-accent: #936b4f", "--text-primary: #2c2623", "--text-secondary: #5e524d",
                "--text-light: #ffffff", "--border-dark: #3d272a"}) {
            assertThat(tokens).contains(t);
        }
    }

    @Test
    void publicPagesAreThai() throws IOException {
        for (Path f : files(RES.resolve("templates/public"), ".html")) {
            assertThat(Files.readString(f)).contains("<html lang=\"th\"");
        }
    }

    private static List<Path> files(Path dir, String ext) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(ext)).collect(java.util.stream.Collectors.toList());
        }
    }
}
