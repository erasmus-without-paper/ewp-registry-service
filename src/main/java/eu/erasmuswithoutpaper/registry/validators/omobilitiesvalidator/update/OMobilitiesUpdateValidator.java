package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.update;

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
public class OMobilitiesUpdateValidator extends ApiValidator<OMobilitiesSuiteState> {

  public OMobilitiesUpdateValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobilities", ApiEndpoint.UPDATE);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<OMobilitiesSuiteState> apiTests = new ValidationSuiteInfo<>(
      OMobilitiesUpdateSetupValidationSuite::new,
      OMobilitiesUpdateSetupValidationSuite.getParameters(), OMobilitiesUpdateValidationSuite::new);

  @Override
  protected OMobilitiesSuiteState createState(String url, SemanticVersion version) {
    return new OMobilitiesSuiteState(url, version);
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<OMobilitiesSuiteState>> getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }
}
