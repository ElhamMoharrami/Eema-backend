package com.eema.backend.service;

import com.eema.backend.repository.*;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class FinancialDocumentConcurrencyContractTest {
    @Test void issuanceLocksAuthoritativeAggregateRowsBeforeUniqueDocumentCreation() throws Exception {
        Lock bookingLock = BookingRepository.class.getMethod("findByIdForUpdate", UUID.class).getAnnotation(Lock.class);
        Lock payoutLock = OwnerPayoutRepository.class.getMethod("findByIdForUpdate", UUID.class).getAnnotation(Lock.class);
        assertThat(bookingLock).isNotNull();
        assertThat(bookingLock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
        assertThat(payoutLock).isNotNull();
        assertThat(payoutLock.value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }
}
