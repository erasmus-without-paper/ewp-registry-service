package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.Objects;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
class ManifestAndHostIndex {
  public final String manifestUrl;
  public final int hostId;

  public ManifestAndHostIndex(String manifestUrl, int hostId) {
    this.manifestUrl = manifestUrl;
    this.hostId = hostId;
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
