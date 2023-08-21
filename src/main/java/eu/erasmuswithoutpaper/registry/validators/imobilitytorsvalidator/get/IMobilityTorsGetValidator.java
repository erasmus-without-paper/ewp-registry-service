package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.get;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsSuiteState;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class IMobilityTorsGetValidator extends ApiValidator<IMobilityTorsSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      IMobilityTorsGetValidator.class);

  public IMobilityTorsGetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "imobility-tors",
        ApiEndpoint.Get);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<IMobilityTorsSuiteState> apiTests = new ValidationSuiteInfo<>(
      IMobilityTorsGetSetupValidationSuite::new,
      IMobilityTorsGetSetupValidationSuite.getParameters(),
      IMobilityTorsGetValidationSuite::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<IMobilityTorsSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected IMobilityTorsSuiteState createState(String url, SemanticVersion version) {
    return new IMobilityTorsSuiteState(url, version);
  }
}
