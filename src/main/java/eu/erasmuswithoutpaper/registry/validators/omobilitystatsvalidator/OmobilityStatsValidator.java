package eu.erasmuswithoutpaper.registry.validators.omobilitystatsvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

@Service
public class OmobilityStatsValidator extends ApiValidator<SuiteState> {

  public OmobilityStatsValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, OmobilityStatsValidatedApiInfo.NAME);
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<SuiteState> apiTests =
      new ValidationSuiteInfo<>(OmobilityStatsSetupValidationSuite::new,
          OmobilityStatsSetupValidationSuite.getParameters(), OmobilityStatsValidationSuite::new);

  @Override
  protected List<ValidationSuiteInfoWithVersions<SuiteState>> getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected SuiteState createState(String url, SemanticVersion version) {
    return new SuiteState(url, version);
  }
}
