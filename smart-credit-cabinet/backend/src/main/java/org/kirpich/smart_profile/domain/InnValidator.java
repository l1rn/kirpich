package org.kirpich.smart_profile.domain;

/** Проверка контрольных цифр ИНН: 10 знаков — юрлицо, 12 знаков — ИП. */
public final class InnValidator {

    private static final int[] COEFFS_10 = {2, 4, 10, 3, 5, 9, 4, 6, 8};
    private static final int[] COEFFS_11 = {7, 2, 4, 10, 3, 5, 9, 4, 6, 8};
    private static final int[] COEFFS_12 = {3, 7, 2, 4, 10, 3, 5, 9, 4, 6, 8};

    private InnValidator() {
    }

    public static boolean isValid(String inn) {
        if (inn == null || !inn.chars().allMatch(Character::isDigit)) {
            return false;
        }
        if (inn.length() == 10) {
            return checkDigit(inn, COEFFS_10) == digit(inn, 9);
        }
        if (inn.length() == 12) {
            return checkDigit(inn, COEFFS_11) == digit(inn, 10) && checkDigit(inn, COEFFS_12) == digit(inn, 11);
        }
        return false;
    }

    private static int checkDigit(String inn, int[] coeffs) {
        int sum = 0;
        for (int i = 0; i < coeffs.length; i++) {
            sum += digit(inn, i) * coeffs[i];
        }
        return sum % 11 % 10;
    }

    private static int digit(String inn, int index) {
        return inn.charAt(index) - '0';
    }
}
