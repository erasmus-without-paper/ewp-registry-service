package eu.erasmuswithoutpaper.registry.validators;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.echovalidator.EchoValidator;
import eu.erasmuswithoutpaper.registry.web.SelfManifestProvider;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;

/**
 * Base class for services validating external APIs' implementations.
 */
@Service
public abstract class ApiValidator {
  @Autowired
  protected ManifestRepository repo;
  private final ValidatorKeyStore validatorKeyStore;
  protected final EwpDocBuilder docBuilder;
  protected final Internet internet;

  protected final RegistryClient client;

  /**
   * @param docBuilder Needed for validating Echo API responses against the schemas.
   * @param internet Needed to make Echo API requests across the network.
   * @param client Needed to fetch (and verify) Echo APIs' security settings.
   * @param validatorKeyStore Store providing keys, certificates and covered HEI IDs.
   */
  public ApiValidator(
      EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStore validatorKeyStore) {
    this.docBuilder = docBuilder;
    this.internet = internet;
    this.client = client;

    this.validatorKeyStore = validatorKeyStore;
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
   * from these HEIs). This method allows other services (in particular, the
   * {@link SelfManifestProvider}) to fetch these HEIs from us.
   *
   * @return IDs of the HEIs which are to be associated with the TLS client certificate returned in
   *         {@link #getTlsClientCertificateInUse()}.
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
   * Run a suite of tests on the given Echo API URL.
   *
   * @param urlStr HTTPS URL pointing to the Echo API to be tested.
   * @return A list of test results.
   */
  public abstract List<ValidationStepWithStatus> runTests(String urlStr);

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
}
