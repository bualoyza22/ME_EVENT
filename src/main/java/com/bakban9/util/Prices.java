package com.bakban9.util;

/** Parses the admin price field: digits only (commas and spaces allowed), empty = no price. */
public final class Prices {

    public static final int MAX_PRICE = 10_000_000;

    private Prices() {}

    public sealed interface Result permits Empty, Valid, Invalid {}
    public record Empty() implements Result {}
    public record Valid(int value) implements Result {}
    public record Invalid(String message) implements Result {}

    public static Result parse(String input) {
        String s = input == null ? "" : input.replace(",", "").replace(" ", "").trim();
        if (s.isEmpty()) {
            return new Empty();
        }
        if (!s.matches("\\d{1,9}")) {
            return new Invalid("ราคาต้องเป็นตัวเลขเท่านั้น เช่น 1290");
        }
        long v = Long.parseLong(s);
        if (v > MAX_PRICE) {
            return new Invalid("ราคาต้องไม่เกิน 10,000,000 บาท");
        }
        return new Valid((int) v);
    }
}
