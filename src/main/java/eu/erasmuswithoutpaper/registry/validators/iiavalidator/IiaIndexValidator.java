package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.index.IiaIndexBasicSetupValidationSuiteV2;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.index.IiaIndexBasicValidationSuiteV2;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.index.IiaIndexComplexSetupValidationSuiteV2;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.index.IiaIndexComplexValidationSuiteV2;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v3.index.IiaIndexBasicSetupValidationSuiteV3;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v3.index.IiaIndexBasicValidationSuiteV3;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v3.index.IiaIndexComplexSetupValidationSuiteV3;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v3.index.IiaIndexComplexValidationSuiteV3;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v4.index.IiaIndexBasicSetupValidationSuiteV4;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v4.index.IiaIndexBasicValidationSuiteV4;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v4.index.IiaIndexComplexSetupValidationSuiteV4;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.v4.index.IiaIndexComplexValidationSuiteV4;
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

    validationSuites.put(
        new SemanticVersion(3, 0, 0),
        new ValidationSuiteInfo<>(
            IiaIndexBasicSetupValidationSuiteV3::new,
            IiaIndexBasicSetupValidationSuiteV3.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(3, 0, 0),
        new ValidationSuiteInfo<>(IiaIndexBasicValidationSuiteV3::new)
    );
    validationSuites.put(
        new SemanticVersion(3, 0, 0),
        new ValidationSuiteInfo<>(
            IiaIndexComplexSetupValidationSuiteV3::new,
            IiaIndexComplexSetupValidationSuiteV3.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(3, 0, 0),
        new ValidationSuiteInfo<>(IiaIndexComplexValidationSuiteV3::new)
    );

    validationSuites.put(
        new SemanticVersion(4, 0, 0),
        new ValidationSuiteInfo<>(
            IiaIndexBasicSetupValidationSuiteV4::new,
            IiaIndexBasicSetupValidationSuiteV4.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(4, 0, 0),
        new ValidationSuiteInfo<>(IiaIndexBasicValidationSuiteV4::new)
    );
    validationSuites.put(
        new SemanticVersion(4, 0, 0),
        new ValidationSuiteInfo<>(
            IiaIndexComplexSetupValidationSuiteV4::new,
            IiaIndexComplexSetupValidationSuiteV4.getParameters()
        )
    );
    validationSuites.put(
        new SemanticVersion(4, 0, 0),
        new ValidationSuiteInfo<>(IiaIndexComplexValidationSuiteV4::new)
    );
  }

  public IiaIndexValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "iias",
        ApiEndpoint.Index);
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
