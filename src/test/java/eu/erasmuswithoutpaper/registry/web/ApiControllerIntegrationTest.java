package eu.erasmuswithoutpaper.registry.web;

import static org.assertj.core.api.Assertions.assertThat;

import eu.erasmuswithoutpaper.registry.WRIntegrationTest;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.junit.Before;
import org.junit.Test;


/**
 * Check if the URLs served by our {@link ApiController} are actually served.
 */
public class ApiControllerIntegrationTest extends WRIntegrationTest {

  private TestRestTemplate template;

  @Autowired
  private EwpDocBuilder docBuilder;

  @Autowired
  private ManifestRepository repo;

  @Autowired
  private SelfManifestProvider selfManifestProvider;

  @Autowired
  private NotifierService notifier;

  /**
   * Check if requests to non-existent files end with a proper HTTP 404 response.
   */
  @Test
  public void respondsValid404() {
    ResponseEntity<String> response =
        this.template.getForEntity(this.baseURL + "/nonexistent", String.class);
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
    /*
     * At the same time, we will check if the notifier picks it up. Before exception is called,
     * there should be no errors.
     */
    assertThat(this.notifier.getAllErroredFlags()).hasSize(0);
    ResponseEntity<String> response =
        this.template.getForEntity(this.baseURL + "/throw-exception", String.class);
    assertThat(this.notifier.getAllErroredFlags()).hasSize(1);
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
    this.repo.putCatalogue("<xml/>");
    ResponseEntity<String> response =
        this.template.getForEntity(this.baseURL + "/catalogue-v1.xml", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo("<xml/>");
  }

  /**
   * Check if the manifest is being served. Same, as above - just testing if it is properly
   * connected with {@link SelfManifestProvider}.
   */
  @Test
  public void servesTheManifest() {
    ResponseEntity<String> response =
        this.template.getForEntity(this.baseURL + "/manifest.xml", String.class);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isEqualTo(this.selfManifestProvider.getManifest());
  }

  @Before
  public void setUp() throws Exception {
    this.template = new TestRestTemplate();
  }
}


