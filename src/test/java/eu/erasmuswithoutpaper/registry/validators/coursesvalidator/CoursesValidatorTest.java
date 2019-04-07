package eu.erasmuswithoutpaper.registry.validators.coursesvalidator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.coursereplicationvalidator.CourseReplicationServiceV2Valid;
import eu.erasmuswithoutpaper.registry.validators.types.CoursesResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import org.apache.xerces.impl.dv.util.Base64;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CoursesValidatorTest extends AbstractApiTest {
  private static String replicationUrlHTTT;
  private static String coursesUrlHTTT;
  @Autowired
  protected CoursesValidator validator;
  private SemanticVersion version070 = new SemanticVersion(0, 7, 0);

  @BeforeClass
  public static void setUpClass() {
    selfManifestUrl = "https://registry.example.com/manifest.xml";
    apiManifestUrl = "https://university.example.com/manifest.xml";
    coursesUrlHTTT = "https://university.example.com/courses/HTTT/";
    replicationUrlHTTT = "https://university.example.com/creplication/HTTT/";
    needsReinit = true;
  }

  @Before
  public void setUp() {
    if (needsReinit) {
      /*
       * Minimal setup for the services to guarantee that repo contains a valid catalogue,
       * consistent with the certificates returned by the validator.
       */
      this.sourceProvider.clearSources();
      this.repo.deleteAll();
      this.internet.clearAll();

      String myManifest = this.selfManifestProvider.getManifest();
      this.internet.putURL(selfManifestUrl, myManifest);
      this.sourceProvider.addSource(ManifestSource.newTrustedSource(selfManifestUrl));

      String apiManifest = this.getFileAsString("coursesvalidator/manifest.xml");
      myKeyPair = this.validator.generateKeyPair();
      apiManifest = apiManifest.replace(
          "SERVER-KEY-PLACEHOLDER",
          Base64.encode(myKeyPair.getPublic().getEncoded())
      );
      this.internet.putURL(apiManifestUrl, apiManifest);
      this.sourceProvider
          .addSource(ManifestSource.newRegularSource(apiManifestUrl, Lists.newArrayList()));

      this.registryUpdater.reloadAllManifestSources();
      needsReinit = false;
    }
  }

  private void serviceTestContains(FakeInternetService service, String url, List<String> expected) {
    try {
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(url, version070, null)).contains(expected);
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  private void serviceTest(FakeInternetService service, String url, String filename) {
    try {
      this.internet.addFakeInternetService(service);
      assertThat(this.getValidatorReport(url, version070, null))
          .isEqualTo(this.getFileAsString(filename));
      this.internet.removeFakeInternetService(service);

    } finally {
      this.internet.clearAll();
    }
  }

  private CourseReplicationServiceV2Valid GetCoursesReplication() {
    return new CourseReplicationServiceV2Valid(replicationUrlHTTT, client, validatorKeyStore);
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
          protected void ErrorMaxLosIdsExceeded() throws ErrorResponseException {
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
          protected void ErrorMaxLosCodesExceeded() throws ErrorResponseException {
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
          protected void ErrorNoParams()
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
          protected void HandleUnexpectedParams() throws ErrorResponseException {
            throw new ErrorResponseException(
                createErrorResponse(this.currentRequest, 400, "Unknown parameter")
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
          protected void ErrorNoHeiId() throws ErrorResponseException {
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
          protected void ErrorNoIdsNorCodes() throws ErrorResponseException {
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
          protected void ErrorMultipleHeiIds() throws ErrorResponseException {
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
          protected void ErrorIdsAndCodes() throws ErrorResponseException {
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
          protected void ErrorIdsAndCodes() throws ErrorResponseException {
            this.requestedLosCodes.clear();
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
          protected void ErrorIdsAndCodes() throws ErrorResponseException {
            this.requestedLosIds.clear();
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
          protected void ErrorNoHeiId() throws ErrorResponseException {
            this.requestedHeiId = this.CourseReplicationServiceV2.GetCoveredHeiIds().get(0);
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
          protected void ErrorNoIdsNorCodes() throws ErrorResponseException {
            String id = this.coveredLosIds.values().iterator().next().getLosId();
            this.requestedLosIds = Arrays.asList(id);
            this.requestedLosCodes = new ArrayList<>();
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
          protected void ErrorUnknownHeiId() throws ErrorResponseException {
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
          protected void ErrorUnknownHeiId() throws ErrorResponseException {
            this.requestedHeiId = this.CourseReplicationServiceV2.GetCoveredHeiIds().get(0);
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
          protected CoursesResponse.LearningOpportunitySpecification HandleUnknownLos() {
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
          protected XMLGregorianCalendar ErrorDateFormat() throws ErrorResponseException {
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
          protected XMLGregorianCalendar CheckDateFormat(String date)
              throws ErrorResponseException {
            try {
              return super.CheckDateFormat(date);
            } catch (ErrorResponseException e) {
              // Ignore, try different format
            }

            try {
              DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
              formatter.parse(date); // Check pattern
              return DatatypeFactory.newInstance()
                  .newXMLGregorianCalendar(2000, 1, 1, 0, 0, 0, 0, 0);
            } catch (DateTimeParseException | DatatypeConfigurationException e) {
              return ErrorDateFormat();
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
          protected void ErrorMultipleLoisAfter() throws ErrorResponseException {
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
          protected void ErrorMultipleLoisBefore() throws ErrorResponseException {
            // Ignore
          }
        },
        coursesUrlHTTT, "coursesvalidator/CoursesInvalidOutput22.txt"
    );
  }

  @Override
  protected ApiValidator GetValidator() {
    return validator;
  }
}

