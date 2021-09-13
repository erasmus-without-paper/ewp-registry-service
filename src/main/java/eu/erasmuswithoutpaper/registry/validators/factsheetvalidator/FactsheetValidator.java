package eu.erasmuswithoutpaper.registry.validators.factsheetvalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.stereotype.Service;

import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FactsheetValidator extends ApiValidator<FactsheetSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      FactsheetValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<FactsheetSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();
    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            FactsheetSetupValidationSuiteV1::new,
            FactsheetSetupValidationSuiteV1
                .getParameters()
        )

    );
    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            FactsheetValidationSuiteV1::new)
    );
  }

  public FactsheetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "factsheet");
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<FactsheetSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected FactsheetSuiteState createState(String url, SemanticVersion version) {
    return new FactsheetSuiteState(url, version);
  }
}
