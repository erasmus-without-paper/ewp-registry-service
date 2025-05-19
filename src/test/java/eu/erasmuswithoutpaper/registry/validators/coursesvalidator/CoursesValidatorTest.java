package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator.CourseReplicationServiceV1Valid;

import https.github_com.erasmus_without_paper.ewp_specs_api_courses.tree.stable_v1.CoursesResponse;

public class CoursesValidatorTest extends AbstractApiTest<CoursesSuiteState> {
  private static final String replicationUrlHTTT = "https://university.example.com/creplication/HTTT/";
  private static final String coursesUrlHTTT = "https://university.example.com/courses/HTTT/";
  @Autowired
  protected CoursesValidator validator;
  private SemanticVersion version070 = new SemanticVersion(0, 7, 0);

  @Override
  protected String getManifestFilename() {
    return "coursesvalidator/manifest.xml";
  }

  @Override
  protected String getUrl() {
    return coursesUrlHTTT;
  }

  @Override
  protected SemanticVersion getVersion() {
    return version070;
  }

  @Override
  protected ApiValidator<CoursesSuiteState> getValidator() {
    return validator;
  }

  private CourseReplicationServiceV1Valid GetCoursesReplication() {
    return new CourseReplicationServiceV1Valid(
        replicationUrlHTTT, client, validatorKeyStoreSet.getMainKeyStore()
    );
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication());
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidationLengthOfLosIdListIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorMaxLosIdsExceeded(
              RequestData requestData) throws ErrorResponseException {
            //Do nothing
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-los-ids> known los_ids, expect 400.");
  }

  @Test
  public void testNotValidationLengthOfLosCodeListIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorMaxLosCodesExceeded(
              RequestData requestData) throws ErrorResponseException {
            //Do nothing
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-los-codes> known los_codes, expect 400.");
  }

  @Test
  public void testReturningCorrectResponseWhenNoParametersArePassedIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorNoParams(
              RequestData requestData)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                createCoursesResponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without parameters, expect 400.");
  }

  @Test
  public void testNotReportingAnErrorWhenHeiIdParameterIsMissingIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorNoHeiId(RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createCoursesResponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without hei_id, expect 400.");
  }

  @Test
  public void testNotReportingAnErrorWhenNeitherLosCodeNorLosIdIsPassedIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createCoursesResponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without los_ids and los_codes, expect 400.");
  }

  @Test
  public void testIgnoringAdditionalHeiIdsIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorMultipleHeiIds(RequestData requestData)
              throws ErrorResponseException {
            //Ignore
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request with correct hei_id twice, expect 400.");
  }

  @Test
  public void testHandlingBothIdsAndCodesIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
            //Ignore
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct los_id and correct los_code, expect 400.");
  }

  @Test
  public void testHandlingIdsWhenIdsAndCodesArePassedIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
            requestData.losCodes.clear();
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct los_id and correct los_code, expect 400.");
  }

  @Test
  public void testHandlingCodesWhenIdsAndCodesArePassedIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
            requestData.losIds.clear();
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct los_id and correct los_code, expect 400.");
  }

  @Test
  public void testReturningNonEmptyResponseWhenNoHeiIdIsPassedIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorNoHeiId(RequestData requestData) throws ErrorResponseException {
            requestData.heiId = this.CourseReplicationServiceV2.getCoveredHeiIds().get(0);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without hei_id, expect 400.");
  }

  @Test
  public void testReturningNonEmptyResponseWhenNoLosIdIsPassedIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
            String id = this.coveredLosIds.values().iterator().next().getLosId();
            requestData.losIds = Arrays.asList(id);
            requestData.losCodes = new ArrayList<>();
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without los_ids and los_codes, expect 400.");
  }

  @Test
  public void testReturningNonEmptyResponseWhenUnknownHeiIdIsPassedIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createCoursesResponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request for one of known los_ids with unknown hei_id, expect 400.");
  }

  @Test
  public void testRetruningNonEmptyResponseWhenUnknownHeiIdIsPassed() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
            requestData.heiId = this.CourseReplicationServiceV2.getCoveredHeiIds().get(0);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request for one of known los_ids with unknown hei_id, expect 400.");
  }

  @Test
  public void testReturningInvalidLosIdsIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected CoursesResponse.LearningOpportunitySpecification handleKnownLos(
              RequestData requestData,
              CoursesResponse.LearningOpportunitySpecification data) {
            data.setLosId("invalid-id");
            return data;
          }
        };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request for one of known los-ids, expect 200 OK.");
  }

  @Test
  public void testReturningNonEmptyResponseForUnknownLosIdsIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected CoursesResponse.LearningOpportunitySpecification handleUnknownLos(
              RequestData requestData) {
            return this.coveredLosIds.values().iterator().next();
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request one unknown los_id, expect 200 and empty response.");
  }

  @Test
  public void testReturningSingleLosForMultipleEqualLosIdsIsAccepted() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected List<CoursesResponse.LearningOpportunitySpecification> processRequested(
              RequestData requestData, List<String> requested,
              Map<String, CoursesResponse.LearningOpportunitySpecification> covered) {
            return new ArrayList<>(
                new HashSet<>(super.processRequested(requestData, requested, covered)));
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }


  @Test
  public void testTooLargeMaxLosIdsInManifestIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected int getMaxLosIds() {
            return super.getMaxLosIds() - 1;
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request exactly <max-los-ids> known los_ids,"
        + " expect 200 and non-empty response.");
  }

  @Test
  public void testTooLargeMaxLosCodesInManifestIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected int getMaxLosCodes() {
            return super.getMaxLosCodes() - 1;
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request exactly <max-los-codes> known los_codes,"
        + " expect 200 and non-empty response.");
  }

  @Test
  public void testAcceptingInvalidDatesIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected XMLGregorianCalendar errorDateFormat(
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
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("lois_before parameter is not a date, expect 400.");
  }

  @Test
  public void testAcceptingValidDatesInInvalidFormatIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected XMLGregorianCalendar checkDateFormat(
              RequestData requestData,
              String date)
              throws ErrorResponseException {
            try {
              return super.checkDateFormat(requestData, date);
            } catch (ErrorResponseException e) {
              // Ignore, try different format
            }

            try {
              DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
              formatter.parse(date); // Check pattern
              return DatatypeFactory.newInstance()
                  .newXMLGregorianCalendar(2000, 1, 1, 0, 0, 0, 0, 0);
            } catch (DateTimeParseException | DatatypeConfigurationException e) {
              return errorDateFormat(requestData);
            }
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("lois_before has format dd-MM-yyyy, expect 400.");
  }

  @Test
  public void testIgnoringMultipleLoisAfterParametersIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorMultipleLoisAfter(
              RequestData requestData) throws ErrorResponseException {
            // Ignore
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Multiple lois_after parameters, expect 400.");
  }

  @Test
  public void testIgnoringMultipleLoisBeforeParametersIsDetected() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected void errorMultipleLoisBefore(
              RequestData requestData) throws ErrorResponseException {
            // Ignore
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Multiple lois_before parameters, expect 400.");
  }

  @Test
  public void testValidatingResponsesWithNonZeroTimezoneIsSuccessful() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected int getTimeZone() {
            return 2;
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testValidatingResponsesWithZeroTimezoneIsSuccessful() {
    CoursesServiceV070Valid service =
        new CoursesServiceV070Valid(coursesUrlHTTT, this.client, GetCoursesReplication()) {
          @Override
          protected int getTimeZone() {
            return 0;
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }
}

