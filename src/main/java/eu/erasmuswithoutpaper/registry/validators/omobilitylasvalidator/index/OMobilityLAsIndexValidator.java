package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.index;

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

@Service
public class OMobilityLAsIndexValidator extends ApiValidator<OMobilityLAsSuiteState> {

  public OMobilityLAsIndexValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobility-las",
        ApiEndpoint.INDEX);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<OMobilityLAsSuiteState> basicTests = new ValidationSuiteInfo<>(
      OMobilityLAsIndexSetupValidationSuite::new,
      OMobilityLAsIndexSetupValidationSuite.getParameters(),
      OMobilityLAsIndexValidationSuite::new);

  @ValidatorTestStep
  public ValidationSuiteInfo<OMobilityLAsSuiteState> complexTests = new ValidationSuiteInfo<>(
      OMobilityLAsIndexComplexSetupValidationSuite::new,
      OMobilityLAsIndexComplexSetupValidationSuite.getParameters(),
      OMobilityLAsIndexComplexValidationSuite::new);

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
