package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;

import org.springframework.beans.factory.annotation.Autowired;

import https.github_com.erasmus_without_paper.ewp_specs_api_institutions.tree.stable_v2.InstitutionsResponse;
import org.junit.jupiter.api.Test;

public class InstitutionValidatorTest extends AbstractApiTest<InstitutionsSuiteState> {
  private static String institutionsUrlHTTT = "https://university.example.com/institutions/HTTT/";
  private static String heiId = "test.hei01.uw.edu.pl";

  @Autowired
  private InstitutionsValidator validator;

  @Override
  protected String getManifestFilename() {
    return "institutionsvalidator/manifest.xml";
  }

  @Override
  protected String getUrl() {
    return institutionsUrlHTTT;
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(2, 0, 0);
  }

  @Override
  protected ApiValidator<InstitutionsSuiteState> getValidator() {
    return validator;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidatingLengthOfHeiIdListIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId) {
          @Override
          protected void checkHeis(RequestData requestData) throws ErrorResponseException {
            //Do nothing.
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-hei-ids> known hei_ids, expect 400.")
        .containsFailure("Request more than <max-hei-ids> unknown hei_ids, expect 400.");
  }

  @Test
  public void testCountingUniqueHeiIdParametersIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId) {
          @Override
          protected void checkHeis(RequestData requestData) throws ErrorResponseException {
            ArrayList<String> uniqueHeis = new ArrayList<>(new HashSet<>(requestData.heiIds));
            if (uniqueHeis.size() > max_hei_ids - 1) {
              errorMaxHeiIdsExceeded(requestData);
            }
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-hei-ids> known hei_ids, expect 400.");
  }

  @Test
  public void testWrongMaxHeiIdInManifestIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId) {
          @Override
          protected void checkHeis(RequestData requestData) throws ErrorResponseException {
            if (requestData.heiIds.size() > max_hei_ids - 1) {
              errorMaxHeiIdsExceeded(requestData);
            }
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one known and one unknown hei_id, expect 200 and only one <hei-id> in response.");
  }

  @Test
  public void testNotReportingAnErrorWhenInvalidHttpMethodIsUsedIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId) {
          @Override
          protected void checkRequestMethod(Request request) throws ErrorResponseException {
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsWarning(
        "Trying Combination[-HTTT] with a PUT request. Expecting to receive a valid HTTP 405 "
            + "error response.");
  }

  @Test
  public void testNotReportingAnErrorWhenNotPassingAnyParameterIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId) {
          @Override
          protected void handleNoParams(
              RequestData requestData)
              throws ErrorResponseException {
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without hei_ids, expect 400.");
  }

  @Test
  public void testReturningEmptyResponseWhenParametersOtherThanHeiIdArePassedIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId) {
          @Override
          protected void handleNoHeiIdsParams(
              RequestData requestData)
              throws ErrorResponseException {
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request with single incorrect parameter, expect 400.");
  }

  @Test
  public void testReturningNonEmptyResponseForUnknownHeiIdIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId) {
          @Override
          protected InstitutionsResponse.Hei processNotCoveredHei(RequestData request, String hei)
              throws ErrorResponseException {
            return createFakeHeiData(hei);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request one unknown hei_id, expect 200 and empty response.");
  }

  @Test
  public void testReportingAnErrorWhenUnknownHeiIdIsPassedIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId) {
          @Override
          protected InstitutionsResponse.Hei processNotCoveredHei(RequestData request, String hei)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                this.createErrorResponse(request.request, 400, "Unknown HEI ID encountered")
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one known and one unknown hei_id, expect 200 and only one <hei-id> in response.");
  }

  @Test
  public void testReturningResponseWhereRootOunitIdInNotOnOunitIdListIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId) {
          @Override
          protected List<String> getCoveredOUnits() {
            return Arrays.asList("1", "2", "3");
          }

          @Override
          protected String getRootOUnit() {
            return "4";
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request for one of known hei_ids, expect 200 OK.");
  }

  @Test
  public void testReturningDeduplicatedListIsNotAnError() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client, heiId) {
          @Override
          protected List<InstitutionsResponse.Hei> processHeis(RequestData requestData)
              throws ErrorResponseException {
            requestData.heiIds =
                requestData.heiIds.stream().distinct().collect(Collectors.toList());
            return super.processHeis(requestData);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }
}

