package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.iia.IiaHashService;
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
public class IiaGetValidator extends ApiValidator<IiaSuiteState> {

  private IiaHashService iiaHashService;

  private static final Logger logger = LoggerFactory.getLogger(
      IiaGetValidator.class);

  public IiaGetValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet, IiaHashService iiaHashService) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "iias", ApiEndpoint.GET);
    this.iiaHashService = iiaHashService;
  }

  @ValidatorTestStep(maxMajorVersion = "6")
  public ValidationSuiteInfo<IiaSuiteState> testsV6 = new ValidationSuiteInfo<>(
      IiaGetSetupValidationSuiteV6::new,
      IiaGetSetupValidationSuiteV6.getParameters(),
      (validator, state, config, version) -> new IiaGetValidationSuiteV6(validator, state, config,
          version, iiaHashService));

  @ValidatorTestStep(minMajorVersion = "7")
  public ValidationSuiteInfo<IiaSuiteState> testsV7 = new ValidationSuiteInfo<>(
      IiaGetSetupValidationSuiteV7::new, IiaGetSetupValidationSuiteV7.getParameters(),
      (validator, state, config, version) -> new IiaGetValidationSuiteV7(validator, state, config,
          version, iiaHashService));

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
