package eu.erasmuswithoutpaper.registry.validators.factsheetvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;

import org.springframework.beans.factory.annotation.Autowired;

import https.github_com.erasmus_without_paper.ewp_specs_api_factsheet.tree.stable_v1.FactsheetResponse;
import org.junit.jupiter.api.Test;

public class FactsheetValidatorTest extends AbstractApiTest<FactsheetSuiteState> {
  private static String factsheetUrl = "https://university.example.com/factsheet/HTTT/";
  @Autowired
  private FactsheetValidator validator;

  @Override
  protected String getManifestFilename() {
    return "factsheetvalidator/manifest.xml";
  }

  @Override
  protected String getUrl() {
    return factsheetUrl;
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(1, 0, 0);
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    FactsheetServiceV1Valid service =
        new FactsheetServiceV1Valid(factsheetUrl, this.client,
            validatorKeyStoreSet.getMainKeyStore());
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidatingLengthOfHeiIdListIsDetected() {
    FactsheetServiceV1Valid service =
        new FactsheetServiceV1Valid(factsheetUrl, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
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
    FactsheetServiceV1Valid service =
        new FactsheetServiceV1Valid(factsheetUrl, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void checkHeis(RequestData requestData) throws ErrorResponseException {
            ArrayList<String> uniqueHeis = new ArrayList<>(new HashSet<>(requestData.heis));
            if (uniqueHeis.size() > maxHeiIds) {
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
    FactsheetServiceV1Valid service =
        new FactsheetServiceV1Valid(factsheetUrl, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void checkHeis(RequestData requestData) throws ErrorResponseException {
            if (requestData.heis.size() > maxHeiIds - 1) {
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
    FactsheetServiceV1Valid service =
        new FactsheetServiceV1Valid(factsheetUrl, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
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
    FactsheetServiceV1Valid service =
        new FactsheetServiceV1Valid(factsheetUrl, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void handleNoParams(RequestData params)
              throws ErrorResponseException {
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without hei_ids, expect 400.");
  }

  @Test
  public void testReturningEmptyResponseWhenParametersOtherThanHeiIdArePassedIsDetected() {
    FactsheetServiceV1Valid service =
        new FactsheetServiceV1Valid(factsheetUrl, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void handleNoHeiIdParam(RequestData params)
              throws ErrorResponseException {
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request with single incorrect parameter, expect 400.");
  }

  @Test
  public void testReturningNonEmptyResponseForUnknownHeiIdIsDetected() {
    FactsheetServiceV1Valid service =
        new FactsheetServiceV1Valid(factsheetUrl, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected FactsheetResponse.Factsheet processNotCoveredHei(RequestData requestData,
                                                                     String hei) throws ErrorResponseException {
            return createFactsheet(hei);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request one unknown hei_id, expect 200 and empty response.");
  }

  @Test
  public void testReportingAnErrorWhenUnknownHeiIdIsPassedIsDetected() {
    FactsheetServiceV1Valid service =
        new FactsheetServiceV1Valid(factsheetUrl, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected FactsheetResponse.Factsheet processNotCoveredHei(RequestData requestData,
              String hei) throws ErrorResponseException {
            throw new ErrorResponseException(
                this.createErrorResponse(requestData.request, 400, "Unknown HEI ID encountered")
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one known and one unknown hei_id, expect 200 and only one <hei-id> in response.");
  }

  @Test
  public void testReturningDeduplicatedListIsNotAnError() {
    FactsheetServiceV1Valid service =
        new FactsheetServiceV1Valid(factsheetUrl, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected List<FactsheetResponse.Factsheet> processHeis(RequestData requestData)
              throws ErrorResponseException {
            requestData.heis = requestData.heis.stream().distinct().collect(Collectors.toList());
            return super.processHeis(requestData);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Override
  protected ApiValidator<FactsheetSuiteState> getValidator() {
    return validator;
  }
}

