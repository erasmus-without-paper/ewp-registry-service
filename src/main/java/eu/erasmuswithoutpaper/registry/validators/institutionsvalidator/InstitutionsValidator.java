package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class InstitutionsValidator extends ApiValidator {
  private static final Logger logger = LoggerFactory.getLogger(InstitutionsValidator.class);

  /**
   * @param docBuilder Needed for validating Institutions API responses against the schemas.
   * @param internet   Needed to make Institutions API requests across the network.
   * @param client     Needed to fetch (and verify) Institutions APIs' security settings.
   * @param validatorKeyStore Source of keys and certificates used by validator.
   */

  public InstitutionsValidator(
      EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStore validatorKeyStore) {
    super(docBuilder, internet, client, validatorKeyStore);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public List<ValidationStepWithStatus> runTests(String urlStr) {
    InstitutionsValidationSuite suite =
        new InstitutionsValidationSuite(this, this.docBuilder, this.internet, urlStr, this.client,
            repo);
    suite.run();
    return suite.getResults();
  }
}
