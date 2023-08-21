package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.get;

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
public class OMobilityLAsGetValidator extends ApiValidator<OMobilityLAsSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      OMobilityLAsGetValidator.class);


  public OMobilityLAsGetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobility-las",
        ApiEndpoint.Get);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<OMobilityLAsSuiteState> apiTests = new ValidationSuiteInfo<>(
      OMobilityLAsGetSetupValidationSuite::new,
      OMobilityLAsGetSetupValidationSuite.getParameters(),
      OMobilityLAsGetValidationSuite::new);

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
