package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.get;

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
public class OMobilityLAsGetValidator extends ApiValidator<OMobilityLAsSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      OMobilityLAsGetValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<OMobilityLAsSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();

    validationSuites.put(
        new SemanticVersion(0, 3, 0),
        new ValidationSuiteInfo<>(
            OMobilityLAsGetSetupValidationSuiteV030::new,
            OMobilityLAsGetSetupValidationSuiteV030.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(0, 3, 0),
        new ValidationSuiteInfo<>(
            OMobilityLAsGetValidationSuiteV030::new
        )
    );
  }

  public OMobilityLAsGetValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client, ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "omobility-las",
        ApiEndpoint.Get);
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
