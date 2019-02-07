package eu.erasmuswithoutpaper.registry.validators;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.echovalidator.EchoValidator;
import eu.erasmuswithoutpaper.registry.web.SelfManifestProvider;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.beans.factory.annotation.Autowired;

import org.slf4j.Logger;


/**
 * Base class for services validating external APIs' implementations.
 */
public abstract class ApiValidator {
  protected final RegistryClient client;
  protected final EwpDocBuilder docBuilder;
  protected final Internet internet;
  private final String validatedApiName;
  private final ValidatorKeyStore validatorKeyStore;
  @Autowired
  protected ApiValidatorsManager apiValidatorsManager;
  @Autowired
  private ManifestRepository repo;

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

  private static Collection<ValidationSuiteFactory> getCompatibleSuites(SemanticVersion version,
      TreeMap<SemanticVersion, ValidationSuiteFactory> map) {
    List<ValidationSuiteFactory> result = new ArrayList<>();
    for (Map.Entry<SemanticVersion, ValidationSuiteFactory> entry : map.entrySet()) {
      if (version.isCompatible(entry.getKey())) {
        result.add(entry.getValue());
      }
    }
    return result;
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
   * @return Certificate
   */
  public X509Certificate generateCertificate(KeyPair keyPair) {
    return this.validatorKeyStore.generateCertificate(keyPair);
  }

  /**
   * Generates RSA KeyPair.
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

  protected abstract TreeMap<SemanticVersion, ValidationSuiteFactory> getValidationSuitesMap();

  public Collection<SemanticVersion> getCoveredApiVersions() {
    return getValidationSuitesMap().keySet();
  }

  /**
   * Runs all tests that are compatible with provided version.
   *
   * @param urlStr
   *     url to validate.
   * @param version
   *     version to validate.
   * @param security
   *     security method to validate.
   * @return List of steps performed and their results.
   */
  public List<ValidationStepWithStatus> runTests(String urlStr, SemanticVersion version,
      HttpSecurityDescription security) {
    List<ValidationStepWithStatus> result = new ArrayList<>();
    for (ValidationSuiteFactory sf : getCompatibleSuites(version, getValidationSuitesMap())) {
      AbstractValidationSuite suite =
          sf.create(this, this.docBuilder, this.internet, urlStr, this.client, repo);
      suite.run(security);
      result.addAll(suite.getResults());
      if (suite.isSuiteBroken()) {
        break;
      }
    }
    return result;
  }

  protected interface ValidationSuiteFactory {
    AbstractValidationSuite create(ApiValidator validator, EwpDocBuilder docBuilder,
        Internet internet, String urlStr, RegistryClient regClient, ManifestRepository repo);
  }
}