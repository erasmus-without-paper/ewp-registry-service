package eu.erasmuswithoutpaper.registry.validators.imobilitycnrvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

@Service
public class ImobilityCnrValidator extends ApiValidator<ImobilityCnrSuiteState> {
  public ImobilityCnrValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "imobility-cnr");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<ImobilityCnrSuiteState> apiTests =
      new ValidationSuiteInfo<>(ImobilityCnrSetupValidationSuite::new,
          ImobilityCnrSetupValidationSuite.getParameters(), ImobilityCnrValidationSuite::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<ImobilityCnrSuiteState>> getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected ImobilityCnrSuiteState createState(String url, SemanticVersion version) {
    return new ImobilityCnrSuiteState(url, version);
  }
}
