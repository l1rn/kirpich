package org.kirpich.smart_profile.domain;

import java.util.Locale;

/** Финансовая арифметика: аннуитет, обратный расчёт суммы, форматирование рублей. */
public final class Finance {

    private Finance() {
    }

    private static double monthlyRate(double annualRate) {
        return annualRate / 12 / 100;
    }

    public static long annuityPayment(double amount, double annualRate, int months) {
        double rate = monthlyRate(annualRate);
        if (rate == 0) {
            return Math.round(amount / months);
        }
        return Math.round(amount * rate / (1 - Math.pow(1 + rate, -months)));
    }

    /** Максимальная сумма кредита, ежемесячный платёж по которой не превышает payment. */
    public static long maxAmountForPayment(double payment, double annualRate, int months) {
        if (payment <= 0) {
            return 0;
        }
        double rate = monthlyRate(annualRate);
        if (rate == 0) {
            return (long) (payment * months);
        }
        return (long) (payment * (1 - Math.pow(1 + rate, -months)) / rate);
    }

    public static long roundDown(double amount, long step) {
        return (long) Math.floor(amount / step) * step;
    }

    public static String formatRub(double amount) {
        if (amount >= 1_000_000) {
            String millions = String.format(Locale.ROOT, "%.2f", amount / 1_000_000)
                    .replace('.', ',')
                    .replaceAll("0+$", "")
                    .replaceAll(",$", "");
            return millions + " млн ₽";
        }
        return String.format(Locale.ROOT, "%,d", Math.round(amount)).replace(',', ' ') + " ₽";
    }

    public static String formatPercent(double ratio) {
        return String.format(Locale.ROOT, "%.0f%%", ratio * 100);
    }
}
