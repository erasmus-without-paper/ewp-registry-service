package eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.coursesreplicationvalidator.CourseReplicationValidator;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;

public class CourseReplicationValidatorTest extends AbstractApiTest {
  private static String courseReplicationUrlHTTT =
      "https://university.example.com/creplication/HTTT/";
  @Autowired
  private CourseReplicationValidator validator;
  private SemanticVersion version100 = new SemanticVersion(1, 0, 0);

  @Override
  protected SemanticVersion getVersion() {
    return version100;
  }

  @Override
  protected String getManifestFilename() {
    return "coursesvalidator/manifest.xml";
  }

  @Test
  public void testAgainstCourseReplicationValid() {
    serviceTest(
        new CourseReplicationServiceV1Valid(
            courseReplicationUrlHTTT, this.client, this.validatorKeyStore),
        courseReplicationUrlHTTT, "coursereplicationvalidator/CourseReplicationValidOutput.txt"
    );
  }

  /**
   * Return 200 when no parameters are provided.
   */
  @Test
  public void testAgainstCourseReplicationInvalid1() {
    serviceTest(
        new CourseReplicationServiceV1Valid(
            courseReplicationUrlHTTT, this.client, this.validatorKeyStore) {
          @Override
          protected void ErrorNoParameters(RequestData requestData,
              Map<String, List<String>> params)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                createCourseReplicationResponse(new ArrayList<>())
            );
          }
        },
        courseReplicationUrlHTTT, "coursereplicationvalidator/CourseReplicationInvalidOutput1.txt"
    );
  }

  /**
   * Return 400 when unknown parameters are passed.
   */
  @Test
  public void testAgainstCourseReplicationInvalid2() {
    serviceTest(
        new CourseReplicationServiceV1Valid(
            courseReplicationUrlHTTT, this.client, this.validatorKeyStore) {
          @Override
          protected void ErrorAdditionalParameters(RequestData requestData,
              Map<String, List<String>> params)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                createErrorResponse(requestData.request, 400, "Unknown parameter")
            );
          }
        },
        courseReplicationUrlHTTT, "coursereplicationvalidator/CourseReplicationInvalidOutput2.txt"
    );
  }

  /**
   * Ignores additional hei-ids.
   */
  @Test
  public void testAgainstCourseReplicationInvalid3() {
    serviceTest(
        new CourseReplicationServiceV1Valid(
            courseReplicationUrlHTTT, this.client, this.validatorKeyStore) {
          @Override
          protected void ErrorMultipleHeiIds(RequestData requestData)
              throws ErrorResponseException {
            //Ignore
          }
        },
        courseReplicationUrlHTTT, "coursereplicationvalidator/CourseReplicationInvalidOutput3.txt"
    );
  }

  /**
   * When passed unknown hei-id returns 200 and empty response.
   */
  @Test
  public void testAgainstCourseReplicationInvalid4() {
    serviceTest(
        new CourseReplicationServiceV1Valid(
            courseReplicationUrlHTTT, this.client, this.validatorKeyStore) {
          @Override
          protected List<String> ProcessNotCoveredHei(RequestData requestData)
              throws ErrorResponseException {
            return new ArrayList<>();
          }
        },
        courseReplicationUrlHTTT, "coursereplicationvalidator/CourseReplicationInvalidOutput4.txt"
    );
  }

  /**
   * When passed unknown hei-id uses one of covered hei-ids.
   */
  @Test
  public void testAgainstCourseReplicationInvalid5() {
    serviceTest(
        new CourseReplicationServiceV1Valid(
            courseReplicationUrlHTTT, this.client, this.validatorKeyStore) {
          @Override
          protected List<String> ProcessNotCoveredHei(RequestData requestData)
              throws ErrorResponseException {
            requestData.requestedHeiId = this.coveredHeiIds.get(0);
            return ProcessCoveredHei(requestData);
          }
        },
        courseReplicationUrlHTTT, "coursereplicationvalidator/CourseReplicationInvalidOutput5.txt"
    );
  }

  /**
   * Returns invalid los-ids.
   * <p>
   * This test is performed in a different manner than all the others. We do not check if whole
   * response is what we expect, but only if it contains info about error of parsing LosID. This is
   * due to the fact that document parser assigns different names to XML namespaces each run. This
   * doesn't happen in any other test of the same kind (e.g. OUnits) and it seems to be too arcane
   * to debug it swiftly.
   */
  @Test
  public void testAgainstCourseReplicationInvalid6() {
    serviceTestContains(
        new CourseReplicationServiceV1Valid(
            courseReplicationUrlHTTT, this.client, this.validatorKeyStore) {
          @Override
          protected List<String> ProcessCoveredHei(RequestData requestData)
              throws ErrorResponseException {
            return Arrays.asList("invalid-id", "invalid-invalid-id");
          }
        },
        courseReplicationUrlHTTT,
        Arrays.asList(
            "HTTP response status was okay, but the content has failed Schema validation. Our "
                + "document parser has reported the following errors:",
            "cvc-pattern-valid: Value 'invalid-id' is not facet-valid with respect to"
                + " pattern '(CR|CLS|MOD|DEP)/(.{1,40})' for type 'LosID'.",
            "cvc-pattern-valid: Value 'invalid-invalid-id' is not facet-valid with respect to"
                + " pattern '(CR|CLS|MOD|DEP)/(.{1,40})' for type 'LosID'."
        )
    );
  }

  /**
   * Accepts invalid dates.
   */
  @Test
  public void testAgainstCourseReplicationInvalid7() {
    serviceTest(
        new CourseReplicationServiceV1Valid(
            courseReplicationUrlHTTT, this.client, this.validatorKeyStore) {
          protected void ErrorInvalidModifiedSince(RequestData requestData)
              throws ErrorResponseException {
            requestData.requestedModifiedSinceDate = null;
          }
        },
        courseReplicationUrlHTTT, "coursereplicationvalidator/CourseReplicationInvalidOutput7.txt"
    );
  }

  @Override
  protected ApiValidator GetValidator() {
    return validator;
  }
}

