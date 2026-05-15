package com.sercan.order_service.domain;

import com.sercan.order_service.domain.exception.InvalidPaymentMethodException;

public record PaymentMethod(
        PaymentType type,
        String iban
) {
    public enum PaymentType {
        DIRECT_DEBIT,
        INVOICE
    }

    public void validate() {
        if (type == PaymentType.DIRECT_DEBIT) {
            if (iban == null || iban.isBlank()) {
                throw new InvalidPaymentMethodException("IBAN is required for DIRECT_DEBIT payment type");
            }
            if (!isValidIban(iban)) {
                throw new InvalidPaymentMethodException("Invalid IBAN format: " + iban);
            }
        }
        if (type == PaymentType.INVOICE && iban != null) {
            throw new InvalidPaymentMethodException("IBAN must not be provided for INVOICE payment type");
        }
    }

    private boolean isValidIban(String iban) {
        String cleaned = iban.replaceAll("\\s", "").toUpperCase();
        if (cleaned.length() < 15 || cleaned.length() > 34) return false;
        if (!cleaned.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]+")) return false;

        // Move first 4 chars to end and convert letters to numbers
        String rearranged = cleaned.substring(4) + cleaned.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(c - 'A' + 10);
            } else {
                numeric.append(c);
            }
        }

        // MOD-97 check
        java.math.BigInteger bigInt = new java.math.BigInteger(numeric.toString());
        return bigInt.mod(java.math.BigInteger.valueOf(97)).intValue() == 1;
    }
}
