package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.stereotype.Service;

import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class InstitutionsValidator extends ApiValidator<InstitutionsSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(InstitutionsValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteFactory<InstitutionsSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();
    validationSuites.put(new SemanticVersion(2, 0, 0), InstitutionsSetupValidationSuiteV2::new);
    validationSuites.put(new SemanticVersion(2, 0, 0), InstitutionsValidationSuiteV200::new);
  }

  /**
   * @param docBuilder
   *     Needed for validating Institutions API responses against the schemas.
   * @param internet
   *     Needed to make Institutions API requests across the network.
   * @param client
   *     Needed to fetch (and verify) Institutions APIs' security settings.
   * @param validatorKeyStore
   *     Source of keys and certificates used by validator.
   */
  public InstitutionsValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStore validatorKeyStore) {
    super(docBuilder, internet, client, validatorKeyStore, "institutions");
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteFactory<InstitutionsSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected InstitutionsSuiteState createState() {
    return new InstitutionsSuiteState();
  }
}
