package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.get.IiaGetSetupValidationSuiteV2;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.get.IiaGetValidationSuiteV2;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v3.get.IiaGetSetupValidationSuiteV3;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v3.get.IiaGetValidationSuiteV3;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v4.get.IiaGetSetupValidationSuiteV4;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v4.get.IiaGetValidationSuiteV4;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.stereotype.Service;

import com.google.common.collect.ListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class IiaGetValidator extends ApiValidator<IiaSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      IiaGetValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<IiaSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();

    validationSuites.put(
        new SemanticVersion(2, 0, 0),
        new ValidationSuiteInfo<>(
            IiaGetSetupValidationSuiteV2::new,
            IiaGetSetupValidationSuiteV2.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(2, 0, 0),
        new ValidationSuiteInfo<>(IiaGetValidationSuiteV2::new)
    );

    validationSuites.put(
        new SemanticVersion(3, 0, 0),
        new ValidationSuiteInfo<>(
            IiaGetSetupValidationSuiteV3::new,
            IiaGetSetupValidationSuiteV3.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(3, 0, 0),
        new ValidationSuiteInfo<>(IiaGetValidationSuiteV3::new)
    );

    validationSuites.put(
        new SemanticVersion(4, 0, 0),
        new ValidationSuiteInfo<>(
            IiaGetSetupValidationSuiteV4::new,
            IiaGetSetupValidationSuiteV4.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(4, 0, 0),
        new ValidationSuiteInfo<>(IiaGetValidationSuiteV4::new)
    );
  }

  public IiaGetValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "iias", ApiEndpoint.Get);
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
