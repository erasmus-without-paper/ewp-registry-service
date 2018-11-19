package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for validating external Echo API implementations.
 */
@Service
public class EchoValidator extends ApiValidator {
  @Autowired
  public EchoValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStore validatorKeyStore) {
    super(docBuilder, internet, client, validatorKeyStore);
  }

  @Override
  public List<ValidationStepWithStatus> runTests(String urlStr) {
    EchoValidationSuite suite =
        new EchoValidationSuite(this, this.docBuilder, this.internet, urlStr, this.client,
            this.repo);
    suite.run();
    return suite.getResults();
  }

  private static final Logger logger = LoggerFactory.getLogger(EchoValidator.class);

  @Override
  public Logger getLogger() {
    return logger;
  }
}
