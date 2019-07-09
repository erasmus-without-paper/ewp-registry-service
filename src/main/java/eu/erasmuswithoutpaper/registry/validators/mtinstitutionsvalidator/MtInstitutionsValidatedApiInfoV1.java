package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class MtInstitutionsValidatedApiInfoV1 implements ValidatedApiInfo {
  @Override
  public KnownElement getKnownElement() {
    return KnownElement.RESPONSE_MT_INSTITUTIONS_V1;
  }

  @Override
  public String getApiNamespace() {
    return KnownNamespace.APIENTRY_MT_INSTITUTIONS_V1.getNamespaceUri();
  }

  @Override
  public String getApiName() {
    return "mt-institutions";
  }

  @Override
  public String getApiPrefix() {
    return KnownNamespace.APIENTRY_MT_INSTITUTIONS_V1.getPreferredPrefix();
  }

  @Override
  public String getApiResponsePrefix() {
    return KnownNamespace.RESPONSE_MT_INSTITUTIONS_V1.getPreferredPrefix();
  }

  @Override
  public String getEndpoint() {
    return NO_ENDPOINT;
  }
}
