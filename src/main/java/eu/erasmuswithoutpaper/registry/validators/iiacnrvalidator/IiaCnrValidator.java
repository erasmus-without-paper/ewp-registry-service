package eu.erasmuswithoutpaper.registry.validators.iiacnrvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IiaCnrValidator extends ApiValidator<SuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      IiaCnrValidator.class);

  public IiaCnrValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "iia-cnr");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<SuiteState> apiTests = new ValidationSuiteInfo<>(
      IiaCnrSetupValidationSuite::new,
      IiaCnrSetupValidationSuite.getParameters(),
      IiaCnrValidationSuite::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<SuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected SuiteState createState(String url, SemanticVersion version) {
    return new SuiteState(url, version);
  }
}
