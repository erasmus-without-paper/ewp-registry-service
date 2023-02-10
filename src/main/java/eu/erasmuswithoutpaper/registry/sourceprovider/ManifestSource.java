package eu.erasmuswithoutpaper.registry.sourceprovider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

import eu.erasmuswithoutpaper.registry.constraints.ManifestConstraint;

/**
 * This describes a single manifest source by its URL (see {@link #getUrl()}) and a list of
 * constraints (see {@link #getConstraints()}.
 *
 * <p>
 * Use static methods for construction.
 * </p>
 */
public class ManifestSource {

  private final String url;

  private final List<ManifestConstraint> constraints;

  /**
   * @param url Manifest source's URL.
   * @param constraints List of manifest source's constraints.
   */
  public ManifestSource(String url, List<ManifestConstraint> constraints) {
    this.url = url;
    this.constraints = constraints;

    // Make sure that the URL is valid and uses a safe protocol.

    try {
      URI realUri = new URI(url);
      if (realUri.getScheme() == null || !realUri.getScheme().equalsIgnoreCase("https")) {
        throw new RuntimeException("Only HTTPS scheme is allowed for manifest sources");
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException("Only valid HTTPS URLs can be used for manifest sources", e);
    }
  }

  /**
   * A list of additional constraints (aside from being valid, against the schema) which this
   * particular manifest file must meet.
   *
   * @return A list of {@link ManifestConstraint} instances.
   */
  public List<ManifestConstraint> getConstraints() {
    return this.constraints;
  }

  /**
   * @return A URL at which the manifest file can be found (will always be a valid HTTPS URL).
   */
  public String getUrl() {
    return this.url;
  }

  @Override
  public String toString() {
    return "ManifestSource[" + this.getUrl() + ", " + this.getConstraints().size()
        + " constraint(s)]";
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    ManifestSource that = (ManifestSource) object;
    return url.equals(that.url);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url);
  }
}
