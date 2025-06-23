package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.get;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.IMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

@Service
public class IMobilitiesGetValidator extends ApiValidator<IMobilitiesSuiteState> {
  public IMobilitiesGetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "imobilities",
        ApiEndpoint.GET);
  }

  @ValidatorTestStep(minMajorVersion = "1", maxMajorVersion = "1")
  public ValidationSuiteInfo<IMobilitiesSuiteState> apiTestsV1 = new ValidationSuiteInfo<>(
      IMobilitiesGetSetupValidationSuiteV1::new,
      IMobilitiesGetSetupValidationSuiteV1.getParameters(),
      IMobilitiesGetValidationSuiteV1::new);

  @ValidatorTestStep(minMajorVersion = "2", maxMajorVersion = "2")
  public ValidationSuiteInfo<IMobilitiesSuiteState> apiTestsV2 = new ValidationSuiteInfo<>(
      IMobilitiesGetSetupValidationSuiteV2::new,
      IMobilitiesGetSetupValidationSuiteV2.getParameters(),
      IMobilitiesGetValidationSuiteV2::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<IMobilitiesSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected IMobilitiesSuiteState createState(String url, SemanticVersion version) {
    return new IMobilitiesSuiteState(url, version);
  }
}
