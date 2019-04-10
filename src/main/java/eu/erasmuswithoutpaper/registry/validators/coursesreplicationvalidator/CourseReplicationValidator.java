package eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator;

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
public class CourseReplicationValidator extends ApiValidator<CourseReplicationSuiteState> {
  private static final Logger logger = LoggerFactory.getLogger(
      CourseReplicationValidator.class);
  private static ListMultimap<SemanticVersion, ValidationSuiteFactory<CourseReplicationSuiteState>>
      validationSuites;

  static {
    validationSuites = ApiValidator.createMultimap();
    validationSuites.put(
        new SemanticVersion(1, 0, 0, 9),
        CourseReplicationSetupValidationSuiteV100::new
    );
    validationSuites.put(
        new SemanticVersion(1, 0, 0, 9),
        CourseReplicationValidationSuiteV100::new
    );
  }

  public CourseReplicationValidator(EwpDocBuilder docBuilder, Internet internet,
      RegistryClient client,
      ValidatorKeyStore validatorKeyStore) {
    super(docBuilder, internet, client, validatorKeyStore, "simple-course-replication");
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  protected ListMultimap<SemanticVersion, ValidationSuiteFactory<CourseReplicationSuiteState>>
      getValidationSuites() {
    return validationSuites;
  }

  @Override
  protected CourseReplicationSuiteState createState(String url, SemanticVersion version) {
    return new CourseReplicationSuiteState(url, version);
  }
}
