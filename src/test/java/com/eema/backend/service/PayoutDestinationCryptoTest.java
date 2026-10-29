package com.eema.backend.service;
import com.eema.backend.config.PayoutDestinationProperties;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;
class PayoutDestinationCryptoTest{
 private PayoutDestinationCrypto crypto(){String a=Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());String b=Base64.getEncoder().encodeToString("fedcba9876543210fedcba9876543210".getBytes());PayoutDestinationCrypto c=new PayoutDestinationCrypto(new PayoutDestinationProperties(true,a,b));c.initialize();return c;}
 @Test void authenticatedEncryptionUsesRandomNonceAndRoundTrips(){var c=crypto();String one=c.encrypt("NL91ABNA0417164300"),two=c.encrypt("NL91ABNA0417164300");assertThat(one).doesNotContain("NL91").isNotEqualTo(two);assertThat(c.decrypt(one)).isEqualTo("NL91ABNA0417164300");assertThat(c.fingerprint("NL91ABNA0417164300")).isEqualTo(c.fingerprint("NL91ABNA0417164300"));}
 @Test void tamperingFailsAuthentication(){var c=crypto();String value=c.encrypt("NL91ABNA0417164300");String[] parts=value.split(":",3);byte[] ciphertext=Base64.getUrlDecoder().decode(parts[2]);ciphertext[0]^=1;String tampered=parts[0]+":"+parts[1]+":"+Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);assertThatThrownBy(()->c.decrypt(tampered)).isInstanceOf(IllegalStateException.class);}

 @org.junit.jupiter.params.ParameterizedTest
 @org.junit.jupiter.params.provider.ValueSource(strings={"v2:AAAA:BBBB","v1:not-base64!:BBBB","v1:AA:AA","unversioned"})
 void malformedEnvelopeFailsClosed(String value){assertThatThrownBy(()->crypto().decrypt(value)).isInstanceOf(IllegalStateException.class).hasMessage("Could not read payout destination");}
 @Test void missingOrShortConfiguredKeysFailStartup(){
  String valid=Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());
  String shortKey=Base64.getEncoder().encodeToString("0123456789abcdef".getBytes());
  for(var properties:java.util.List.of(new PayoutDestinationProperties(true,null,valid),new PayoutDestinationProperties(true,shortKey,valid),new PayoutDestinationProperties(true,valid,shortKey)))
   assertThatThrownBy(()->new PayoutDestinationCrypto(properties).initialize()).isInstanceOf(IllegalStateException.class);
 }
}
