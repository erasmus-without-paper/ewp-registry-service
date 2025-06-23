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


@Service
public class CoursesValidator extends ApiValidator<CoursesSuiteState> {
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
  protected List<ValidationSuiteInfoWithVersions<CoursesSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected CoursesSuiteState createState(String url, SemanticVersion version) {
    return new CoursesSuiteState(url, version);
  }
}
