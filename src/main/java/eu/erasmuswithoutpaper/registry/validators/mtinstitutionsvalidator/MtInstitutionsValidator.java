package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

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
public class MtInstitutionsValidator extends ApiValidator<MtInstitutionsSuiteState> {

  public MtInstitutionsValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "mt-institutions");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<MtInstitutionsSuiteState> apiTests = new ValidationSuiteInfo<>(
      MtInstitutionsSetupValidationSuite::new,
      MtInstitutionsSetupValidationSuite.getParameters(),
      MtInstitutionsValidationSuite::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<MtInstitutionsSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected MtInstitutionsSuiteState createState(String url, SemanticVersion version) {
    return new MtInstitutionsSuiteState(url, version);
  }
}
