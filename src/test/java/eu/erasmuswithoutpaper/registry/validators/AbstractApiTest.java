package eu.erasmuswithoutpaper.registry.validators;

import java.security.KeyPair;
import java.util.List;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepositoryImpl;
import eu.erasmuswithoutpaper.registry.sourceprovider.TestManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdater;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.web.SelfManifestProvider;
import eu.erasmuswithoutpaper.registry.web.UiController;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.beans.factory.annotation.Autowired;

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
  protected ValidatorKeyStore validatorKeyStore;

  protected static boolean needsReinit;
  protected static String selfManifestUrl;
  protected static String apiManifestUrl;
  protected static KeyPair myKeyPair;

  protected abstract ApiValidator<StateType> GetValidator();

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
        GetValidator().runTests(url, semanticVersion, security, new ValidationParameters());

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
