package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

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
public class OUnitsValidator extends ApiValidator<OUnitsSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(OUnitsValidator.class);

  @Override
  public Logger getLogger() {
    return logger;
  }

  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<OUnitsSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();
    validationSuites.put(
        new SemanticVersion(2, 0, 0),
        new ValidationSuiteInfo<>(
            OUnitsSetupValidationSuiteV2::new,
            OUnitsSetupValidationSuiteV2.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(2, 0, 0),
        new ValidationSuiteInfo<>(OUnitsValidationSuiteV2::new)
    );
  }

  public OUnitsValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStore validatorKeyStore) {
    super(docBuilder, internet, client, validatorKeyStore, "organizational-units");
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<OUnitsSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected OUnitsSuiteState createState(String url, SemanticVersion version) {
    return new OUnitsSuiteState(url, version);
  }
}
