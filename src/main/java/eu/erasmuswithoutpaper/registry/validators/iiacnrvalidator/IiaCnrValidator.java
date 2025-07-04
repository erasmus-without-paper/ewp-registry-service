package eu.erasmuswithoutpaper.registry.validators.iiacnrvalidator;

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
public class IiaCnrValidator extends ApiValidator<IiaCnrSuiteState> {
  public IiaCnrValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "iia-cnr");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<IiaCnrSuiteState> apiTests = new ValidationSuiteInfo<>(
      IiaCnrSetupValidationSuite::new,
      IiaCnrSetupValidationSuite.getParameters(),
      IiaCnrValidationSuite::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<IiaCnrSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected IiaCnrSuiteState createState(String url, SemanticVersion version) {
    return new IiaCnrSuiteState(url, version);
  }
}
