package eu.erasmuswithoutpaper.registry.validators;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.PostConstruct;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.githubtags.GitHubTagsGetter;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;


/**
 * Base class for services validating external APIs' implementations.
 */
public abstract class ApiValidator<S extends SuiteState> {
  protected final RegistryClient client;
  protected final EwpDocBuilder docBuilder;
  protected final Internet internet;
  private final String validatedApiName;
  private final ValidatorKeyStoreSet validatorKeyStoreSet;
  private final ApiEndpoint endpoint;
  @Autowired
  protected ApiValidatorsManager apiValidatorsManager;
  @Autowired
  private CatalogueMatcherProvider catalogueMatcherProvider;
  @Autowired
  private GitHubTagsGetter gitHubTagsGetter;

  /**
   * @param docBuilder
   *     Needed for validating API responses against the schemas.
   * @param internet
   *     Needed to make API requests across the network.
   * @param client
   *     Needed to fetch (and verify) APIs' security settings.
   * @param validatorKeyStoreSet
   *     Set of stores providing keys, certificates and covered HEI IDs.
   * @param validatedApiName
   *     lowercase name of API validated by this class.
   */
  public ApiValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet, String validatedApiName) {
    this(docBuilder, internet, client, validatorKeyStoreSet, validatedApiName,
        ApiEndpoint.NoEndpoint);
  }

  public ValidatorKeyStoreSet getValidatorKeyStoreSet() {
    return this.validatorKeyStoreSet;
  }

  /**
   * @param docBuilder
   *     Needed for validating API responses against the schemas.
   * @param internet
   *     Needed to make API requests across the network.
   * @param client
   *     Needed to fetch (and verify) APIs' security settings.
   * @param validatorKeyStoreSet
   *     Set of stores providing keys, certificates and covered HEI IDs.
   * @param validatedApiName
   *     lowercase name of API validated by this class.
   * @param endpoint
   *     lowercase name API endpoint that is validated by this class.
   */
  public ApiValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet, String validatedApiName, ApiEndpoint endpoint) {
    this.docBuilder = docBuilder;
    this.internet = internet;
    this.client = client;

    this.validatorKeyStoreSet = validatorKeyStoreSet;
    this.validatedApiName = validatedApiName;
    this.endpoint = endpoint;
  }

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  protected static <K extends Comparable<? super K>, V> ListMultimap<K, V> createMultimap() {
    return MultimapBuilder.treeKeys().linkedListValues().build();
  }

  private Collection<ValidationSuiteInfo<S>> getCompatibleSuites(
      SemanticVersion version,
      List<ValidationSuiteInfoWithVersions<S>> allSuites) {
    List<ValidationSuiteInfo<S>> result = new ArrayList<>();
    for (ValidationSuiteInfoWithVersions<S> suite : allSuites) {
      if (isVersionInRange(version, suite.minMajorVersion, suite.maxMajorVersion)) {
        result.add(suite.validationSuiteInfo);
      }
    }
    return result;
  }

  /**
   * Checks if version is in specified range.
   *
   * @param version
   *     version to check.
   * @param minMajorVersion
   *     the lowest acceptable major version.
   * @param maxMajorVersion
   *     the greatest acceptable major version.
   * @return
   *     Boolean result of checking if version is in range.
   */
  public static Boolean isVersionInRange(SemanticVersion version, String minMajorVersion,
      String maxMajorVersion) {
    try {
      return
          (version.major >= Integer.parseInt(minMajorVersion))
          && (maxMajorVersion.equals("inf") || version.major <= Integer.parseInt(maxMajorVersion));
    } catch (NumberFormatException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Get list of parameters for all validators compatible with version `version`.
   *
   * @param version
   *     version of the API for which parameters will be returned.
   * @return
   *     List of parameters for all validators compatible with version `version`.
   */
  public List<ValidationParameter> getParameters(SemanticVersion version) {
    List<ValidationParameter> parameters = new ArrayList<>();
    for (ValidationSuiteInfo<S> info : getCompatibleSuites(version, getValidationSuites())) {
      parameters.addAll(info.parameters);
    }
    return parameters;
  }

  public static class ParameterWithVersion {
    public ValidationParameter parameter;
    public String minMajorVersion;
    public String maxMajorVersion;

    /**
     * @param parameter
     *     validation parameter.
     * @param minMajorVersion
     *     the lowest acceptable version.
     * @param maxMajorVersion
     *     the greatest acceptable version.
     */
    public ParameterWithVersion(
        ValidationParameter parameter,
        String minMajorVersion,
        String maxMajorVersion) {
      this.parameter = parameter;
      this.minMajorVersion = minMajorVersion;
      this.maxMajorVersion = maxMajorVersion;
    }
  }

  public static class ValidationSuiteInfoWithVersions<K extends SuiteState> {
    public String minMajorVersion;
    public String maxMajorVersion;
    public ValidationSuiteInfo<K> validationSuiteInfo;

    ValidationSuiteInfoWithVersions(String minMajorVersion,
        String maxMajorVersion,
        ValidationSuiteInfo<K> validationSuiteInfo) {
      this.maxMajorVersion = maxMajorVersion;
      this.minMajorVersion = minMajorVersion;
      this.validationSuiteInfo = validationSuiteInfo;
    }
  }

  /**
   * Get list of parameters for all validators.
   * @return list of parameters for all validators.
   */
  public List<ParameterWithVersion> getParameters() {
    List<ParameterWithVersion> parameters = new ArrayList<>();

    for (ValidationSuiteInfoWithVersions<S> suite : getValidationSuites()) {
      ValidationSuiteInfo<S> info = suite.validationSuiteInfo;
      for (ValidationParameter parameter : info.parameters) {
        parameters.add(new ParameterWithVersion(
            parameter,
            suite.minMajorVersion,
            suite.maxMajorVersion
        ));
      }
    }
    return parameters;
  }

  @PostConstruct
  // Probably Spotbugs bug, there was no error in Java 11, but there is in 17
  @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
  private void registerApiName() { //NOPMD
    this.apiValidatorsManager.registerApiValidator(this.validatedApiName, this.endpoint, this);
  }

  public abstract Logger getLogger();

  protected abstract List<ValidationSuiteInfoWithVersions<S>> getValidationSuites();

  @SuppressWarnings("unchecked")
  protected List<ValidationSuiteInfoWithVersions<S>> getValidationSuitesFromValidator(
      ApiValidator<S> validator) {

    Class<?> validatorClass = validator.getClass();
    List<ValidationSuiteInfoWithVersions<S>> validationSuites = new ArrayList<>();

    for (Field field : validatorClass.getDeclaredFields()) {
      if (field.isAnnotationPresent(ValidatorTestStep.class)) {

        try {
          ValidatorTestStep annotation = field.getAnnotation(ValidatorTestStep.class);
          ValidationSuiteInfo<S> validationSuite = (ValidationSuiteInfo<S>) field.get(validator);

          validationSuites.add(new ValidationSuiteInfoWithVersions<S>(annotation.minMajorVersion(),
              annotation.maxMajorVersion(), validationSuite));
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }

    return validationSuites;
  }

  protected abstract S createState(String url, SemanticVersion version);

  /**
   * Runs all tests that are compatible with provided version.
   *
   * @param urlStr
   *     url to validate.
   * @param version
   *     version to validate.
   * @param security
   *     security method to validate.
   * @param parameters
   *     parameters passed by user.
   * @return List of steps performed and their results.
   */
  public List<ValidationStepWithStatus> runTests(String urlStr, SemanticVersion version,
      HttpSecurityDescription security, ValidationParameters parameters) {
    AbstractValidationSuite.ValidationSuiteConfig config =
        new AbstractValidationSuite.ValidationSuiteConfig(
            this.docBuilder, this.internet, this.client, this.catalogueMatcherProvider,
            this.gitHubTagsGetter
        );
    List<ValidationStepWithStatus> result = new ArrayList<>();
    S state = createState(urlStr, version);
    state.parameters = parameters;
    for (ValidationSuiteInfo<S> info : getCompatibleSuites(version, getValidationSuites())) {
      runTestsFromSuiteFactory(version, security, state, config, info.setupFactory, result);
      if (state.broken) {
        break;
      }

      runTestsFromSuiteFactory(version, security, state, config, info.testFactory, result);
      if (state.broken) {
        break;
      }
    }

    return result;
  }

  private void runTestsFromSuiteFactory(SemanticVersion version, HttpSecurityDescription security,
      S state, AbstractValidationSuite.ValidationSuiteConfig config,
      ValidationSuiteFactory<S> factory, List<ValidationStepWithStatus> result) {

    AbstractValidationSuite<S> setupSuite =
        factory.create(this, state, config, version.major);
    setupSuite.run(security);
    result.addAll(setupSuite.getResults());
  }

  protected interface ValidationSuiteFactory<T extends SuiteState> {
    AbstractValidationSuite<T> create(ApiValidator<T> validator, T state,
        AbstractValidationSuite.ValidationSuiteConfig config, int version);
  }

  public static class ValidationSuiteInfo<T extends SuiteState> {
    ValidationSuiteFactory<T> setupFactory;
    ValidationSuiteFactory<T> testFactory;
    List<ValidationParameter> parameters;

    /**
     * Stores classes necessary to create a validator test step.
     * @param setupFactory
     *     a factory that creates a setup validation suite.
     * @param parameters
     *     parameters for the suite creation.
     * @param testFactory
     *     a factory that creates a validation suite.
     */
    public ValidationSuiteInfo(ValidationSuiteFactory<T> setupFactory,
        List<ValidationParameter> parameters,
        ValidationSuiteFactory<T> testFactory) {
      this.setupFactory = setupFactory;
      this.testFactory = testFactory;
      this.parameters = parameters;
    }
  }
}
