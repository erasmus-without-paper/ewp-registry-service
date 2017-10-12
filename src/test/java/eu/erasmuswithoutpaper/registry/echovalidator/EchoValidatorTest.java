package eu.erasmuswithoutpaper.registry.echovalidator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

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

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class EchoValidatorTest extends WRTest {

  private static String selfManifestUrl;
  private static String echoManifestUrl;
  private static String echoV1Url;
  private static String echoUrlTTTT;
  private static String echoUrlSTTT;
  private static String echoUrlHTTT;
  private static String echoUrlMTTT;

  @BeforeClass
  public static void setUpClass() {
    selfManifestUrl = "https://registry.example.com/manifest.xml";
    echoManifestUrl = "https://university.example.com/manifest.xml";
    echoV1Url = "https://university.example.com/echo/v1/";
    echoUrlTTTT = "https://university.example.com/echo/TTTT/";
    echoUrlSTTT = "https://university.example.com/echo/STTT/";
    echoUrlHTTT = "https://university.example.com/echo/HTTT/";
    echoUrlMTTT = "https://university.example.com/echo/MTTT/";

    // https://github.com/adamcin/httpsig-java/issues/9
    Locale.setDefault(Locale.US);
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

    String echoManifest = this.getFileAsString("echovalidator/manifest.xml");
    this.internet.putURL(echoManifestUrl, echoManifest);
    this.sourceProvider
        .addSource(ManifestSource.newRegularSource(echoManifestUrl, Lists.newArrayList()));

    this.registryUpdater.reloadAllManifestSources();
  }

  @Test
  public void testAgainstServiceHTTTInvalid1() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid1(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlHTTT)).contains("FAILURE",
          "HTTP 200 expected, but HTTP 400 received.",
          "This endpoint requires HTTP Signature to use one of the following algorithms: RSA_SHA512");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid10() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid10(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT);
      assertThat(out).containsOnlyOnce("FAILURE");
      assertThat(out)
          .contains("FAILURE: Trying HTTPSIG Client Authentication with an invalid Digest.");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid11() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid11(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT);
      assertThat(out).containsOnlyOnce("WARNING");
      assertThat(out).contains(
          "WARNING: Trying HTTPSIG Client Authentication with non-canonical X-Request-ID");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid2() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid2(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT);
      // We expect only a single test to fail here.
      assertThat(out).containsOnlyOnce("FAILURE");
      assertThat(out).contains(
          "### FAILURE: Trying HTTPSIG Client Authentication with Original-Date (instead of Date). "
              + "Expecting to receive a valid HTTP 200 response.\n\n"
              + "HTTP 200 expected, but HTTP 400 received.\n"
              + "This endpoint requires your request to include the \"Date\" header.");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid3() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid3(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT);
      assertThat(out).containsOnlyOnce("FAILURE");
      assertThat(out).contains(
          "### FAILURE: Trying HTTPSIG Client Authentication with unsigned x-request-id header. "
              + "Expecting to receive a valid HTTP 400 or HTTP 401 error response.\n\n"
              + "HTTP 400 or HTTP 401 expected, but HTTP 200 received.");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid4() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid4(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT);
      assertThat(out).containsOnlyOnce("FAILURE");
      assertThat(out).contains(
          "FAILURE: Trying HTTPSIG Client Authentication with some extra unknown, but properly signed headers");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid5() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid5(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).containsOnlyOnce("WARNING");
      assertThat(out).contains(
          "It is RECOMMENDED for HTTP 401 responses to contain a proper Want-Digest header");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid6() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid6(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT);
      assertThat(out).doesNotContain("FAILURE");
      assertThat(out).doesNotContain("ERROR");
      assertThat(out).containsOnlyOnce("WARNING");
      assertThat(out).contains("If you want to include the \"headers\" property in your "
          + "WWW-Authenticate header, then it should contain at least all required values");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid7() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid7(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT);
      assertThat(out).containsOnlyOnce("FAILURE");
      assertThat(out).contains(
          "FAILURE: Trying HTTPSIG Client Authentication signed with a server key, instead of a client key. "
              + "Expecting to receive a valid HTTP 403 error response");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid8() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid8(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT);
      assertThat(out).containsOnlyOnce("FAILURE");
      assertThat(out)
          .contains("FAILURE: Trying HTTPSIG Client Authentication with an unsynchronized clock");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTInvalid9() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTInvalid9(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlHTTT);
      assertThat(out)
          .contains("FAILURE: Trying HTTPSIG Client Authentication with a known keyId, but "
              + "invalid signature");
      assertThat(out).contains("FAILURE: Trying HTTPSIG Client Authentication with missing headers "
          + "that were supposed to be signed");
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceHTTTValid() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceHTTTValid(echoUrlHTTT, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlHTTT))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceHTTTValid.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMTTTInvalid1() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceMTTTInvalid1(echoUrlMTTT, client);
      this.internet.addFakeInternetService(service);
      String out = this.getValidatorReport(echoUrlMTTT);
      assertThat(out).containsOnlyOnce("FAILURE");
      assertThat(out)
          .contains("FAILURE: Accessing your Echo API without any form of client authentication");
      this.internet.removeFakeInternetService(service);
    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceMTTTValid() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceMTTTValid(echoUrlMTTT, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlMTTT))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceMTTTValid.txt"));
      this.internet.removeFakeInternetService(service);
    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTTInvalid1() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceSTTTInvalid1(echoUrlSTTT, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlSTTT))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceSTTTInvalid1.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTTInvalid2() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceSTTTInvalid2(echoUrlSTTT, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlSTTT))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceSTTTInvalid2.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTTInvalid3() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceSTTTInvalid3(echoUrlSTTT, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlSTTT))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceSTTTInvalid3.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceSTTTValid() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceSTTTValid(echoUrlSTTT, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlSTTT))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceSTTTValid.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceTTTTDummy() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceTTTTDummy(echoUrlTTTT, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoUrlTTTT))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceTTTTDummy.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceV1Invalid1() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceV1Invalid1(echoV1Url, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoV1Url))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceV1Invalid1.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceV1Invalid2() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceV1Invalid2(echoV1Url, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoV1Url))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceV1Invalid2.txt"));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  @Test
  public void testAgainstServiceV1Valid() {
    RegistryClient client = this.createClient();
    try {
      FakeInternetService service;

      service = new ServiceV1Valid(echoV1Url, client);
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(echoV1Url))
          .isEqualTo(this.getFileAsString("echovalidator/ServiceV1Valid.txt"));
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
        if (result.getServerDeveloperErrorMessage().isPresent()) {
          sb.append(result.getServerDeveloperErrorMessage().get()).append("\n");
        }
        sb.append("\n\n");
      }
    }
    return sb.toString();
  }

}
