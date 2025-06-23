package eu.erasmuswithoutpaper.registry.validators.omobilitycnrvalidator;

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
public class OmobilityCnrValidator extends ApiValidator<OmobilityCnrSuiteState> {

  public OmobilityCnrValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobility-cnr");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<OmobilityCnrSuiteState> apiTests =
      new ValidationSuiteInfo<>(OmobilityCnrSetupValidationSuite::new,
          OmobilityCnrSetupValidationSuite.getParameters(), OmobilityCnrValidationSuite::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<OmobilityCnrSuiteState>> getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected OmobilityCnrSuiteState createState(String url, SemanticVersion version) {
    return new OmobilityCnrSuiteState(url, version);
  }
}
