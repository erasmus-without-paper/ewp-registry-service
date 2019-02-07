package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.util.TreeMap;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for validating external Echo API implementations.
 */
@Service
public class EchoValidator extends ApiValidator {
  private static final Logger logger = LoggerFactory.getLogger(EchoValidator.class);
  private static TreeMap<SemanticVersion, ValidationSuiteFactory> validationSuites =
      new TreeMap<>();

  static {
    validationSuites.put(new SemanticVersion(2, 0, 0), EchoValidationSuiteV200::new);
    validationSuites.put(new SemanticVersion(1, 0, 0), EchoValidationSuiteV100::new);
  }

  @Autowired
  public EchoValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStore validatorKeyStore) {
    super(docBuilder, internet, client, validatorKeyStore, "echo");
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected TreeMap<SemanticVersion, ValidationSuiteFactory> getValidationSuitesMap() {
    return validationSuites;
  }
}
