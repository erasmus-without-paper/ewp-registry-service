package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for validating external Echo API implementations.
 */
@Service
public class EchoValidator extends ApiValidator<EchoSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(EchoValidator.class);

  @Autowired
  public EchoValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "echo");
  }

  @ValidatorTestStep(maxMajorVersion = "1")
  public ValidationSuiteInfo<EchoSuiteState> apiTestsV1 = new ValidationSuiteInfo<>(
      EchoSetupValidationSuiteV1::new,
      EchoSetupValidationSuiteV1.getParameters(),
      EchoValidationSuiteCommon::new);

  @ValidatorTestStep(minMajorVersion = "2")
  public ValidationSuiteInfo<EchoSuiteState> apiTestsV2 = new ValidationSuiteInfo<>(
      EchoSetupValidationSuite::new,
      EchoSetupValidationSuite.getParameters(),
      EchoValidationSuiteCommon::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<EchoSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected EchoSuiteState createState(String url, SemanticVersion version) {
    return new EchoSuiteState(url, version);
  }
}
