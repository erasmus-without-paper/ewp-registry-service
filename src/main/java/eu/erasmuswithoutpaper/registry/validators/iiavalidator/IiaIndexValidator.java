package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IiaIndexValidator extends ApiValidator<IiaSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      IiaIndexValidator.class);

  public IiaIndexValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "iias", ApiEndpoint.INDEX);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<IiaSuiteState> basicTests = new ValidationSuiteInfo<>(
      IiaIndexBasicSetupValidationSuite::new,
      IiaIndexBasicSetupValidationSuite.getParameters(),
      IiaIndexBasicValidationSuite::new);

  @ValidatorTestStep(maxMajorVersion = "2")
  public ValidationSuiteInfo<IiaSuiteState> complexTestsV2 = new ValidationSuiteInfo<>(
      IiaIndexComplexSetupValidationSuiteV2::new,
      IiaIndexComplexSetupValidationSuiteV2.getParameters(),
      IiaIndexComplexValidationSuite::new);

  @ValidatorTestStep(minMajorVersion = "3", maxMajorVersion = "3")
  public ValidationSuiteInfo<IiaSuiteState> complexTestsV3 = new ValidationSuiteInfo<>(
      IiaIndexComplexSetupValidationSuiteV3::new,
      IiaIndexComplexSetupValidationSuiteV3.getParameters(),
      IiaIndexComplexValidationSuite::new);

  @ValidatorTestStep(minMajorVersion = "4", maxMajorVersion = "4")
  public ValidationSuiteInfo<IiaSuiteState> complexTestsV4 = new ValidationSuiteInfo<>(
      IiaIndexComplexSetupValidationSuiteV4::new,
      IiaIndexComplexSetupValidationSuiteV4.getParameters(),
      IiaIndexComplexValidationSuite::new);

  @ValidatorTestStep(minMajorVersion = "6")
  public ValidationSuiteInfo<IiaSuiteState> complexTestsV6 = new ValidationSuiteInfo<>(
      IiaIndexComplexSetupValidationSuiteV6::new,
      IiaIndexComplexSetupValidationSuiteV6.getParameters(),
      IiaIndexComplexValidationSuite::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

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