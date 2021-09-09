package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.update;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.OMobilityLAsSuiteState;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.stereotype.Service;

import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class OMobilityLAsUpdateValidator extends ApiValidator<OMobilityLAsSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      OMobilityLAsUpdateValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<OMobilityLAsSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();

    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            OMobilityLAsUpdateSetupValidationSuiteV1::new,
            OMobilityLAsUpdateSetupValidationSuiteV1.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(1, 0, 0),
        new ValidationSuiteInfo<>(
            OMobilityLAsUpdateValidationSuiteV1::new
        )
    );
  }

  public OMobilityLAsUpdateValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobility-las",
        ApiEndpoint.Update);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<OMobilityLAsSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected OMobilityLAsSuiteState createState(String url, SemanticVersion version) {
    return new OMobilityLAsSuiteState(url, version);
  }
}
