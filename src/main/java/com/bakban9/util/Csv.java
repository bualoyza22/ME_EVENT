package com.bakban9.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** CSV writer for Excel: UTF-8 with BOM, CRLF, quoted fields, formula-injection guard. */
public final class Csv {

    public static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private Csv() {}

    public static byte[] toBytes(List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(cell(row.get(i)));
            }
            sb.append("\r\n");
        }
        byte[] body = sb.toString().getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[UTF8_BOM.length + body.length];
        System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
        System.arraycopy(body, 0, out, UTF8_BOM.length, body.length);
        return out;
    }

    static String cell(String value) {
        String v = value == null ? "" : value;
        // values starting with these characters are run as formulas by Excel
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        return "\"" + v.replace("\"", "\"\"") + "\"";
    }
}
