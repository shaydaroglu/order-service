package com.sercan.order_service.domain;

import com.sercan.order_service.domain.exception.InvalidPaymentMethodException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

public class PaymentMethodTest {

    @Nested
    @DisplayName("INVOICE payment type")
    class InvoicePaymentType {

        @Test
        @DisplayName("should be valid without IBAN")
        void shouldBeValidWithoutIban() {
            assertThatNoException().isThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.INVOICE, null).validate());
        }

        @Test
        @DisplayName("should throw when IBAN is provided")
        void shouldThrowWhenIbanProvided() {
            assertThatThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.INVOICE, "DE89370400440532013000").validate())
                    .isInstanceOf(InvalidPaymentMethodException.class)
                    .hasMessageContaining("IBAN must not be provided for INVOICE payment type");
        }
    }

    @Nested
    @DisplayName("DIRECT_DEBIT payment type")
    class DirectDebitPaymentType {

        @Test
        @DisplayName("should be valid with valid IBAN")
        void shouldBeValidWithValidIban() {
            assertThatNoException().isThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.DIRECT_DEBIT, "DE89370400440532013000").validate());
        }

        @Test
        @DisplayName("should throw when IBAN is null")
        void shouldThrowWhenIbanIsNull() {
            assertThatThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.DIRECT_DEBIT, null).validate())
                    .isInstanceOf(InvalidPaymentMethodException.class)
                    .hasMessageContaining("IBAN is required for DIRECT_DEBIT payment type");
        }

        @Test
        @DisplayName("should throw when IBAN is blank")
        void shouldThrowWhenIbanIsBlank() {
            assertThatThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.DIRECT_DEBIT, "   ").validate())
                    .isInstanceOf(InvalidPaymentMethodException.class)
                    .hasMessageContaining("IBAN is required for DIRECT_DEBIT payment type");
        }

        @Test
        @DisplayName("should throw when IBAN format is invalid")
        void shouldThrowWhenIbanFormatIsInvalid() {
            assertThatThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.DIRECT_DEBIT, "INVALID_IBAN").validate())
                    .isInstanceOf(InvalidPaymentMethodException.class)
                    .hasMessageContaining("Invalid IBAN format");
        }

        @Test
        @DisplayName("should throw when IBAN fails MOD-97 checksum")
        void shouldThrowWhenIbanFailsChecksum() {
            assertThatThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.DIRECT_DEBIT, "DE00370400440532013000").validate())
                    .isInstanceOf(InvalidPaymentMethodException.class)
                    .hasMessageContaining("Invalid IBAN format");
        }

        @Test
        @DisplayName("should accept IBAN with spaces")
        void shouldAcceptIbanWithSpaces() {
            assertThatNoException().isThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.DIRECT_DEBIT, "DE89 3704 0044 0532 0130 00").validate());
        }

        @Test
        @DisplayName("should accept various valid country IBANs")
        void shouldAcceptVariousValidIbans() {
            assertThatNoException().isThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.DIRECT_DEBIT, "GB29NWBK60161331926819").validate());
            assertThatNoException().isThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.DIRECT_DEBIT, "FR7630006000011234567890189").validate());
            assertThatNoException().isThrownBy(() ->
                    new PaymentMethod(PaymentMethod.PaymentType.DIRECT_DEBIT, "TR330006100519786457841326").validate());
        }
    }
}
