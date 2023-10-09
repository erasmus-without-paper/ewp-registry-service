package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.Objects;

class ManifestAndHostIndex {
  private final String manifestUrl;
  private final int hostId;

  public ManifestAndHostIndex(String manifestUrl, int hostId) {
    this.manifestUrl = manifestUrl;
    this.hostId = hostId;
  }

  public String getManifestUrl() {
    return manifestUrl;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    ManifestAndHostIndex that = (ManifestAndHostIndex) other;
    return hostId == that.hostId
        && Objects.equals(manifestUrl, that.manifestUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(manifestUrl, hostId);
  }
}
