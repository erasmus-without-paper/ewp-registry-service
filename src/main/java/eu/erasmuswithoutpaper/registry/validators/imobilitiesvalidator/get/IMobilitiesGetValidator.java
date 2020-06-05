package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.get;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.IMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.stereotype.Service;

import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class IMobilitiesGetValidator extends ApiValidator<IMobilitiesSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      IMobilitiesGetValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<IMobilitiesSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();

    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            IMobilitiesGetSetupValidationSuiteV1::new,
            IMobilitiesGetSetupValidationSuiteV1.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            IMobilitiesGetValidationSuiteV1::new
        )
    );
  }

  public IMobilitiesGetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "imobilities",
        ApiEndpoint.Get);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<IMobilitiesSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected IMobilitiesSuiteState createState(String url, SemanticVersion version) {
    return new IMobilitiesSuiteState(url, version);
  }
}
