package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

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
public class InstitutionsValidator extends ApiValidator<InstitutionsSuiteState> {

  public InstitutionsValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "institutions");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<InstitutionsSuiteState> universalTests = new ValidationSuiteInfo<>(
      InstitutionsSetupValidationSuite::new,
      InstitutionsSetupValidationSuite.getParameters(),
      InstitutionsValidationSuite::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<InstitutionsSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected InstitutionsSuiteState createState(String url, SemanticVersion version) {
    return new InstitutionsSuiteState(url, version);
  }
}
