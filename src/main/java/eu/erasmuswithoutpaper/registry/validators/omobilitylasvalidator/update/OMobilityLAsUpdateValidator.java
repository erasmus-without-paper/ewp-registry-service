package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.update;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class OMobilityLAsUpdateValidator extends ApiValidator<OMobilityLAsSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      OMobilityLAsUpdateValidator.class);

  public OMobilityLAsUpdateValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobility-las",
        ApiEndpoint.Update);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<OMobilityLAsSuiteState> apiTests = new ValidationSuiteInfo<>(
      OMobilityLAsUpdateSetupValidationSuite::new,
      OMobilityLAsUpdateSetupValidationSuite.getParameters(),
      OMobilityLAsUpdateValidationSuite::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected OMobilityLAsSuiteState createState(String url, SemanticVersion version) {
    return new OMobilityLAsSuiteState(url, version);
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<OMobilityLAsSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }
}
