package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

@Service
public class IiaIndexValidator extends ApiValidator<IiaSuiteState> {
  public IiaIndexValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "iias", ApiEndpoint.INDEX);
  }

  @ValidatorTestStep(minMajorVersion = "6", maxMajorVersion = "6")
  public ValidationSuiteInfo<IiaSuiteState> basicTestsV6 = new ValidationSuiteInfo<>(
      IiaIndexBasicSetupValidationSuiteV6::new,
      IiaIndexBasicSetupValidationSuiteV6.getParameters(),
      IiaIndexBasicValidationSuiteV6::new);

  @ValidatorTestStep(minMajorVersion = "6", maxMajorVersion = "6")
  public ValidationSuiteInfo<IiaSuiteState> complexTestsV6 = new ValidationSuiteInfo<>(
      IiaIndexComplexSetupValidationSuiteV6::new,
      IiaIndexComplexSetupValidationSuiteV6.getParameters(),
      IiaIndexComplexValidationSuiteV6::new);

  @ValidatorTestStep(minMajorVersion = "7")
  public ValidationSuiteInfo<IiaSuiteState> basicTestsV7 = new ValidationSuiteInfo<>(
          IiaIndexBasicSetupValidationSuiteV7::new,
          Collections.emptyList(),
          IiaIndexBasicValidationSuiteV7::new);

  @ValidatorTestStep(minMajorVersion = "7")
  public ValidationSuiteInfo<IiaSuiteState> complexTestsV7 = new ValidationSuiteInfo<>(
      IiaIndexComplexSetupValidationSuiteV7::new,
      IiaIndexComplexSetupValidationSuiteV7.getParameters(),
      IiaIndexComplexValidationSuiteV7::new);

  @Override
  protected IiaSuiteState createState(String url, SemanticVersion version) {
    return new IiaSuiteState(url, version);
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<IiaSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }
}