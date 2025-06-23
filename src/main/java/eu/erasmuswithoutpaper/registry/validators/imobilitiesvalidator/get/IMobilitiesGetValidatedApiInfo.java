package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.get;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;

class IMobilitiesGetValidatedApiInfo extends ValidatedApiInfo {
  private final int version;
  private final ApiEndpoint endpoint;

  public IMobilitiesGetValidatedApiInfo(int version, ApiEndpoint endpoint) {
    this.version = version;
    this.endpoint = endpoint;
  }

  @Override
  public KnownElement getResponseKnownElement() {
    String endpoint = getEndpoint().getName().substring(0, 1);
    String responseXsd = getEndpoint().getName() + "-response.xsd";
    String uriEnding =
        getNamespaceApiName() + "/blob/stable-v" + getVersion() + "/endpoints/" + responseXsd;


    KnownNamespace namespace = new KnownNamespace(
        getPreferredPrefix()  + endpoint + getVersion(),
        uriEnding,
        getNamespaceApiName() + "/stable-v" + getVersion() + "/endpoints/" + responseXsd,
        responseIncludeInCatalogueXmlns()
    );

    return new KnownElement(
        namespace,
        getElementName(),
        getHumanReadableName()
    );
  }

  @Override
  public int getVersion() {
    return this.version;
  }

  @Override
  public String getPreferredPrefix() {
    return "im";
  }

  @Override
  public boolean responseIncludeInCatalogueXmlns() {
    return false;
  }

  @Override
  public boolean apiEntryIncludeInCatalogueXmlns() {
    return false;
  }

  @Override
  public String getApiName() {
    return "imobilities";
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return this.endpoint;
  }
}
