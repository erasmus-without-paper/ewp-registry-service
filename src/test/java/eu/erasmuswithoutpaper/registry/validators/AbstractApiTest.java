package eu.erasmuswithoutpaper.registry.validators;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet;
import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepositoryImpl;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSourceFactory;
import eu.erasmuswithoutpaper.registry.sourceprovider.TestManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdater;
import eu.erasmuswithoutpaper.registry.web.SelfManifestProvider;
import eu.erasmuswithoutpaper.registry.web.UiController;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import org.apache.xerces.impl.dv.util.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractApiTest<StateType extends SuiteState> extends WRTest {
  @Autowired
  protected UiController uiController;

  @Autowired
  protected FakeInternet internet;

  @Autowired
  protected ManifestRepositoryImpl repo;

  @Autowired
  protected SelfManifestProvider selfManifestProvider;

  @Autowired
  protected TestManifestSourceProvider sourceProvider;

  @Autowired
  protected RegistryUpdater registryUpdater;

  @Autowired
  protected RegistryClient client;

  @Autowired
  protected ValidatorKeyStoreSet validatorKeyStoreSet;

  @Autowired
  private ManifestSourceFactory manifestFactory;

  protected ValidatorKeyStore serviceKeyStore = new ValidatorKeyStore(
      Arrays.asList("test.hei01.uw.edu.pl", "test.hei02.uw.edu.pl")
  );

  private static final String apiManifestUrl = "https://university.example.com/manifest.xml";

  private static boolean needsReinit;
  /**
   * KeyPair to be used for signing responses of our test services.
   */
  protected static KeyPair myKeyPair;

  protected abstract ApiValidator<StateType> getValidator();

  /**
   * Run the validator and create a formatted report of its results.
   *
   * <p>
   * We use this intermediate format to make our tests a bit more understandable.
   * </p>
   *
   * @param url
   *     The URL which to test.
   * @param semanticVersion
   *     version to test.
   * @param security
   *     security to test.
   * @return Report contents.
   */
  protected String getValidatorReport(String url,
      SemanticVersion semanticVersion,
      HttpSecurityDescription security) {
    List<ValidationStepWithStatus> results =
        getValidationReportSteps(url, semanticVersion, security);

    TestValidationReport testValidationReport = new TestValidationReport(results, false);
    return testValidationReport.toString();
  }

  protected String getValidationReport(FakeInternetService service, String url,
      SemanticVersion version, HttpSecurityDescription security) {
    this.internet.addFakeInternetService(service);
    String result = this.getValidatorReport(url, version, security);
    this.internet.removeFakeInternetService(service);
    this.internet.clearAll();
    return result;
  }

  protected List<ValidationStepWithStatus> getValidationReportSteps(String url,
      SemanticVersion semanticVersion,
      HttpSecurityDescription security) {
    return getValidationReportSteps(url, semanticVersion, security, new ValidationParameters());
  }

  protected List<ValidationStepWithStatus> getValidationReportSteps(String url,
      SemanticVersion semanticVersion,
      HttpSecurityDescription security,
      ValidationParameters validationParameters) {
    return getValidator().runTests(url, semanticVersion, security, validationParameters);
  }

  protected void serviceTestContains(FakeInternetService service, String url,
      List<String> expected) {
    assertThat(getValidationReport(service, url, getVersion(), getSecurity())).contains(expected);
  }

  protected void serviceTest(FakeInternetService service, String url, String filename) {
    assertThat(getValidationReport(service, url, getVersion(), getSecurity()))
        .isEqualTo(this.getFileAsString(filename));
  }

  /*
  `needsReinit` static variable is shared among all classes inherited from AbstractApiTest.
  We need to reset it to `true` between running tests from different classes to perform
  manifest initialization when `setUp` method is called.
   */
  @BeforeAll
  public static void setUpClass() {
    needsReinit = true;
  }

  @BeforeEach
  public void setUp() {
    if (needsReinit) {
      /*
       * Minimal setup for the services to guarantee that repo contains a valid catalogue,
       * consistent with the certificates returned by the validator.
       */
      this.sourceProvider.clearSources();
      this.repo.deleteAll();
      this.internet.clearAll();

      Map<String,String> registryManifests = this.selfManifestProvider.getManifests();

      for (String myManifestName : registryManifests.keySet()) {
        String myManifest = registryManifests.get(myManifestName);
        String myUrl = "https://registry.example.com/manifest-" + myManifestName + ".xml";
        this.internet.putURL(myUrl, myManifest);
        this.sourceProvider.addSource(manifestFactory.newTrustedSource(myUrl));
      }

      String apiManifest = this.getFileAsString(getManifestFilename());
      myKeyPair = this.getValidator().getValidatorKeyStoreSet().getMainKeyStore().generateKeyPair();
      if (this.getValidator().getValidatorKeyStoreSet().getSecondaryKeyStore() != null) {
        apiManifest = apiManifest.replace(
            "SERVER-KEY-PLACEHOLDER-SECONDARY",
            Base64.encode(
                this.serviceKeyStore.getTlsKeyPairInUse().getPublic().getEncoded()
            )
        );
      }
      apiManifest = apiManifest.replace(
          "SERVER-KEY-PLACEHOLDER",
          Base64.encode(myKeyPair.getPublic().getEncoded())
      );
      this.internet.putURL(apiManifestUrl, apiManifest);
      this.sourceProvider
          .addSource(manifestFactory.newRegularSource(apiManifestUrl, Lists.newArrayList()));

      this.registryUpdater.reloadAllManifestSources();
      needsReinit = false;
    }
  }

  protected abstract String getManifestFilename();

  protected TestValidationReport getRawReport(FakeInternetService service) {
    return getRawReport(service, new ValidationParameters());
  }

  protected TestValidationReport getRawReport(FakeInternetService service,
      ValidationParameters validationParameters) {
    String url = getUrl();
    SemanticVersion semanticVersion = getVersion();
    HttpSecurityDescription security = getSecurity();
    this.internet.addFakeInternetService(service);
    List<ValidationStepWithStatus> results =
        getValidationReportSteps(url, semanticVersion, security, validationParameters);
    this.internet.removeFakeInternetService(service);
    this.internet.clearAll();
    return new TestValidationReport(results);
  }

  protected abstract String getUrl();

  protected HttpSecurityDescription getSecurity() {
    // By default we use HTTT in tests, it doesn't matter except in Echo, but there tests are more
    // fine grained and don't use this API.
    return new HttpSecurityDescription(
        CombEntry.CLIAUTH_HTTPSIG,
        CombEntry.SRVAUTH_TLSCERT,
        CombEntry.REQENCR_TLS,
        CombEntry.RESENCR_TLS
    );
  }

  protected abstract SemanticVersion getVersion();
}
