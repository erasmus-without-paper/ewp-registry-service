package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.institutionsvalidator.InstitutionServiceV2Valid;

import org.springframework.beans.factory.annotation.Autowired;

import https.github_com.erasmus_without_paper.ewp_specs_api_ounits.tree.stable_v2.OunitsResponse;
import org.junit.Test;

public class OUnitsValidatorTest extends AbstractApiTest<OUnitsSuiteState> {
  private static final String institutionsUrlHTTT =
      "https://university.example.com/institutions/HTTT/";
  private static final String ounitsUrlHTTT = "https://university.example.com/ounits/HTTT/";
  @Autowired
  private OUnitsValidator validator;

  @Override
  protected String getManifestFilename() {
    return "ounitsvalidator/manifest.xml";
  }

  @Override
  protected String getUrl() {
    return ounitsUrlHTTT;
  }

  private InstitutionServiceV2Valid GetInstitutions() {
    return new InstitutionServiceV2Valid(
        institutionsUrlHTTT, client, validatorKeyStoreSet.getMainKeyStore()) {
      @Override
      protected List<String> getCoveredOUnits() {
        return Arrays.asList("ounit-1", "ounit-2", "ounit-3");
      }

      @Override
      protected String getRootOUnit() {
        return "ounit-3";
      }
    };
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions());
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidationLengthOfOunitIdListIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void errorMaxOUnitIdsExceeded(
              RequestData requestData) throws ErrorResponseException {
            //Do nothing
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-ounit-ids> known ounit_ids, expect 400.");
  }

  @Test
  public void testNotValidatingLengthOfOunitCodeListIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void errorMaxOUnitCodesExceeded(
              RequestData requestData) throws ErrorResponseException {
            //Do nothing
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-ounit-codes> known ounit_codes, expect 400.");
  }

  @Test
  public void testAcceptingRequestWithoutParametersIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void handleNoParams(
              RequestData requestData)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                createOUnitsResponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without hei_id and ounit_ids, expect 400.");
  }

  @Test
  public void testNotReportingAnErrorWhenNoHeiIdIsPassedIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void handleNoHeiId(
              RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createOUnitsResponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without hei_id, expect 400.");
  }

  @Test
  public void testNotReportingAnErrorWhenNeitherOunitIdNorOunitCodeArePassedIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void handleNoIdsNorCodes(
              RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createOUnitsResponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without ounit_ids and ounit_codes, expect 400.");
  }

  @Test
  public void testNotReturningAnErrorWhenAdditionalHeiIdsArePassedIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void handleMultipleHeiIds(
              RequestData requestData) throws ErrorResponseException {
            //Ignore
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request with correct hei_id twice, expect 400.");
    assertThat(report)
        .containsFailure("Request with correct hei_id and incorrect hei_id, expect 400.");
  }

  @Test
  public void testHandlingBothOunitIdsAndOunitCodesIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void handleIdsAndCodes(
              RequestData requestData) throws ErrorResponseException {
            //Ignore
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct ounit_id and correct ounit_code, expect 400.");
  }

  @Test
  public void testHandlingIdsWhenBothIdsAndCodesArePassedIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void handleIdsAndCodes(RequestData requestData) throws ErrorResponseException {
            requestData.ounitCodes.clear();
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct ounit_id and correct ounit_code, expect 400.");
  }

  @Test
  public void testHandlingCodesWhenBothIdsAndCodesArePassedIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void handleIdsAndCodes(
              RequestData requestData) throws ErrorResponseException {
            requestData.ounitIds.clear();
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct ounit_id and correct ounit_code, expect 400.");
  }

  @Test
  public void testReturningNonEmptyResponseWhenNoHeiIdIsPassedIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void handleNoHeiId(
              RequestData requestData) throws ErrorResponseException {
            requestData.heiId = this.institutionsServiceV2.getCoveredHeiIds().get(0);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without hei_id, expect 400.");
  }

  @Test
  public void testReturningNonEmptyResponseWhenNoOunitIdIsPassedIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void handleNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
            String id = this.coveredOUnitsIds.values().iterator().next().getOunitId();
            requestData.ounitIds = Arrays.asList(id);
            requestData.ounitCodes = new ArrayList<>();
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without ounit_ids and ounit_codes, expect 400.");
  }

  @Test
  public void testNotReturningAnErrorWhenUnknownHeiIdsIsPassedIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void errorUnknownHeiId(
              RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createOUnitsResponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request for one of known ounit_ids with unknown hei_id, expect 400.");
  }

  @Test
  public void testReturningNonEmptyResponseWhenUnknownHeiIdsIsPassedIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected void errorUnknownHeiId(
              RequestData requestData) throws ErrorResponseException {
            requestData.heiId = this.institutionsServiceV2.getCoveredHeiIds().get(0);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request for one of known ounit_ids with unknown hei_id, expect 400.");
  }

  @Test
  public void testReturningWrongOunitIdIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected OunitsResponse.Ounit handleKnownOUnit(OunitsResponse.Ounit data) {
            data.setOunitId("invalid-id");
            return data;
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request for one of known ounit-ids, expect 200 OK.");
  }

  @Test
  public void testReturningNonEmptyResponseWhenUnknownOunitIdIsPassedIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected OunitsResponse.Ounit handleUnknownOUnit() {
            return this.coveredOUnitsIds.values().iterator().next();
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request one unknown ounit_id, expect 200 and empty response.");
  }

  @Test
  public void testReturningSingleOunitForMultipleEqualOunitIdsIsAccepted() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected List<OunitsResponse.Ounit> processRequested(
              List<String> requested,
              Map<String, OunitsResponse.Ounit> covered) {
            return new ArrayList<>(new HashSet<>(super.processRequested(requested, covered)));
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testTooLargeMaxOunitIdsInManifestIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected int getMaxOunitIds() {
            return super.getMaxOunitIds() - 1;
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request exactly <max-ounit-ids> known ounit_ids,"
        + " expect 200 and non-empty response.");
  }

  @Test
  public void testTooLargeMaxOunitCodesInManifestIsDetected() {
    OUnitsServiceV2Valid service =
        new OUnitsServiceV2Valid(ounitsUrlHTTT, this.client, GetInstitutions()) {
          @Override
          protected int getMaxOunitCodes() {
            return super.getMaxOunitCodes() - 1;
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request exactly <max-ounit-codes> known ounit_codes,"
            + " expect 200 and non-empty response.");
  }

  @Override
  protected ApiValidator<OUnitsSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(2, 0, 0);
  }
}

