package eu.erasmuswithoutpaper.registry.validators.mtdictionariesvalidator;

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
public class MtDictionariesValidator extends ApiValidator<MtDictionariesSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      MtDictionariesValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<MtDictionariesSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();
    validationSuites.put(
        new SemanticVersion(0, 1, 0),
        new ValidationSuiteInfo<>(
            MtDictionariesSetupValidationSuiteV1::new,
            MtDictionariesSetupValidationSuiteV1.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(0, 1, 0),
        new ValidationSuiteInfo<>(MtDictionariesValidationSuiteV1::new)
    );
  }

  public MtDictionariesValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStore validatorKeyStore) {
    super(docBuilder, internet, client, validatorKeyStore, "mt-dictionaries");
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<MtDictionariesSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected MtDictionariesSuiteState createState(String url, SemanticVersion version) {
    return new MtDictionariesSuiteState(url, version);
  }
}
