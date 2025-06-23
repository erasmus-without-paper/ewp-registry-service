package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.index;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

@Service
public class OMobilitiesIndexValidator extends ApiValidator<OMobilitiesSuiteState> {

  public OMobilitiesIndexValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobilities",
        ApiEndpoint.INDEX);
  }

  @ValidatorTestStep(minMajorVersion = "2", maxMajorVersion = "2")
  public ValidationSuiteInfo<OMobilitiesSuiteState> basicApiTestsV2 = new ValidationSuiteInfo<>(
      OMobilitiesIndexSetupValidationSuiteV2::new,
      OMobilitiesIndexSetupValidationSuiteV2.getParameters(),
      OMobilitiesIndexValidationSuiteV2::new);

  @ValidatorTestStep(minMajorVersion = "2", maxMajorVersion = "2")
  public ValidationSuiteInfo<OMobilitiesSuiteState> complexApiTestsV2 = new ValidationSuiteInfo<>(
      OMobilitiesIndexComplexSetupValidationSuiteV2::new,
      OMobilitiesIndexComplexSetupValidationSuiteV2.getParameters(),
      OMobilitiesIndexComplexValidationSuiteV2::new);

  @ValidatorTestStep(minMajorVersion = "3")
  public ValidationSuiteInfo<OMobilitiesSuiteState> basicApiTestsV3 = new ValidationSuiteInfo<>(
      OMobilitiesIndexSetupValidationSuiteV3::new,
      OMobilitiesIndexSetupValidationSuiteV3.getParameters(),
      OMobilitiesIndexValidationSuiteV3::new);

  @ValidatorTestStep(minMajorVersion = "3")
  public ValidationSuiteInfo<OMobilitiesSuiteState> complexApiTestsV3 = new ValidationSuiteInfo<>(
      OMobilitiesIndexComplexSetupValidationSuiteV3::new,
      OMobilitiesIndexComplexSetupValidationSuiteV3.getParameters(),
      OMobilitiesIndexComplexValidationSuiteV3::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<OMobilitiesSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected OMobilitiesSuiteState createState(String url, SemanticVersion version) {
    return new OMobilitiesSuiteState(url, version);
  }
}
