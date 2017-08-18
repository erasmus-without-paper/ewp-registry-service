package eu.erasmuswithoutpaper.registry.sourceprovider;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.constraints.ForbidRegistryImplementations;
import eu.erasmuswithoutpaper.registry.constraints.ManifestConstraint;
import eu.erasmuswithoutpaper.registry.constraints.RemoveEmbeddedCatalogues;
import eu.erasmuswithoutpaper.registry.constraints.ClientKeySecurityConstraint;
import eu.erasmuswithoutpaper.registry.constraints.TlsClientCertificateSecurityConstraint;
import eu.erasmuswithoutpaper.registry.constraints.VerifyApiVersions;
import eu.erasmuswithoutpaper.registry.constraints.VerifyDiscoveryApiEntry;

import com.google.common.collect.Lists;

/**
 * This describes a single manifest source by its URL (see {@link #getUrl()}) and a list of
 * constraints (see {@link #getConstraints()}.
 *
 * <p>
 * Use static methods for construction.
 * </p>
 */
public class ManifestSource {

  /**
   * Returns a new instance of {@link ManifestSource} with basic security constraints applied.
   *
   * @param url See {@link #getUrl()}
   * @param extraConstraints A list of additional constraints for this particular source.
   * @return New {@link ManifestSource}.
   */
  public static ManifestSource newRegularSource(String url,
      List<ManifestConstraint> extraConstraints) {
    List<ManifestConstraint> all = new ArrayList<>(extraConstraints.size() + 4);
    all.add(new TlsClientCertificateSecurityConstraint(1024));
    all.add(new ClientKeySecurityConstraint(2048));
    all.add(new VerifyDiscoveryApiEntry(url));
    all.add(new ForbidRegistryImplementations());
    all.add(new VerifyApiVersions());
    all.add(new RemoveEmbeddedCatalogues());
    all.addAll(extraConstraints);
    return new ManifestSource(url, all);
  }

  /**
   * Returns a new instance of {@link ManifestSource}, with no constraints whatsoever. This should
   * be used for URLs we really trust.
   *
   * @param url See {@link #getUrl()}.
   * @return New {@link ManifestSource}.
   */
  public static ManifestSource newTrustedSource(String url) {
    return new ManifestSource(url, Lists.newArrayList());
  }

  private final String url;

  private final List<ManifestConstraint> constraints;

  private ManifestSource(String url, List<ManifestConstraint> constraints) {
    this.url = url;
    this.constraints = constraints;

    // Make sure that the URL is valid and uses a safe protocol.

    try {
      URI realUri = new URI(url);
      if (!realUri.getScheme().equalsIgnoreCase("https")) {
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
   * @return An URL at which the manifest file can be found (will always be a valid HTTPS URL).
   */
  public String getUrl() {
    return this.url;
  }

  @Override
  public String toString() {
    return "ManifestSource[" + this.getUrl() + ", " + this.getConstraints().size()
        + " constraint(s)]";
  }
}
