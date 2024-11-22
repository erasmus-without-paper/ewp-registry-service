package eu.erasmuswithoutpaper.registry.validators.omobilitylascnrvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OMobilityLaCnrValidator extends ApiValidator<OMobilityLaCnrSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      OMobilityLaCnrValidator.class);

  public OMobilityLaCnrValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobility-la-cnr");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<OMobilityLaCnrSuiteState> apiTests = new ValidationSuiteInfo<>(
      OMobilityLaCnrSetupValidationSuite::new,
      OMobilityLaCnrSetupValidationSuite.getParameters(),
      OMobilityLaCnrValidationSuite::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<OMobilityLaCnrSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected OMobilityLaCnrSuiteState createState(String url, SemanticVersion version) {
    return new OMobilityLaCnrSuiteState(url, version);
  }
}
