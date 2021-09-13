package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.get;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsSuiteState;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class IMobilityTorsGetValidator extends ApiValidator<IMobilityTorsSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      IMobilityTorsGetValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<IMobilityTorsSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();

    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            IMobilityTorsGetSetupValidationSuiteV1::new,
            IMobilityTorsGetSetupValidationSuiteV1.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            IMobilityTorsGetValidationSuiteV1::new
        )
    );

    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            IMobilityTorsGetSetupValidationSuiteV1::new,
            IMobilityTorsGetSetupValidationSuiteV1.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            IMobilityTorsGetValidationSuiteV1::new
        )
    );
  }

  public IMobilityTorsGetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "imobility-tors",
        ApiEndpoint.Get);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<IMobilityTorsSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected IMobilityTorsSuiteState createState(String url, SemanticVersion version) {
    return new IMobilityTorsSuiteState(url, version);
  }
}
