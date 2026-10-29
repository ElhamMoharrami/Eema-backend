package com.eema.backend.service;
import com.eema.backend.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class IbanPolicyTest{
 private final IbanPolicy policy=new IbanPolicy();
 @Test void normalizesAndAcceptsValidSepaIbans(){assertThat(policy.normalizeAndValidate("nl91 abna 0417 1643 00")).isEqualTo("NL91ABNA0417164300");assertThat(policy.normalizeAndValidate("DE89 3704 0044 0532 0130 00")).isEqualTo("DE89370400440532013000");}
 @Test void rejectsChecksumAndUnsupportedCountry(){assertThatThrownBy(()->policy.normalizeAndValidate("NL90ABNA0417164300")).isInstanceOf(BadRequestException.class);assertThatThrownBy(()->policy.normalizeAndValidate("GB82WEST12345698765432")).isInstanceOf(BadRequestException.class);}
}
