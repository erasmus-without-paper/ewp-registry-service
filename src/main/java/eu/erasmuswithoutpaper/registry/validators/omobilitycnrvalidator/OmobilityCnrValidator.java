package eu.erasmuswithoutpaper.registry.validators.omobilitycnrvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OmobilityCnrValidator extends ApiValidator<OmobilityCnrSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(OmobilityCnrValidator.class);

  public OmobilityCnrValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobility-cnr");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<OmobilityCnrSuiteState> apiTests =
      new ValidationSuiteInfo<>(OmobilityCnrSetupValidationSuite::new,
          OmobilityCnrSetupValidationSuite.getParameters(), OmobilityCnrValidationSuite::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<OmobilityCnrSuiteState>> getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected OmobilityCnrSuiteState createState(String url, SemanticVersion version) {
    return new OmobilityCnrSuiteState(url, version);
  }
}
