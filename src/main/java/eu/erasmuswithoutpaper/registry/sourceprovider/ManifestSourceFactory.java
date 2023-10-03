package eu.erasmuswithoutpaper.registry.sourceprovider;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.constraints.ClientKeyConstraint;
import eu.erasmuswithoutpaper.registry.constraints.ConstraintFactory;
import eu.erasmuswithoutpaper.registry.constraints.ManifestConstraint;
import eu.erasmuswithoutpaper.registry.constraints.RemoveEmbeddedCatalogues;
import eu.erasmuswithoutpaper.registry.constraints.ServerKeySecurityConstraint;
import eu.erasmuswithoutpaper.registry.constraints.TlsClientCertificateSecurityConstraint;
import eu.erasmuswithoutpaper.registry.constraints.VerifyDiscoveryApiEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ManifestSourceFactory {
  @Autowired
  private ConstraintFactory constraintFactory;

  /**
   * Returns a new instance of {@link ManifestSource} with basic security constraints applied.
   *
   * @param url Manifest source's URL.
   * @param extraConstraints A list of additional constraints for this particular source.
   * @return New {@link ManifestSource}.
   */
  public ManifestSource newRegularSource(String url, List<ManifestConstraint> extraConstraints) {
    List<ManifestConstraint> all = new ArrayList<>(extraConstraints.size() + 4);
    all.add(new TlsClientCertificateSecurityConstraint(1024));
    all.add(new ClientKeyConstraint(2048));
    all.add(new ServerKeySecurityConstraint(2048));
    all.add(constraintFactory.getForbidRegistryImplementations());
    all.add(constraintFactory.getApiUniqueConstraint());
    all.add(constraintFactory.getEndpointUniqueConstraint());
    all.add(new VerifyDiscoveryApiEntry(url));
    all.add(constraintFactory.getVerifyApiVersions());
    all.add(new RemoveEmbeddedCatalogues());
    all.addAll(extraConstraints);
    return new ManifestSource(url, all);
  }

  /**
   * Returns a new instance of {@link ManifestSource}, with no constraints whatsoever. This should
   * be used for URLs we really trust.
   *
   * @param url Manifest source's URL.
   * @return New {@link ManifestSource}.
   */
  public ManifestSource newTrustedSource(String url) {
    return new ManifestSource(url, new ArrayList<>());
  }
}
