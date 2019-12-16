package eu.erasmuswithoutpaper.registry.validators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.githubtags.GitHubTagsGetter;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
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

  protected static <K extends Comparable<? super K>, V> ListMultimap<K, V> createMultimap() {
    return MultimapBuilder.treeKeys().linkedListValues().build();
  }

  private Collection<ValidationSuiteInfo<S>> getCompatibleSuites(
      SemanticVersion version,
      ListMultimap<SemanticVersion, ValidationSuiteInfo<S>> map) {
    List<ValidationSuiteInfo<S>> result = new ArrayList<>();
    for (Map.Entry<SemanticVersion, ValidationSuiteInfo<S>> entry : map.entries()) {
      if (version.isCompatible(entry.getKey())) {
        result.add(entry.getValue());
      }
    }
    return result;
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
    public SemanticVersion version;

    public ParameterWithVersion(
        ValidationParameter parameter,
        SemanticVersion version) {
      this.parameter = parameter;
      this.version = version;
    }
  }

  /**
   * Get list of parameters for all validators.
   * @return list of parameters for all validators.
   */
  public List<ParameterWithVersion> getParameters() {
    List<ParameterWithVersion> parameters = new ArrayList<>();

    for (Map.Entry<SemanticVersion, ValidationSuiteInfo<S>> entry :
        getValidationSuites().entries()) {
      SemanticVersion version = entry.getKey();
      ValidationSuiteInfo<S> info = entry.getValue();
      for (ValidationParameter parameter : info.parameters) {
        parameters.add(new ParameterWithVersion(
            parameter,
            version
        ));
      }
    }
    return parameters;
  }

  @PostConstruct
  private void registerApiName() { //NOPMD
    this.apiValidatorsManager.registerApiValidator(this.validatedApiName, this.endpoint, this);
  }

  public abstract Logger getLogger();

  protected abstract ListMultimap<SemanticVersion, ValidationSuiteInfo<S>> getValidationSuites();

  public Collection<SemanticVersion> getCoveredApiVersions() {
    return getValidationSuites().keySet();
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
      AbstractValidationSuite<S> suite =
          info.factory.create(this, state, config);
      suite.run(security);
      result.addAll(suite.getResults());
      if (state.broken) {
        break;
      }
    }
    return result;
  }

  protected interface ValidationSuiteFactory<T extends SuiteState> {
    AbstractValidationSuite<T> create(ApiValidator<T> validator, T state,
        AbstractValidationSuite.ValidationSuiteConfig config);
  }

  public static class ValidationSuiteInfo<T extends SuiteState> {
    ValidationSuiteFactory<T> factory;
    List<ValidationParameter> parameters;

    public ValidationSuiteInfo(ValidationSuiteFactory<T> factory,
        List<ValidationParameter> parameters) {
      this.factory = factory;
      this.parameters = parameters;
    }

    public ValidationSuiteInfo(ValidationSuiteFactory<T> factory) {
      this.factory = factory;
      this.parameters = new ArrayList<>();
    }
  }
}
