package com.eema.backend.service;
import com.eema.backend.entity.enums.ProviderCostResponsibility;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class ProviderCostAllocationTest {
    private final BookingFinancials financials = new BookingFinancials();
    @ParameterizedTest
    @CsvSource({
        "NONE,16,5,16,0,5,0", "NONE,16,16,16,0,16,0", "NONE,16,25,16,0,25,0", "NONE,0,25,0,0,25,0",
        "EEMA,16,5,16,0,5,0", "EEMA,16,16,16,0,16,0", "EEMA,16,25,16,0,25,0", "EEMA,0,25,0,0,25,0",
        "OWNER_SETTLEMENT,16,5,11,5,0,0", "OWNER_SETTLEMENT,16,16,0,16,0,0", "OWNER_SETTLEMENT,16,25,0,16,9,0", "OWNER_SETTLEMENT,0,25,0,0,25,0",
        "OWNER,16,5,11,5,0,0", "OWNER,16,16,0,16,0,0", "OWNER,16,25,0,16,0,9", "OWNER,0,25,0,0,0,25"
    })
    void v1Allocation(ProviderCostResponsibility responsibility,String entitlement,String cost,String payout,String applied,String platform,String debt){
        var result=financials.allocateProviderCost(new BigDecimal(entitlement),new BigDecimal(cost),responsibility);
        assertEquals(money(payout),result.ownerPayout());assertEquals(money(applied),result.appliedToSettlement());
        assertEquals(money(platform),result.eemaProviderCost());assertEquals(money(debt),result.ownerLiability());
        assertEquals(money(cost),result.appliedToSettlement().add(result.eemaProviderCost()).add(result.ownerLiability()));
    }
    private BigDecimal money(String value){return new BigDecimal(value).setScale(2);}
}
