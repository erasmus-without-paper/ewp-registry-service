package eu.erasmuswithoutpaper.registry.validators.mtdictionariesvalidator;

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
public class MtDictionariesValidator extends ApiValidator<MtDictionariesSuiteState> {
  public MtDictionariesValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "mt-dictionaries");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<MtDictionariesSuiteState> apiTests = new ValidationSuiteInfo<>(
      MtDictionariesSetupValidationSuite::new,
      MtDictionariesSetupValidationSuite.getParameters(),
      MtDictionariesValidationSuite::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<MtDictionariesSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected MtDictionariesSuiteState createState(String url, SemanticVersion version) {
    return new MtDictionariesSuiteState(url, version);
  }
}
