package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.get;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class OMobilitiesGetValidator extends ApiValidator<OMobilitiesSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(OMobilitiesGetValidator.class);

  public OMobilitiesGetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobilities",
        ApiEndpoint.GET);
  }

  @ValidatorTestStep(minMajorVersion = "2", maxMajorVersion = "2")
  public ValidationSuiteInfo<OMobilitiesSuiteState> apiTestsV2 = new ValidationSuiteInfo<>(
      OMobilitiesGetSetupValidationSuiteV2::new,
      OMobilitiesGetSetupValidationSuiteV2.getParameters(),
      OMobilitiesGetValidationSuiteV2::new);

  @ValidatorTestStep(minMajorVersion = "3")
  public ValidationSuiteInfo<OMobilitiesSuiteState> apiTestsV3 = new ValidationSuiteInfo<>(
      OMobilitiesGetSetupValidationSuiteV3::new,
      OMobilitiesGetSetupValidationSuiteV3.getParameters(),
      OMobilitiesGetValidationSuiteV3::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

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
