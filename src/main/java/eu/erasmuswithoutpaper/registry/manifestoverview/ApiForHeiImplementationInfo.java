package eu.erasmuswithoutpaper.registry.manifestoverview;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
class ApiForHeiImplementationInfo {
  public final String manifestUrl;
  public final int hostId;
  public final String version;

  public ApiForHeiImplementationInfo(String manifestUrl, int hostId, String version) {
    this.manifestUrl = manifestUrl;
    this.hostId = hostId;
    this.version = version;
  }
}
