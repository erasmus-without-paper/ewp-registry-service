package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class EchoValidatedApiInfoV2 implements ValidatedApiInfo {
  @Override
  public String getApiName() {
    return "echo";
  }

  @Override
  public KnownElement getResponseKnownElement() {
    return KnownElement.RESPONSE_ECHO_V2;
  }

  @Override
  public KnownNamespace getApiEntryKnownNamespace() {
    return KnownNamespace.APIENTRY_ECHO_V2;
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return ApiEndpoint.NoEndpoint;
  }
}
