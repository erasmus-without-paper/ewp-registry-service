package eu.erasmuswithoutpaper.registry.validators;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.echovalidator.EchoValidator;
import eu.erasmuswithoutpaper.registry.validators.githubtags.GitHubTagsGetter;
import eu.erasmuswithoutpaper.registry.web.SelfManifestProvider;
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
  private final ValidatorKeyStore validatorKeyStore;
  @Autowired
  protected ApiValidatorsManager apiValidatorsManager;
  @Autowired
  private ManifestRepository repo;
  @Autowired
  private GitHubTagsGetter gitHubTagsGetter;

  /**
   * @param docBuilder
   *     Needed for validating Echo API responses against the schemas.
   * @param internet
   *     Needed to make Echo API requests across the network.
   * @param client
   *     Needed to fetch (and verify) Echo APIs' security settings.
   * @param validatorKeyStore
   *     Store providing keys, certificates and covered HEI IDs.
   * @param validatedApiName
   *     lowercase name of API validated by this class.
   */
  public ApiValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStore validatorKeyStore, String validatedApiName) {
    this.docBuilder = docBuilder;
    this.internet = internet;
    this.client = client;

    this.validatorKeyStore = validatorKeyStore;
    this.validatedApiName = validatedApiName;
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
   * @param version
   *    version of the API for which parameters will be returned.
   */
  public List<ValidationParameter> getParameters(SemanticVersion version) {
    List<ValidationParameter> parameters = new ArrayList<>();
    for (ValidationSuiteInfo<S> info : getCompatibleSuites(version, getValidationSuites())) {
      parameters.addAll(info.parameters);
    }
    return parameters;
  }

  @PostConstruct
  private void registerApiName() { //NOPMD
    this.apiValidatorsManager.registerApiValidator(this.validatedApiName, this);
  }

  /**
   * Similar to {@link #getTlsClientCertificateInUse()}, but for HTTPSIG keys.
   *
   * @return A {@link KeyPair} to be published in the Registry.
   */
  public RSAPublicKey getClientRsaPublicKeyInUse() {
    return this.validatorKeyStore.getClientRsaPublicKeyInUse();
  }

  /**
   * The TLS client certificate published for the {@link EchoValidator} needs to cover a specific
   * set of virtual HEIs (so that the tester can expect Echo APIs to think that the request comes
   * from these HEIs). This method allows other services (in particular, the {@link
   * SelfManifestProvider}) to fetch these HEIs from us.
   *
   * @return IDs of the HEIs which are to be associated with the TLS client certificate returned in
   *     {@link #getTlsClientCertificateInUse()}.
   */
  public List<String> getCoveredHeiIDs() {
    return this.validatorKeyStore.getCoveredHeiIDs();
  }

  /**
   * Since it takes some time to propagate information about new client credentials, this date may
   * be useful. If it's quite fresh, then it's an indicator that the clients might not yet be
   * recognized by all the EWP Hosts.
   *
   * @return The date indicating since when the current credentials are used.
   */
  public Date getCredentialsGenerationDate() {
    return this.validatorKeyStore.getCredentialsGenerationDate();
  }

  /**
   * @return A {@link KeyPair} to be published in the Registry.
   */
  public RSAPublicKey getServerRsaPublicKeyInUse() {
    return this.validatorKeyStore.getServerRsaPublicKeyInUse();
  }

  /**
   * The {@link EchoValidator} instance needs to publish its TLS Client Certificate in the Registry
   * in order to be able to test TLS Client Certificate Authentication. This method allows other
   * services (in particular, the {@link SelfManifestProvider}) to fetch this information from us.
   *
   * @return An {@link X509Certificate} to be published in the Registry.
   */
  public X509Certificate getTlsClientCertificateInUse() {
    return this.validatorKeyStore.getTlsClientCertificateInUse();
  }

  /**
   * Generate a certificate for given KeyPair.
   *
   * @param keyPair
   *     keyPair to use.
   * @return Certificate
   */
  public X509Certificate generateCertificate(KeyPair keyPair) {
    return this.validatorKeyStore.generateCertificate(keyPair);
  }

  /**
   * Generates RSA KeyPair.
   *
   * @return generated KeyPair.
   */
  public KeyPair generateKeyPair() {
    return validatorKeyStore.generateKeyPair();
  }

  public KeyPair getClientRsaKeyPairInUse() {
    return this.validatorKeyStore.getClientRsaKeyPairInUse();
  }

  public KeyPair getServerRsaKeyPairInUse() {
    return this.validatorKeyStore.getServerRsaKeyPairInUse();
  }

  public KeyPair getTlsKeyPairInUse() {
    return this.validatorKeyStore.getTlsKeyPairInUse();
  }

  public KeyPair getUnregisteredKeyPair() {
    return this.validatorKeyStore.getUnregisteredKeyPair();
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
            this.docBuilder, this.internet, this.client, this.repo,
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
