package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.get;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsSuiteState;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

@Service
public class IMobilityTorsGetValidator extends ApiValidator<IMobilityTorsSuiteState> {

  public IMobilityTorsGetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "imobility-tors",
        ApiEndpoint.GET);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<IMobilityTorsSuiteState> apiTests = new ValidationSuiteInfo<>(
      IMobilityTorsGetSetupValidationSuite::new,
      IMobilityTorsGetSetupValidationSuite.getParameters(),
      IMobilityTorsGetValidationSuite::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<IMobilityTorsSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected IMobilityTorsSuiteState createState(String url, SemanticVersion version) {
    return new IMobilityTorsSuiteState(url, version);
  }
}
