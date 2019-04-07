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
  private static final Logger logger = LoggerFactory.getLogger(
      CoursesValidator.class);

  @Override
  public Logger getLogger() {
    return logger;
  }

  private static ListMultimap<SemanticVersion, ValidationSuiteFactory<CoursesSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();
    validationSuites.put(
        new SemanticVersion(0, 7, 0),
        CoursesSetupValidationSuiteV070::new
    );
    validationSuites.put(
        new SemanticVersion(0, 7, 0),
        CoursesValidationSuiteV070::new
    );
  }

  public CoursesValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStore validatorKeyStore) {
    super(docBuilder, internet, client, validatorKeyStore, "courses");
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteFactory<CoursesSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected CoursesSuiteState createState(String url, SemanticVersion version) {
    return new CoursesSuiteState(url, version);
  }
}
