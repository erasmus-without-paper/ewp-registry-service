package eu.erasmuswithoutpaper.registry.validators;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

public abstract class ValidatedApiInfo {
  /**
   * @return
   *     a name that should be used in creation of a KnownElement.
   */
  public String getElementName() {
    if (getEndpoint() != ApiEndpoint.NoEndpoint) {
      return getApiName() + "-" + getEndpoint().getName() + "-response";
    } else {
      return getApiName() + "-response";
    }
  }

  public String getHumanReadableName() {
    return "API entry: " + getApiName().replace("-", " ") + " v" + getVersion();
  }

  public abstract int getVersion();

  public abstract String preferredPrefix();

  public abstract boolean responseIncludeInCatalogueXmlns();

  public abstract boolean apiEntryIncludeInCatalogueXmlns();

  /**
   * @return
   *     a KnownElement created using the information provided by the other functions.
   */
  public KnownElement getResponseKnownElement() {
    String endpoint = "";
    String responseXsd = "response.xsd";
    String uriEnding = getNamespaceApiName() + "/tree/stable-v" + getVersion();

    // If endpoint is defined, the strings follow different patterns.
    if (getEndpoint() != ApiEndpoint.NoEndpoint) {
      endpoint = getEndpoint().getName().substring(0, 1);
      responseXsd = getEndpoint().getName() + "-response.xsd";
      uriEnding =
          getNamespaceApiName() + "/blob/stable-v" + getVersion() + "/endpoints/" + responseXsd;
    }


    KnownNamespace namespace = new KnownNamespace(
        preferredPrefix() + "r" + endpoint + getVersion(),
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

  /**
   * @return
   *     a KnownNamespace created using the information provided by the other functions.
   */
  public KnownNamespace getApiEntryKnownNamespace() {
    return new KnownNamespace(
        preferredPrefix() + getVersion(),
        getNamespaceApiName() + "/blob/stable-v" + getVersion() + "/manifest-entry.xsd",
        getNamespaceApiName() + "/stable-v" + getVersion() + "/manifest-entry.xsd",
        apiEntryIncludeInCatalogueXmlns()
    );
  }

  public String getNamespaceApiName() {
    return "api-" + getApiName();
  }

  public abstract String getApiName();

  public String getGitHubRepositoryName() {
    return getApiName();
  }

  public abstract ApiEndpoint getEndpoint();

  public String getApiNamespace() {
    return getApiEntryKnownNamespace().getNamespaceUri();
  }

  public String getApiPrefix() {
    return getApiEntryKnownNamespace().getPreferredPrefix();
  }

  public String getResponsePrefix() {
    return getResponseKnownElement().getNamespacePreferredPrefix();
  }
}
