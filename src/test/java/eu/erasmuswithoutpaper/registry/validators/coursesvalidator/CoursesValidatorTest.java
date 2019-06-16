package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator.CourseReplicationServiceV1Valid;
import eu.erasmuswithoutpaper.registry.validators.types.CoursesResponse;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;

public class CoursesValidatorTest extends AbstractApiTest {
  private static String replicationUrlHTTT = "https://university.example.com/creplication/HTTT/";
  private static String coursesUrlHTTT = "https://university.example.com/courses/HTTT/";
  @Autowired
  protected CoursesValidator validator;
  private SemanticVersion version070 = new SemanticVersion(0, 7, 0);

  @Override
  protected String getManifestFilename() {
    return "coursesvalidator/manifest.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return version070;
  }

  @Override
  protected ApiValidator GetValidator() {
    return validator;
  }

  private CourseReplicationServiceV1Valid GetCoursesReplication() {
    return new CourseReplicationServiceV1Valid(replicationUrlHTTT, client, validatorKeyStore);
  }

  @Test
  public void testAgainstCoursesValid() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {},
        coursesUrlHTTT, "coursesvalidator/CoursesValidOutput.txt"
    );
  }

  /**
   * Doesn't validate length of los-id list.
   */
  @Test
  public void testAgainstCoursesInvalid1() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorMaxLosIdsExceeded(
              RequestData requestData) throws ErrorResponseException {
            //Do nothing
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput1.txt"
    );
  }

  /**
   * Doesn't validate length of los-code list.
   */
  @Test
  public void testAgainstCoursesInvalid2() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorMaxLosCodesExceeded(
              RequestData requestData) throws ErrorResponseException {
            //Do nothing
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput2.txt"
    );
  }

  /**
   * Return 200 when no parameters are provided.
   */
  @Test
  public void testAgainstCoursesInvalid3() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorNoParams(
              RequestData requestData)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                createCoursesResponse(new ArrayList<>())
            );
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput3.txt"
    );
  }

  /**
   * Return 400 when unknown parameters are passed.
   */
  @Test
  public void testAgainstCoursesInvalid4() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void HandleUnexpectedParams(RequestData requestData)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                createErrorResponse(requestData.request, 400, "Unknown parameter")
            );
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput4.txt"
    );
  }

  /**
   * Returns 200 when hei-id is not passed.
   */
  @Test
  public void testAgainstCoursesInvalid5() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorNoHeiId(RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createCoursesResponse(new ArrayList<>())
            );
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput5.txt"
    );
  }

  /**
   * Returns 200 when los-code and los-id is not passed.
   */
  @Test
  public void testAgainstCoursesInvalid6() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createCoursesResponse(new ArrayList<>())
            );
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput6.txt"
    );
  }

  /**
   * Ignores additional hei-ids.
   */
  @Test
  public void testAgainstCoursesInvalid7() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorMultipleHeiIds(RequestData requestData)
              throws ErrorResponseException {
            //Ignore
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput7.txt"
    );
  }

  /**
   * Handles both ids and codes.
   */
  @Test
  public void testAgainstCoursesInvalid8() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
            //Ignore
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput8.txt"
    );
  }

  /**
   * When ids and codes are passed, ignored codes.
   */
  @Test
  public void testAgainstCoursesInvalid9() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
            requestData.losCodes.clear();
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput9.txt"
    );
  }

  /**
   * When ids and codes are passed, ignored ids.
   */
  @Test
  public void testAgainstCoursesInvalid10() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
            requestData.losIds.clear();
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput10.txt"
    );
  }

  /**
   * When hei-id is not provided first covered hei is used.
   */
  @Test
  public void testAgainstCoursesInvalid11() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorNoHeiId(RequestData requestData) throws ErrorResponseException {
            requestData.heiId = this.CourseReplicationServiceV2.GetCoveredHeiIds().get(0);
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput11.txt"
    );
  }

  /**
   * When los-id is not provided first covered is used.
   */
  @Test
  public void testAgainstCoursesInvalid12() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
            String id = this.coveredLosIds.values().iterator().next().getLosId();
            requestData.losIds = Arrays.asList(id);
            requestData.losCodes = new ArrayList<>();
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput12.txt"
    );
  }

  /**
   * When passed unknown hei-id returns 200 and empty response.
   */
  @Test
  public void testAgainstCoursesInvalid13() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createCoursesResponse(new ArrayList<>())
            );
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput13.txt"
    );
  }

  /**
   * When passed unknown hei-id uses one of covered hei-ids.
   */
  @Test
  public void testAgainstCoursesInvalid14() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
            requestData.heiId = this.CourseReplicationServiceV2.GetCoveredHeiIds().get(0);
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput14.txt"
    );
  }

  /**
   * Returns wrong los-ids.
   * <p>
   * This test is performed in a different manner than all the others. We do not check if whole
   * response is what we expect, but only if it contains info about error of parsing LosID. This is
   * due to the fact that document parser assigns different names to XML namespaces each run. This
   * doesn't happen in any other test of the same kind (e.g. OUnits) and it seems to be too arcane
   * to debug it swiftly.
   */
  @Test
  public void testAgainstCoursesInvalid15() {
    serviceTestContains(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected CoursesResponse.LearningOpportunitySpecification HandleKnownLos(
              RequestData requestData,
              CoursesResponse.LearningOpportunitySpecification data) {
            data.setLosId("invalid-id");
            return data;
          }
        },
        coursesUrlHTTT,
        Arrays.asList(
            "HTTP response status was okay, but the content has failed Schema validation. Our "
                + "document parser has reported the following errors:",
            "1. (Line 4) cvc-pattern-valid: Value 'invalid-id' is not facet-valid with respect to"
                + " pattern '(CR|CLS|MOD|DEP)/(.{1,40})' for type 'LosID'."
        )
    );
  }

  /**
   * Returns some data for unknown los-ids.
   */
  @Test
  public void testAgainstCoursesInvalid16() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected CoursesResponse.LearningOpportunitySpecification HandleUnknownLos(
              RequestData requestData) {
            return this.coveredLosIds.values().iterator().next();
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput16.txt"
    );
  }

  /**
   * Too large max-los-ids in manifest.
   */
  @Test
  public void testAgainstCoursesInvalid17() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected int getMaxLosIds() {
            return super.getMaxLosIds() - 1;
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput17.txt"
    );
  }

  /**
   * Too large max-los-codes in manifest.
   */
  @Test
  public void testAgainstCoursesInvalid18() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected int getMaxLosCodes() {
            return super.getMaxLosCodes() - 1;
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput18.txt"
    );
  }

  /**
   * Accepts invalid dates.
   */
  @Test
  public void testAgainstCoursesInvalid19() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected XMLGregorianCalendar ErrorDateFormat(
              RequestData requestData) throws ErrorResponseException {
            try {
              return DatatypeFactory.newInstance()
                  .newXMLGregorianCalendar(2000, 1, 1, 0, 0, 0, 0, 0);
            } catch (DatatypeConfigurationException e) {
              // Shouldn't happen
              assert false;
              return null;
            }
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput19.txt"
    );
  }

  /**
   * Accepts valid dates but in invalid format.
   */
  @Test
  public void testAgainstCoursesInvalid20() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected XMLGregorianCalendar CheckDateFormat(
              RequestData requestData,
              String date)
              throws ErrorResponseException {
            try {
              return super.CheckDateFormat(requestData, date);
            } catch (ErrorResponseException e) {
              // Ignore, try different format
            }

            try {
              DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
              formatter.parse(date); // Check pattern
              return DatatypeFactory.newInstance()
                  .newXMLGregorianCalendar(2000, 1, 1, 0, 0, 0, 0, 0);
            } catch (DateTimeParseException | DatatypeConfigurationException e) {
              return ErrorDateFormat(requestData);
            }
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput20.txt"
    );
  }

  /**
   * Ignores multiple lois_after
   */
  @Test
  public void testAgainstCoursesInvalid21() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorMultipleLoisAfter(
              RequestData requestData) throws ErrorResponseException {
            // Ignore
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput21.txt"
    );
  }

  /**
   * Ignores multiple lois_before
   */
  @Test
  public void testAgainstCoursesInvalid22() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void ErrorMultipleLoisBefore(
              RequestData requestData) throws ErrorResponseException {
            // Ignore
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput22.txt"
    );
  }

  /**
   * Adds timezone to dates in response.
   */
  @Test
  public void testAgainstCoursesInvalid23() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected int getTimeZone() {
            return 2;
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesValidOutput.txt"
    );
  }

  /**
   * Adds timezone to dates in response.
   */
  @Test
  public void testAgainstCoursesInvalid24() {
    serviceTest(
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected int getTimeZone() {
            return 0;
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesValidOutput.txt"
    );
  }

}

