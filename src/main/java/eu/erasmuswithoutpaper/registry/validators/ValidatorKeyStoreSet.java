package eu.erasmuswithoutpaper.registry.validators;

import java.security.Security;

import eu.erasmuswithoutpaper.registry.configuration.ConsoleEnvInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Keeps keys and certificates used by {@link ApiValidator}s.
 */
@Service
public class ValidatorKeyStoreSet {
  private static final Logger logger = LoggerFactory.getLogger(ValidatorKeyStoreSet.class);

  private ValidatorKeyStore mainKeyStore;
  private ValidatorKeyStore secondaryKeyStore;

  /**
   * Generates credential and certificates to be used by validators.
   */
  @Autowired
  public ValidatorKeyStoreSet(ConsoleEnvInfo consoleEnvInfo) {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      logger.debug("Registering BouncyCastle security provider");
      Security.addProvider(new BouncyCastleProvider());
    }

    this.mainKeyStore = new ValidatorKeyStore(!consoleEnvInfo.isConsole());
    this.secondaryKeyStore = null;
  }

  public ValidatorKeyStore getMainKeyStore() {
    return mainKeyStore;
  }

  public ValidatorKeyStore getSecondaryKeyStore() {
    return secondaryKeyStore;
  }

  public void setMainKeyStore(ValidatorKeyStore keyStore) {
    this.mainKeyStore = keyStore;
  }

  public void setSecondaryKeyStore(ValidatorKeyStore keyStore) {
    this.secondaryKeyStore = keyStore;
  }
}
