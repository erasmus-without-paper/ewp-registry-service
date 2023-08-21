package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.get;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.IMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class IMobilitiesGetValidator extends ApiValidator<IMobilitiesSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      IMobilitiesGetValidator.class);

  public IMobilitiesGetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "imobilities",
        ApiEndpoint.Get);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<IMobilitiesSuiteState> apiTests = new ValidationSuiteInfo<>(
      IMobilitiesGetSetupValidationSuite::new,
      IMobilitiesGetSetupValidationSuite.getParameters(),
      IMobilitiesGetValidationSuite::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<IMobilitiesSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected IMobilitiesSuiteState createState(String url, SemanticVersion version) {
    return new IMobilitiesSuiteState(url, version);
  }
}
