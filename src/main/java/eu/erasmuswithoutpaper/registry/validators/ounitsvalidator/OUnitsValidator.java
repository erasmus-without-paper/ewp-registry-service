package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

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
public class OUnitsValidator extends ApiValidator<OUnitsSuiteState> {

  public OUnitsValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "organizational-units");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<OUnitsSuiteState> apiTests = new ValidationSuiteInfo<>(
      OUnitsSetupValidationSuite::new,
      OUnitsSetupValidationSuite.getParameters(),
      OUnitsValidationSuite::new);

  @Override
  protected OUnitsSuiteState createState(String url, SemanticVersion version) {
    return new OUnitsSuiteState(url, version);
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<OUnitsSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }
}
