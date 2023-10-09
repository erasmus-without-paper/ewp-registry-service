package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.List;

public class ImplementedApiInfo {

  private final String name;
  private final ApiVersion version;
  private final List<String> urls;

  /**
   * Constructs a ImplementedApiInfo that describes implemented API listed in a manifest file.
   * @param name
   *      Name of implemented API.
   * @param version
   *      Version of implemented API.
   * @param urls
   *      URLs where this API is available.
   */
  public ImplementedApiInfo(String name, ApiVersion version, List<String> urls) {
    this.name = name;
    this.version = version;
    this.urls = urls;
  }

  public String getName() {
    return name;
  }

  public ApiVersion getVersion() {
    return version;
  }

  public List<String> getUrls() {
    return urls;
  }
}
