package eu.erasmuswithoutpaper.registry.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.constraints.RestrictInstitutionsCovered;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepositoryImpl;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.TestManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdaterTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.Lists;
import org.junit.Test;

/**
 * Tests for {@link ApiController}.
 */
public class ApiControllerTest extends WRTest {

  @Autowired
  private TestManifestSourceProvider manifestSourceProvider;

  @Autowired
  private ManifestRepositoryImpl repo;

  @Autowired
  private FakeInternet internet;

  @Autowired
  private ApiController apiController;

  @Autowired
  private UiController uiController;

  /**
   * This test is a bit similar to {@link RegistryUpdaterTest#testScenario1()}, but it acts directly
   * on the API HTTP endpoints, and the scenario is a different (e.g. the {@link NotifierService} is
   * not tested here).
   *
   * <p>
   * This scenario is a pretty direct copy of the actions performed during the "demo" of the beta
   * version of the Registry Service performed on EWP meeting in Warsaw, June 2016.
   * </p>
   */
  @Test
  public void testScenario1() {

    /* Set up manifest sources. */

    this.manifestSourceProvider.clearSources();
    String urlSelf, urlPL, urlSE;
    // urlSelf = "https://registry.erasmuswithoutpaper.eu/manifest.xml";
    urlPL = "https://schowek.usos.edu.pl/w.rygielski/ewp/uw.edu.pl/manifest.xml";
    urlSE = "https://schowek.usos.edu.pl/w.rygielski/ewp/ladok.se/manifest.xml";
    // this.manifestSourceProvider.addSource(ManifestSource.newTrustedSource(urlSelf));
    this.manifestSourceProvider.addSource(ManifestSource.newRegularSource(urlPL,
        Lists.newArrayList(new RestrictInstitutionsCovered("^uw\\.edu\\.pl$"))));
    this.manifestSourceProvider.addSource(ManifestSource.newRegularSource(urlSE,
        Lists.newArrayList(new RestrictInstitutionsCovered("^.+\\.se$"))));
    this.repo.deleteAll();
    // this.internet.putURL(urlSelf, this.apiController.getSelfManifest().getBody());

    /* Call the refresh URL and verify its contents. */

    String result;
    result = this.uiController.refresh().getBody();
    assertThat(result).contains("successfully queued");

    /*
     * Since we are SyncTaskExecutor in tests, we may assume that the refresh is already completed.
     *
     * [Test A] At first, the catalogue should be empty, because none of the URLs was reachable.
     */

    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/A-out.xml"));
    // assertThat(this.status(urlSelf)).contains("Last access status: OK");
    assertThat(this.status(urlPL)).contains("unable to fetch");
    assertThat(this.status(urlSE)).contains("unable to fetch");

    /* [Test B] Let's add a simple, empty, valid manifest for Poland. */

    this.internet.putURL(urlPL, this.getFile("demo1/B-inPL.xml"));

    /* The calogue is not yet refreshed, because we didn't call refresh. */

    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/A-out.xml"));

    /* Refresh it. */

    this.uiController.refresh();
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/B-out.xml"));
    assertThat(this.status(urlPL)).contains("Last access status: OK");
    assertThat(this.status(urlPL)).doesNotContain("unable to fetch");

    /* [Test C] Define one HEI. */

    this.internet.putURL(urlPL, this.getFile("demo1/C-inPL.xml"));
    this.uiController.refresh();
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/C-out.xml"));

    /* [Test D] Put a similar manifest into the Swedish URL. We'll use two HEIs this time. */

    this.internet.putURL(urlSE, this.getFile("demo1/D-inSE.xml"));
    this.uiController.refresh();
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/D-out.xml"));

    /* [Test E] Add some API definitions - Echo API and some arbitrary API. */

    this.internet.putURL(urlSE, this.getFile("demo1/E-inSE.xml"));
    this.uiController.refresh();
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/E-out.xml"));

    /* [Test F] Try to use suspicious TLS client certificates (2 invalid, 1 obsolete). */

    this.internet.putURL(urlSE, this.getFile("demo1/F-inSE.xml"));
    this.uiController.refresh();
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/F-out.xml"));
    assertThat(this.status(urlSE))
        .contains("minimum required length of TLS client certificate key is 1024 bits");
    assertThat(this.status(urlSE))
        .contains("One of your TLS client certificates (1st of 3) uses 512 bits only");
    assertThat(this.status(urlSE))
        .contains("One of your TLS client certificates (2nd of 3) uses an insecure "
            + "MD-based signature algorithm (MD5withRSA)");
    assertThat(this.status(urlSE))
        .contains("One of your TLS client certificates (3rd of 3) uses a SHA-1-based "
            + "signature algorithm (SHA1withRSA). Consider upgrading to SHA-256.");

    /* [Test G] Replace certificate with a valid one. */

    this.internet.putURL(urlSE, this.getFile("demo1/G-inSE.xml"));
    this.uiController.refresh();
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/G-out.xml"));

    /* [Test H] Try to invade Poland. */

    this.internet.putURL(urlSE, this.getFile("demo1/H-inSE.xml"));
    this.uiController.refresh();
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/G-out.xml"));
    assertThat(this.status(urlSE)).contains(
        "Institution <code>uw.edu.pl</code> didn't match the <code>^.+\\.se$</code> filter pattern");

    /*
     * [Test I] Add a second manifest source for uw.edu.pl. Define some alternative PIC and Erasmus
     * codes, along with some alternative names. Expect all of them to be imported, but each unique
     * value should be present exactly once.
     */

    String urlPL2 = "https://schowek.usos.edu.pl/w.rygielski/ewp/uw.edu.pl/manifest2.xml";
    this.manifestSourceProvider.addSource(ManifestSource.newRegularSource(urlPL2,
        Lists.newArrayList(new RestrictInstitutionsCovered("^uw\\.edu\\.pl$"))));
    this.internet.putURL(urlPL2, this.getFile("demo1/I-inPL2.xml"));
    this.uiController.refresh();
    assertThat(this.status(urlPL2)).contains("Last access status: OK");
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/I-out.xml"));

    /*
     * [Test J] Add the <hack-attempt> element to the manifest source. Expect the hack to be
     * detected and neutralized.
     */

    this.internet.putURL(urlPL2, this.getFile("demo1/J-inPL2.xml"));
    this.uiController.refresh();
    assertThat(this.status(urlPL2)).contains("Last access status: Warning");
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/J-out.xml"));
  }

  /**
   * @return Catalogue body with all binary content replaced with placeholders.
   */
  private String getCatalogueBodyWithoutBinaries() {
    String body = this.apiController.getCatalogue().getBody();
    // Replace fingerprints
    body = body.replaceAll("\"[0-9a-f]{64,64}\"", "\"(SHA-256 fingerprint here)\"");
    // Replace base64 values... (this one is hackish, but it's enough for tests).
    body = body.replaceAll(">\\n(            [^ <>]+\n)+        <",
        ">\n            (Base64-encoded content here)\n        <");
    return body;
  }

  /**
   * Return the <b>content</b> (as a String) of the Manifest status page, for the given manifest
   * URL.
   */
  private String status(String url) {
    ResponseEntity<String> response = this.uiController.manifestStatus(url);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    return response.getBody();
  }
}


