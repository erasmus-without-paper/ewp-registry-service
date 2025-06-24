package eu.erasmuswithoutpaper.registry.validators;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;

public interface ValidatedApiInfo {

  /**
   * @return
   *     a name that should be used in creation of a KnownElement.
   */
  default String getElementName() {
    ApiEndpoint endpoint = getEndpoint();
    if (endpoint != ApiEndpoint.NO_ENDPOINT) {
      return getApiName() + "-" + endpoint.getName() + "-response";
    } else {
      return getApiName() + "-response";
    }
  }

  default String getHumanReadableName() {
    return "API entry: " + getApiName().replace("-", " ") + " v" + getVersion();
  }

  String getApiName();

  ApiEndpoint getEndpoint();

  int getVersion();

  String getPreferredPrefix();

  boolean responseIncludeInCatalogueXmlns();

  boolean apiEntryIncludeInCatalogueXmlns();

  /**
   * @return
   *     a KnownElement created using the information provided by the other functions.
   */
  default KnownElement getResponseKnownElement() {
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
        getPreferredPrefix() + "r" + endpoint + version,
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
  default KnownNamespace getApiEntryKnownNamespace() {
    var namespaceApiName = getNamespaceApiName();
    var version = getVersion();
    return new KnownNamespace(
        getPreferredPrefix() + getVersion(),
        namespaceApiName + "/blob/stable-v" + version + "/manifest-entry.xsd",
        namespaceApiName + "/stable-v" + version + "/manifest-entry.xsd",
        apiEntryIncludeInCatalogueXmlns()
    );
  }

  default String getNamespaceApiName() {
    return "api-" + getApiName();
  }

  default String getGitHubRepositoryName() {
    return getApiName();
  }

  default String getApiNamespace() {
    return getApiEntryKnownNamespace().getNamespaceUri();
  }

  default String getApiPrefix() {
    return getApiEntryKnownNamespace().getPreferredPrefix();
  }

  default String getResponsePrefix() {
    return getResponseKnownElement().getNamespacePreferredPrefix();
  }
}
