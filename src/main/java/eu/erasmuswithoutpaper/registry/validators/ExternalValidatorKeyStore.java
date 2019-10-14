package eu.erasmuswithoutpaper.registry.validators;

import java.security.KeyPair;
import java.security.Security;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@ConditionalOnNotWebApplication
public class ExternalValidatorKeyStore extends ValidatorKeyStore {
  private static final Logger logger = LoggerFactory.getLogger(ExternalValidatorKeyStore.class);

  @Autowired
  private RegistryClient registryClient;

  /**
   * Constructor.
   */
  public ExternalValidatorKeyStore() {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      logger.debug("Registering BouncyCastle security provider");
      Security.addProvider(new BouncyCastleProvider());
    }

    this.myCredentialsDate = new Date();
    this.myClientRsaKeyPair = this.generateKeyPair();
    this.myServerRsaKeyPair = this.generateKeyPair();
    this.myTlsKeyPair = this.generateKeyPair();
    this.myTlsCertificate = this.generateCertificate(this.myTlsKeyPair);
    this.myUnregisteredKeyPair = this.generateKeyPair();

  }

  /**
   * Sets Client RSA Key and fills covered hei ids.
   */
  public void setClientRsaKey(KeyPair keyPair) {
    this.myClientRsaKeyPair = keyPair;

    if (this.myClientRsaKeyPair == null) {
      return;
    }

    // Add artificial HEIs that are used by validators.
    // Collection<String> heisCoveredByCertificate = registryClient
    // .getHeisCoveredByCertificate(this.myTlsCertificate);

    Collection<String> heisCoveredByClientKey = registryClient
        .getHeisCoveredByClientKey((RSAPublicKey) this.myClientRsaKeyPair.getPublic());

    ArrayList<String> coveredHeiIDs = new ArrayList<>();
    // TODO should those lists be equal?
    // coveredHeiIDs.addAll(heisCoveredByCertificate);
    coveredHeiIDs.addAll(heisCoveredByClientKey);
    this.myCoveredHeiIDs = coveredHeiIDs.stream().distinct().collect(Collectors.toList());
  }

  public void setServerRsaKey(KeyPair keyPair) {
    this.myServerRsaKeyPair = keyPair;
  }
}
