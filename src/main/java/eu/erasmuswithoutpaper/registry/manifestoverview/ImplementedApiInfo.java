package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class ImplementedApiInfo {
  public String name;
  public ApiVersion version;
  public List<String> urls;

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
}
