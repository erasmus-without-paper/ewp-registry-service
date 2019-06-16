package eu.erasmuswithoutpaper.registry.validators.iiavalidator.index;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class IiaIndexValidator extends ApiValidator<IiaSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(IiaIndexValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<IiaSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();
    validationSuites.put(
        new SemanticVersion(2, 0, 0),
        new ValidationSuiteInfo<>(
            IiaIndexBasicSetupValidationSuiteV2::new,
            IiaIndexBasicSetupValidationSuiteV2.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(2, 0, 0),
        new ValidationSuiteInfo<>(IiaIndexBasicValidationSuiteV2::new)
    );
    validationSuites.put(
        new SemanticVersion(2, 0, 0),
        new ValidationSuiteInfo<>(
            IiaIndexComplexSetupValidationSuiteV2::new,
            IiaIndexComplexSetupValidationSuiteV2.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(2, 0, 0),
        new ValidationSuiteInfo<>(IiaIndexComplexValidationSuiteV2::new)
    );
  }

  public IiaIndexValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStore validatorKeyStore) {
    super(docBuilder, internet, client, validatorKeyStore, "iias", "index");
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<IiaSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected IiaSuiteState createState(String url, SemanticVersion version) {
    return new IiaSuiteState(url, version);
  }
}
