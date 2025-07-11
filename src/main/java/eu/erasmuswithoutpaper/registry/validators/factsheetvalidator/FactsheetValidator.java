package eu.erasmuswithoutpaper.registry.validators.factsheetvalidator;

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
public class FactsheetValidator extends ApiValidator<FactsheetSuiteState> {
  public FactsheetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "factsheet");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<FactsheetSuiteState> apiTests = new ValidationSuiteInfo<>(
      FactsheetSetupValidationSuite::new,
      FactsheetSetupValidationSuite.getParameters(),
      FactsheetValidationSuite::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<FactsheetSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected FactsheetSuiteState createState(String url, SemanticVersion version) {
    return new FactsheetSuiteState(url, version);
  }
}
