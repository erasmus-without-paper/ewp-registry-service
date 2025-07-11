package eu.erasmuswithoutpaper.registry.validators.mtprojectsvalidator;

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
public class MtProjectsValidator extends ApiValidator<MtProjectsSuiteState> {

  public MtProjectsValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "mt-projects");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<MtProjectsSuiteState> apiTests = new ValidationSuiteInfo<>(
      MtProjectsSetupValidationSuite::new,
      MtProjectsSetupValidationSuite.getParameters(),
      MtProjectsValidationSuite::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<MtProjectsSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected MtProjectsSuiteState createState(String url, SemanticVersion version) {
    return new MtProjectsSuiteState(url, version);
  }
}
