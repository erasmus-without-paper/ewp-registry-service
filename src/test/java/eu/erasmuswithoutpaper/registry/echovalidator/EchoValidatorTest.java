package eu.erasmuswithoutpaper.registry.echovalidator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.echovalidator.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet;
import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.repository.CatalogueNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepositoryImpl;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.TestManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdater;
import eu.erasmuswithoutpaper.registry.web.SelfManifestProvider;
import eu.erasmuswithoutpaper.registryclient.CatalogueFetcher;
import eu.erasmuswithoutpaper.registryclient.ClientImpl;
import eu.erasmuswithoutpaper.registryclient.ClientImplOptions;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import eu.erasmuswithoutpaper.registryclient.RegistryClient.RefreshFailureException;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EchoValidatorTest extends WRTest {

  private static String selfManifestUrl;
  private static String echoUrl;

  @BeforeClass
  public static void setUpClass() {
    selfManifestUrl = "https://example.com/self-manifest.xml";
    echoUrl = "https://example.com/echo/";
  }

  @Autowired
  private FakeInternet internet;

  @Autowired
  private EchoValidator validator;

  @Autowired
  private ManifestRepositoryImpl repo;

  @Autowired
  private SelfManifestProvider selfManifestProvider;

  @Autowired
  private TestManifestSourceProvider sourceProvider;

  @Autowired
  private RegistryUpdater registryUpdater;

  @Before
  public void setUp() {
    /*
     * Minimal setup for the services to guarantee that repo contains a valid catalogue, consistent
     * with the certificates returned by the validator.
     */
    this.sourceProvider.clearSources();
    this.repo.deleteAll();
    this.internet.clearAll();
    String myManifest = this.selfManifestProvider.getManifest();
    this.internet.putURL(selfManifestUrl, myManifest);
    this.sourceProvider.addSource(ManifestSource.newTrustedSource(selfManifestUrl));
    this.registryUpdater.reloadAllManifestSources();
  }

  @Test
  public void testAgainstInternalServices() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new Service1(echoUrl, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrl))
          .isEqualTo(this.getFileAsString("echovalidator/result1.txt"));
      this.internet.removeFakeInternetService(service);

      service = new Service2(echoUrl, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrl))
          .isEqualTo(this.getFileAsString("echovalidator/result2.txt"));
      this.internet.removeFakeInternetService(service);

      service = new Service3(echoUrl, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrl))
          .isEqualTo(this.getFileAsString("echovalidator/result3.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  private RegistryClient createClient() {
    ClientImplOptions options = new ClientImplOptions();
    options.setCatalogueFetcher(new CatalogueFetcher() {

      @Override
      public RegistryResponse fetchCatalogue(String etag) throws IOException {
        try {
          return new Http200RegistryResponse(
              EchoValidatorTest.this.repo.getCatalogue().getBytes(StandardCharsets.UTF_8), etag,
              null);
        } catch (CatalogueNotFound e) {
          throw new RuntimeException(e);
        }
      }
    });
    RegistryClient client = new ClientImpl(options);
    try {
      client.refresh();
    } catch (RefreshFailureException e) {
      throw new RuntimeException(e);
    }
    return client;
  }

  /**
   * Run the validator and create a formatted report of its results.
   *
   * <p>
   * We use this intermediate format to make our tests a bit more understandable.
   * </p>
   *
   * @param url The URL which to test.
   * @return Report contents.
   */
  private String getValidatorReport(String url) {
    List<ValidationStepWithStatus> results = this.validator.runTests(url);

    StringBuilder sb = new StringBuilder();
    for (ValidationStepWithStatus result : results) {
      if (!result.getStatus().equals(Status.SUCCESS)) {
        sb.append('\n');
      }
      sb.append("### ").append(result.getStatus()).append(": ").append(result.getName())
          .append('\n');
      if (!result.getStatus().equals(Status.SUCCESS)) {
        sb.append('\n');
        sb.append(result.getMessage()).append('\n');
        sb.append("\n\n");
      }
    }
    return sb.toString();
  }

}
