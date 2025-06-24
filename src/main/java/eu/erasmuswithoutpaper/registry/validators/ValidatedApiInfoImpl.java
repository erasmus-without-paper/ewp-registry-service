package eu.erasmuswithoutpaper.registry.validators;

public class ValidatedApiInfoImpl implements ValidatedApiInfo {

  private final String apiName;
  private final ApiEndpoint endpoint;
  private final int version;
  private final String preferredPrefix;
  private final boolean isResponseIncludeInCatalogueXmlns;
  private final boolean isApiEntryIncludeInCatalogueXmlns;

  public ValidatedApiInfoImpl(String apiName, int version, ApiEndpoint endpoint,
      String preferredPrefix) {
    this(apiName, endpoint, version, preferredPrefix, false, false);
  }

  /**
   * @param apiName name of api
   * @param endpoint type of endpoint
   * @param version validated version
   * @param preferredPrefix preferred prefix
   * @param isResponseIncludeInCatalogueXmlns is response included in the catalogue XMLNS
   * @param isApiEntryIncludeInCatalogueXmlns is api entry included in the catalogue XMLNS
   */
  public ValidatedApiInfoImpl(String apiName, ApiEndpoint endpoint, int version,
      String preferredPrefix, boolean isResponseIncludeInCatalogueXmlns,
      boolean isApiEntryIncludeInCatalogueXmlns) {
    this.apiName = apiName;
    this.endpoint = endpoint;
    this.version = version;
    this.preferredPrefix = preferredPrefix;
    this.isResponseIncludeInCatalogueXmlns = isResponseIncludeInCatalogueXmlns;
    this.isApiEntryIncludeInCatalogueXmlns = isApiEntryIncludeInCatalogueXmlns;
  }

  @Override
  public String getApiName() {
    return apiName;
  }

  @Override
  public ApiEndpoint getEndpoint() {
    return endpoint;
  }

  @Override
  public int getVersion() {
    return version;
  }

  @Override
  public String getPreferredPrefix() {
    return preferredPrefix;
  }

  @Override
  public boolean responseIncludeInCatalogueXmlns() {
    return isResponseIncludeInCatalogueXmlns;
  }

  @Override
  public boolean apiEntryIncludeInCatalogueXmlns() {
    return isApiEntryIncludeInCatalogueXmlns;
  }

}
