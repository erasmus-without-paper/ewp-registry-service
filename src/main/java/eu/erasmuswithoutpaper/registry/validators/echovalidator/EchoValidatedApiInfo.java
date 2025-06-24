package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfoImpl;

class EchoValidatedApiInfo extends ValidatedApiInfoImpl {

  public EchoValidatedApiInfo(int version) {
    super("echo", version, ApiEndpoint.NO_ENDPOINT, "e");
  }

  @Override
  public String getElementName() {
    return "response";
  }

  @Override
  public boolean apiEntryIncludeInCatalogueXmlns() {
    return version == 2;
  }

}
