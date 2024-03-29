package eu.erasmuswithoutpaper.registry.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.constraints.ClientKeyConstraint;
import eu.erasmuswithoutpaper.registry.constraints.ConstraintFactory;
import eu.erasmuswithoutpaper.registry.constraints.FailedConstraintNotice;
import eu.erasmuswithoutpaper.registry.constraints.ManifestConstraint;
import eu.erasmuswithoutpaper.registry.constraints.RestrictInstitutionsCovered;
import eu.erasmuswithoutpaper.registry.constraints.ServerKeySecurityConstraint;
import eu.erasmuswithoutpaper.registry.constraints.TlsClientCertificateSecurityConstraint;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

/**
 * Tests for {@link SelfManifestProvider}.
 */
public class SelfManifestProviderTest extends WRTest {

  @Autowired
  private SelfManifestProvider provider;

  @Autowired
  private EwpDocBuilder docBuilder;

  @Autowired
  private RegistryClient registryClient;

  @Value("${app.root-url}")
  private String rootUrl;

  @Autowired
  private ConstraintFactory constraintFactory;

  /**
   * Check if the manifest returned is valid.
   */
  @Test
  public void producesAValidManifest() {
    Map<String, String> manifests = this.provider.getManifests();

    for (String manifestName : manifests.keySet()) {
      BuildParams params = new BuildParams(manifests.get(manifestName));
      params.setExpectedKnownElement(KnownElement.RESPONSE_MANIFEST_V6);
      BuildResult result = this.docBuilder.build(params);
      assertThat(result.getErrors()).isEmpty();
      assertThat(result.isValid()).isTrue();

      Document doc = result.getDocument().get();

      List<ManifestConstraint> constraints = new ArrayList<>();
      constraints.add(new TlsClientCertificateSecurityConstraint(2048));
      constraints.add(new ClientKeyConstraint(2048));
      constraints.add(new ServerKeySecurityConstraint(2048));
      constraints
          .add(new RestrictInstitutionsCovered("^.*\\.developers\\.erasmuswithoutpaper\\.eu$"));
      constraints.add(constraintFactory.getVerifyApiVersions());
      for (ManifestConstraint c : constraints) {
        List<FailedConstraintNotice> notices = c.filter(doc, registryClient);
        assertThat(notices).isEmpty();
      }
    }
  }
}
