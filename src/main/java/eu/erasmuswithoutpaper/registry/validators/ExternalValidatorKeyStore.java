package eu.erasmuswithoutpaper.registry.validators;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.configuration.ConsoleEnvInfo;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnNotWebApplication
public class ExternalValidatorKeyStore extends ValidatorKeyStore {
  @Autowired
  private RegistryClient registryClient;

  /**
   * Constructor.
   */
  @Autowired
  public ExternalValidatorKeyStore(ConsoleEnvInfo consoleEnvInfo) {
    super(consoleEnvInfo);
    this.myCredentialsDate = null;
    this.myClientRsaKeyPair = this.generateKeyPair();
    this.myServerRsaKeyPair = this.generateKeyPair();
    this.myTlsKeyPair = this.generateKeyPair();
    this.myTlsCertificate = this.generateCertificate(this.myTlsKeyPair);
    this.myUnregisteredKeyPair = this.generateKeyPair();
  }

  /**
   * Sets certificate.
   */
  public void setCertificate(KeyPair keyPair, X509Certificate certificate) {
    this.myTlsKeyPair = keyPair;
    this.myTlsCertificate = certificate;

    if (this.myTlsCertificate == null || this.myTlsKeyPair == null) {
      return;
    }

    // Add artificial HEIs that are used by validators.
    Collection<String> heisCoveredByCertificate = registryClient
        .getHeisCoveredByCertificate(this.myTlsCertificate);
    ArrayList<String> coveredHeiIDs = new ArrayList<>(this.myCoveredHeiIDs);
    coveredHeiIDs.addAll(heisCoveredByCertificate);
    this.myCoveredHeiIDs = coveredHeiIDs.stream().distinct().collect(Collectors.toList());
  }

  /**
   * Sets Client RSA Key and fills covered hei ids.
   */
  public void setClientRsaKey(KeyPair keyPair) {
    this.myClientRsaKeyPair = keyPair;

    if (this.myClientRsaKeyPair == null) {
      return;
    }

    Collection<String> heisCoveredByClientKey = registryClient
        .getHeisCoveredByClientKey((RSAPublicKey) this.myClientRsaKeyPair.getPublic());
    ArrayList<String> coveredHeiIDs = new ArrayList<>(this.myCoveredHeiIDs);
    coveredHeiIDs.addAll(heisCoveredByClientKey);
    this.myCoveredHeiIDs = coveredHeiIDs.stream().distinct().collect(Collectors.toList());
  }

  public void setServerRsaKey(KeyPair keyPair) {
    this.myServerRsaKeyPair = keyPair;
  }
}
