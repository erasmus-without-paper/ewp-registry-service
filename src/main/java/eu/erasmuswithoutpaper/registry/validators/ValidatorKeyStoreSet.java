package eu.erasmuswithoutpaper.registry.validators;

import java.security.Security;
import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.configuration.ConsoleEnvInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
   * @param rootUrl
   *      URL where this instance is hosted, used to check, if we are in a production environment.
   * @param consoleEnvInfo
   *      Provides info whether this application is run as a standalone console validator.
   * @param additionalHeiIdsArray
   *      List of hei ids that should be added to validators covered-institutions list.
   */
  @Autowired
  public ValidatorKeyStoreSet(ConsoleEnvInfo consoleEnvInfo,
      @Value("${app.root-url}") String rootUrl,
      @Value("${app.local-registry.additional-hei-ids:#{null}}") List<String> additionalHeiIdsArray
  ) {
    if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
      logger.debug("Registering BouncyCastle security provider");
      Security.addProvider(new BouncyCastleProvider());
    }

    if (additionalHeiIdsArray == null) {
      additionalHeiIdsArray = new ArrayList<>();
    }

    ArrayList<String> additionalHeiIds = new ArrayList<>(additionalHeiIdsArray);

    // We are providing URL ourselves because Application.getRootUrl() will return null,
    // because initialization is not finished yet.
    if (Application.isProductionSite(rootUrl)) {
      // No hei ids should be added in production environment, even if explicitly specified.
      additionalHeiIds.clear();
    } else if (!consoleEnvInfo.isConsole()) {
      // In development environment we add artificial hei ids for validator.
      for (int i = 1; i <= 2; i++) {
        additionalHeiIds.add("validator-hei0" + i + ".developers.erasmuswithoutpaper.eu");
      }
    }

    this.mainKeyStore = new ValidatorKeyStore(additionalHeiIds);
    this.secondaryKeyStore = new ValidatorKeyStore();
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
