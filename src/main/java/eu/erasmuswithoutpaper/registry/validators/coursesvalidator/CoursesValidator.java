package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

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
public class CoursesValidator extends ApiValidator<CoursesSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(CoursesValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteInfo<CoursesSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();
    validationSuites.put(
        new SemanticVersion(0, 7, 0),
        new ValidationSuiteInfo<>(
            CoursesSetupValidationSuiteV070::new, CoursesSetupValidationSuiteV070.getParameters())
    );
    validationSuites.put(
        new SemanticVersion(0, 7, 0),
        new ValidationSuiteInfo<>(CoursesValidationSuiteV070::new)
    );
  }

  public CoursesValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStore validatorKeyStore) {
    super(docBuilder, internet, client, validatorKeyStore, "courses");
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteInfo<CoursesSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected CoursesSuiteState createState(String url, SemanticVersion version) {
    return new CoursesSuiteState(url, version);
  }
}
