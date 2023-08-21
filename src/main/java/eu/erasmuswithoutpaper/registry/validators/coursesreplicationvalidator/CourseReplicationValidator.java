package eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator;

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
public class CourseReplicationValidator extends ApiValidator<CourseReplicationSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      CourseReplicationValidator.class);

  public CourseReplicationValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStoreSet validatorKeyStoreSet) {
    super(docBuilder, internet, client, validatorKeyStoreSet, "simple-course-replication");
  }

  @ValidatorTestStep
  public ValidationSuiteInfo<CourseReplicationSuiteState> apiTests = new ValidationSuiteInfo<>(
      CourseReplicationSetupValidationSuite::new,
      CourseReplicationSetupValidationSuite.getParameters(),
      CourseReplicationValidationSuite::new);

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected List<ValidationSuiteInfoWithVersions<CourseReplicationSuiteState>>
      getValidationSuites() {
    return getValidationSuitesFromValidator(this);
  }

  @Override
  protected CourseReplicationSuiteState createState(String url, SemanticVersion version) {
    return new CourseReplicationSuiteState(url, version);
  }
}
