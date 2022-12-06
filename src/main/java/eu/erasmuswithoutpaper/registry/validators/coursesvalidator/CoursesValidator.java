package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.ValidatorTestStep;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class CoursesValidator extends ApiValidator<CoursesSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(CoursesValidator.class);

  public CoursesValidator(EwpDocBuilder docBuilder, Internet internet, RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "courses");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<CoursesSuiteState> universalTests = new ValidationSuiteInfo<>(
      CoursesSetupValidationSuite::new,
      CoursesSetupValidationSuite.getParameters(),
      CoursesValidationSuite::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<CoursesSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected CoursesSuiteState createState(String url, SemanticVersion version) {
    return new CoursesSuiteState(url, version);
  }
}
