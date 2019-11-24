package eu.erasmuswithoutpaper.registry.validators.mtprojectsvalidator;

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
public class MtProjectsValidator extends ApiValidator<MtProjectsSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      MtProjectsValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<MtProjectsSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();
    validationSuites.put(
        new SemanticVersion(0, 1, 0),
        new ValidationSuiteInfo<>(
            MtProjectsSetupValidationSuiteV1::new,
            MtProjectsSetupValidationSuiteV1.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(0, 1, 0),
        new ValidationSuiteInfo<>(MtProjectsValidationSuiteV1::new)
    );
  }

  public MtProjectsValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "mt-projects");
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<MtProjectsSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected MtProjectsSuiteState createState(String url, SemanticVersion version) {
    return new MtProjectsSuiteState(url, version);
  }
}
