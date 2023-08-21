package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IiaGetValidator extends ApiValidator<IiaSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      IiaGetValidator.class);

  public IiaGetValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "iias", ApiEndpoint.Get);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<IiaSuiteState> universalTests = new ValidationSuiteInfo<>(
      IiaGetSetupValidationSuite::new,
      IiaGetSetupValidationSuite.getParameters(),
      IiaGetValidationSuite::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected IiaSuiteState createState(String url, SemanticVersion version) {
    return new IiaSuiteState(url, version);
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<IiaSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }
}
