package com.eema.backend.service;

import java.math.BigDecimal;
import java.util.Optional;

public interface MollieFeeReportingClient {
    Optional<BigDecimal> findAuthoritativePaymentFee(String paymentId);
}
