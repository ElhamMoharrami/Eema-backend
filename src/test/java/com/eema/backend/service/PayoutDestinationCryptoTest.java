package com.eema.backend.service;
import com.eema.backend.config.PayoutDestinationProperties;
import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.assertj.core.api.Assertions.*;
class PayoutDestinationCryptoTest{
 private PayoutDestinationCrypto crypto(){String a=Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());String b=Base64.getEncoder().encodeToString("fedcba9876543210fedcba9876543210".getBytes());PayoutDestinationCrypto c=new PayoutDestinationCrypto(new PayoutDestinationProperties(true,a,b));c.initialize();return c;}
 @Test void authenticatedEncryptionUsesRandomNonceAndRoundTrips(){var c=crypto();String one=c.encrypt("NL91ABNA0417164300"),two=c.encrypt("NL91ABNA0417164300");assertThat(one).doesNotContain("NL91").isNotEqualTo(two);assertThat(c.decrypt(one)).isEqualTo("NL91ABNA0417164300");assertThat(c.fingerprint("NL91ABNA0417164300")).isEqualTo(c.fingerprint("NL91ABNA0417164300"));}
 @Test void tamperingFailsAuthentication(){var c=crypto();String value=c.encrypt("NL91ABNA0417164300");String tampered=value.substring(0,value.length()-1)+(value.endsWith("A")?"B":"A");assertThatThrownBy(()->c.decrypt(tampered)).isInstanceOf(IllegalStateException.class);}
}
