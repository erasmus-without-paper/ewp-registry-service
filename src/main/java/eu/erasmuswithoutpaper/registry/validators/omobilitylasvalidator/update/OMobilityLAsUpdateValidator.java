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

@Service
public class OMobilityLAsUpdateValidator extends ApiValidator<OMobilityLAsSuiteState> {

  public OMobilityLAsUpdateValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobility-las",
        ApiEndpoint.UPDATE);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<OMobilityLAsSuiteState> apiTests = new ValidationSuiteInfo<>(
      OMobilityLAsUpdateSetupValidationSuite::new,
      OMobilityLAsUpdateSetupValidationSuite.getParameters(),
      OMobilityLAsUpdateValidationSuite::new);

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
