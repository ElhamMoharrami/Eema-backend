package com.eema.backend.service;

import com.eema.backend.exception.BadRequestException;
import org.springframework.stereotype.Component;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;

@Component
public class IbanPolicy {
    private static final Map<String,Integer> LENGTHS=Map.ofEntries(
            Map.entry("AT",20),Map.entry("BE",16),Map.entry("BG",22),Map.entry("HR",21),Map.entry("CY",28),
            Map.entry("CZ",24),Map.entry("DK",18),Map.entry("EE",20),Map.entry("FI",18),Map.entry("FR",27),
            Map.entry("DE",22),Map.entry("GR",27),Map.entry("HU",28),Map.entry("IE",22),Map.entry("IT",27),
            Map.entry("LV",21),Map.entry("LT",20),Map.entry("LU",20),Map.entry("MT",31),Map.entry("NL",18),
            Map.entry("PL",28),Map.entry("PT",25),Map.entry("RO",24),Map.entry("SK",24),Map.entry("SI",19),
            Map.entry("ES",24),Map.entry("SE",24),Map.entry("IS",26),Map.entry("LI",21),Map.entry("NO",15));

    public String normalizeAndValidate(String input){
        if(input==null) throw invalid();
        String iban=input.replaceAll("\\s+","").toUpperCase(java.util.Locale.ROOT);
        if(!iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]+")) throw invalid();
        Integer length=LENGTHS.get(iban.substring(0,2));
        if(length==null||iban.length()!=length) throw invalid();
        String rearranged=iban.substring(4)+iban.substring(0,4); StringBuilder digits=new StringBuilder();
        for(char c:rearranged.toCharArray()) digits.append(Character.isDigit(c)?String.valueOf(c):String.valueOf(c-'A'+10));
        if(!new BigInteger(digits.toString()).mod(BigInteger.valueOf(97)).equals(BigInteger.ONE)) throw invalid();
        return iban;
    }
    private BadRequestException invalid(){return new BadRequestException("IBAN is not a valid supported SEPA IBAN");}
}
