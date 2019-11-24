package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for validating external Echo API implementations.
 */
@Service
public class EchoValidator extends ApiValidator<EchoSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(EchoValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<EchoSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();

    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(EchoSetupValidationSuiteV1::new)
    );
    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(EchoValidationSuiteV1::new)
    );

    validationSuites.put(
        new SemanticVersion(2, 0, 0),
        new ValidationSuiteInfo<>(EchoSetupValidationSuiteV2::new)
    );
    validationSuites.put(
        new SemanticVersion(2, 0, 0),
        new ValidationSuiteInfo<>(EchoValidationSuiteV2::new)
    );
  }

  @Autowired
  public EchoValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "echo");
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<EchoSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected EchoSuiteState createState(String url, SemanticVersion version) {
    return new EchoSuiteState(url, version);
  }
}
