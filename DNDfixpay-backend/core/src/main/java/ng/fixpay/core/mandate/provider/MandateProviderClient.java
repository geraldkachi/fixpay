package ng.fixpay.core.mandate.provider;

import ng.fixpay.core.mandate.dto.CreateMandateRequest;

public interface MandateProviderClient {
    MandateProviderResult createMandate(String mandateReference, CreateMandateRequest request);
    MandateProviderResult syncMandateStatus(String mandateReference, String providerReference);
}
