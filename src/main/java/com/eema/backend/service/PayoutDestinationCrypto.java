package com.eema.backend.service;

import com.eema.backend.config.PayoutDestinationProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

@Component @RequiredArgsConstructor
public class PayoutDestinationCrypto {
    private static final String VERSION="v1";
    private final PayoutDestinationProperties properties;
    private SecretKey encryptionKey; private SecretKey fingerprintKey;
    @PostConstruct void initialize(){
        if(!properties.enabled()) return;
        byte[] enc=decode(properties.encryptionKey(),"PAYOUT_DATA_ENCRYPTION_KEY");
        byte[] fp=decode(properties.fingerprintKey(),"PAYOUT_DATA_FINGERPRINT_KEY");
        if(enc.length!=32||fp.length<32) throw new IllegalStateException("Payout destination keys must contain at least 256 bits");
        encryptionKey=new SecretKeySpec(enc,"AES"); fingerprintKey=new SecretKeySpec(fp,"HmacSHA256");
    }
    public String encrypt(String value){try{byte[] iv=new byte[12];SecureRandom.getInstanceStrong().nextBytes(iv);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,encryptionKey,new GCMParameterSpec(128,iv));return VERSION+":"+Base64.getUrlEncoder().withoutPadding().encodeToString(iv)+":"+Base64.getUrlEncoder().withoutPadding().encodeToString(c.doFinal(value.getBytes(StandardCharsets.UTF_8)));}catch(GeneralSecurityException e){throw new IllegalStateException("Could not protect payout destination",e);}}
    public String decrypt(String encoded){try{String[] p=encoded.split(":",3);if(p.length!=3||!VERSION.equals(p[0]))throw new GeneralSecurityException("Unsupported ciphertext");byte[] iv=Base64.getUrlDecoder().decode(p[1]);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,encryptionKey,new GCMParameterSpec(128,iv));return new String(c.doFinal(Base64.getUrlDecoder().decode(p[2])),StandardCharsets.UTF_8);}catch(GeneralSecurityException|IllegalArgumentException e){throw new IllegalStateException("Could not read payout destination",e);}}
    public String fingerprint(String normalized){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(fingerprintKey);return java.util.HexFormat.of().formatHex(mac.doFinal(normalized.getBytes(StandardCharsets.UTF_8)));}catch(GeneralSecurityException e){throw new IllegalStateException("Could not fingerprint payout destination",e);}}
    private byte[] decode(String value,String name){try{if(value==null||value.isBlank())throw new IllegalArgumentException();return Base64.getDecoder().decode(value);}catch(IllegalArgumentException e){throw new IllegalStateException(name+" must be configured as Base64",e);}}
}
