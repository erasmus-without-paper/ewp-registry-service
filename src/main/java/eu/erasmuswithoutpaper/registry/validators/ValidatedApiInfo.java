package eu.erasmuswithoutpaper.registry.validators;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

public abstract class ValidatedApiInfo {
  /**
   * @return
   *     a name that should be used in creation of a KnownElement.
   */
  public String getElementName() {
    ApiEndpoint endpoint = getEndpoint();
    if (endpoint != ApiEndpoint.NO_ENDPOINT) {
      return getApiName() + "-" + endpoint.getName() + "-response";
    } else {
      return getApiName() + "-response";
    }
  }

  public String getHumanReadableName() {
    return "API entry: " + getApiName().replace("-", " ") + " v" + getVersion();
  }

  public abstract String getApiName();

  public abstract ApiEndpoint getEndpoint();

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
    int version = getVersion();
    String namespaceApiName = getNamespaceApiName();

    String uriEnding = namespaceApiName + "/tree/stable-v" + version;

    // If endpoint is defined, the strings follow different patterns.
    ApiEndpoint apiEndpoint = getEndpoint();
    if (apiEndpoint != ApiEndpoint.NO_ENDPOINT) {
      String name = apiEndpoint.getName();
      endpoint = name.substring(0, 1);
      responseXsd = name + "-response.xsd";
      uriEnding =
          namespaceApiName + "/blob/stable-v" + version + "/endpoints/" + responseXsd;
    }


    KnownNamespace namespace = new KnownNamespace(
        preferredPrefix() + "r" + endpoint + version,
        uriEnding,
        namespaceApiName + "/stable-v" + version + "/endpoints/" + responseXsd,
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
    var namespaceApiName = getNamespaceApiName();
    var version = getVersion();
    return new KnownNamespace(
        preferredPrefix() + getVersion(),
        namespaceApiName + "/blob/stable-v" + version + "/manifest-entry.xsd",
        namespaceApiName + "/stable-v" + version + "/manifest-entry.xsd",
        apiEntryIncludeInCatalogueXmlns()
    );
  }

  public String getNamespaceApiName() {
    return "api-" + getApiName();
  }

  public String getGitHubRepositoryName() {
    return getApiName();
  }

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
