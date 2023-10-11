package eu.erasmuswithoutpaper.registry.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.WRIntegrationTest;
import eu.erasmuswithoutpaper.registry.constraints.RestrictInstitutionsCovered;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepositoryImpl;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSourceFactory;
import eu.erasmuswithoutpaper.registry.sourceprovider.TestManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdaterTest;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import org.junit.jupiter.api.Test;

/**
 * Check if the URLs served by our {@link ApiController} are actually served.
 */
@TestPropertySource(properties = { "app.use-flag-to-notify-about-exceptions=true" })
public class ApiControllerIntegrationTest extends WRIntegrationTest {

  @Autowired
  private TestRestTemplate template;

  @Autowired
  private EwpDocBuilder docBuilder;

  @Autowired
  private RegistryClient client;

  @Autowired
  private ManifestRepositoryImpl repo;

  @Autowired
  private SelfManifestProvider selfManifestProvider;

  @Autowired
  private NotifierService notifier;

  @Autowired
  private ApiController apiController;

  @Autowired
  private TestManifestSourceProvider manifestSourceProvider;

  @Autowired
  private FakeInternet internet;

  @Autowired
  private ManifestSourceFactory manifestFactory;

  /**
   * Check if requests to non-existent files end with a proper HTTP 404 response.
   */
  @Test
  public void respondsValid404() {
    ResponseEntity<String> response = this.template.getForEntity("/nonexistent", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    BuildParams params = new BuildParams(response.getBody());
    params.setExpectedKnownElement(KnownElement.COMMON_ERROR_RESPONSE);
    BuildResult result = this.docBuilder.build(params);
    assertThat(result.isValid()).isTrue();
  }

  /**
   * Check if runtime exceptions cause a proper HTTP 500 response to be rendered.
   */
  @Test
  public void respondsValid500() {
    /* At the same time, we will check if the notifier picks it up. Before exception is called,
     * there should be no errors. */
    int size = this.notifier.getAllErroredFlags().size();
    ResponseEntity<String> response = this.template.getForEntity("/throw-exception", String.class);
    assertThat(this.notifier.getAllErroredFlags()).hasSize(size + 1);
    assertThat(this.notifier.getAllErroredFlags().get(0).getName())
        .contains("Recently recorded runtime errors");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    BuildParams params = new BuildParams(response.getBody());
    params.setExpectedKnownElement(KnownElement.COMMON_ERROR_RESPONSE);
    BuildResult result = this.docBuilder.build(params);
    assertThat(result.isValid()).isTrue();
  }

  /**
   * Check if the catalogue is being served. Note, that we are just testing if the endpoint is
   * properly connected with the repository. We don't care about the contents here.
   */
  @Test
  public void servesTheCatalogue() {
    this.repo.putCatalogue("<xml/>", client);
    ResponseEntity<String> response = this.template.getForEntity("/catalogue-v1.xml", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("<xml/>");
  }

  /**
   * Check if the manifests are being served. Same, as above - just testing if it is properly
   * connected with {@link SelfManifestProvider}.
   */
  @Test
  public void servesTheManifest() {
    Map<String, String> manifests = this.selfManifestProvider.getManifests();

    for (String manifestName : manifests.keySet()) {
      ResponseEntity<String> response = this.template
          .getForEntity("/manifest-" + manifestName + ".xml", String.class);
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isEqualTo(manifests.get(manifestName));
    }
  }

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
    String urlPL, urlSE;
    // urlSelf = "https://registry.erasmuswithoutpaper.eu/manifest.xml";
    urlPL = "https://schowek.usos.edu.pl/w.rygielski/ewp/uw.edu.pl/manifest.xml";
    urlSE = "https://schowek.usos.edu.pl/w.rygielski/ewp/ladok.se/manifest.xml";
    // this.manifestSourceProvider.addSource(manifestFactory.newTrustedSource(urlSelf));
    this.manifestSourceProvider.addSource(manifestFactory.newRegularSource(urlPL,
        Collections.singletonList(new RestrictInstitutionsCovered("^uw\\.edu\\.pl$"))));
    this.manifestSourceProvider.addSource(manifestFactory.newRegularSource(urlSE,
        Collections.singletonList(new RestrictInstitutionsCovered("^.+\\.se$"))));
    this.repo.deleteAll(client);
    // this.internet.putURL(urlSelf, this.apiController.getSelfManifest().getBody());

    /* Call the refresh URL and verify its response status. */

    int status;
    status = this.forceReload(urlPL);
    assertThat(status).isEqualTo(200);
    status = this.forceReload(urlSE);
    assertThat(status).isEqualTo(200);
    status = this.forceReload("non-existing");
    assertThat(status).isEqualTo(400);

    /* Since we are SyncTaskExecutor in tests, we may assume that the refresh is already completed.
     * [Test A] At first, the catalogue should be empty, because none of the URLs was reachable. */

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

    this.forceReload(urlPL);
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/B-out.xml"));
    assertThat(this.status(urlPL))
        .containsPattern("Last access status:[ \n]+<code class='ewpst__bordered-code'>OK</code>");
    assertThat(this.status(urlPL)).doesNotContain("unable to fetch");

    /* [Test C] Define one HEI. */

    this.internet.putURL(urlPL, this.getFile("demo1/C-inPL.xml"));
    this.forceReload(urlPL);
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/C-out.xml"));

    /* [Test D] Put a similar manifest into the Swedish URL. */

    this.internet.putURL(urlSE, this.getFile("demo1/D-inSE.xml"));
    this.forceReload(urlSE);
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/D-out.xml"));

    /* [Test E] Add some API definitions - Echo API and some arbitrary API. */

    this.internet.putURL(urlSE, this.getFile("demo1/E-inSE.xml"));
    this.forceReload(urlSE);
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/E-out.xml"));

    /* [Test H] Try to invade Poland. */

    this.internet.putURL(urlSE, this.getFile("demo1/H-inSE.xml"));
    this.forceReload(urlSE);
    assertThat(this.status(urlSE)).contains(
        "Institution <code>uw.edu.pl</code> didn't match the <code>^.+\\.se$</code> filter pattern");

    /* [Test I] Add a second manifest source for uw.edu.pl. Define some alternative PIC and Erasmus
     * codes, along with some alternative names. Expect all of them to be imported, but each unique
     * value should be present exactly once. */

    this.internet.putURL(urlSE, this.getFile("demo1/E-inSE.xml"));
    this.forceReload(urlSE);
    String urlPL2 = "https://schowek.usos.edu.pl/w.rygielski/ewp/uw.edu.pl/manifest2.xml";
    this.manifestSourceProvider.addSource(manifestFactory.newRegularSource(urlPL2, Collections
        .singletonList(new RestrictInstitutionsCovered("^(uw\\.edu\\.pl)|(university-a\\.edu)$"))));
    this.internet.putURL(urlPL2, this.getFile("demo1/I-inPL2.xml"));
    this.forceReload(urlPL2);
    assertThat(this.status(urlPL2))
        .containsPattern("Last access status:[ \n]+<code class='ewpst__bordered-code'>OK</code>");
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/I-out.xml"));

    /* [Test J] Add the <hack-attempt> element to the manifest source. Expect the hack to be
     * detected and neutralized. */

    this.internet.putURL(urlPL2, this.getFile("demo1/J-inPL2.xml"));
    this.forceReload(urlPL2);
    assertThat(this.status(urlPL2)).containsPattern(
        "Last access status:[ \n]+<code class='ewpst__bordered-code'>Warning</code>");
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/J-out.xml"));

    /* [Test K] Replace with manifest file with multiple hosts. Expect this raise error and to
     * ignore extra hosts. */

    this.internet.putURL(urlPL2, this.getFile("demo1/K-inPL2.xml"));
    this.forceReload(urlPL2);
    assertThat(this.status(urlPL2)).containsPattern(
        "Last access status:[ \n]+<code class='ewpst__bordered-code'>Error</code>");
    assertThat(this.getCatalogueBodyWithoutBinaries())
        .isEqualTo(this.getFileAsString("demo1/J-out.xml"));
  }

  private int forceReload(String manifestUrl) {
    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("url", manifestUrl);
    ResponseEntity<String> response = this.template.postForEntity("/reload", params, String.class);
    return response.getStatusCodeValue();
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
    ResponseEntity<String> response = this.template.getForEntity("/status?url=" + url,
        String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).contains("<body>");
    return response.getBody();
  }

}
