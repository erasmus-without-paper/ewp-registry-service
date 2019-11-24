package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.types.InstitutionsResponse;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;

public class InstitutionValidatorTest extends AbstractApiTest {
  private static String institutionsUrlHTTT = "https://university.example.com/institutions/HTTT/";
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

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore());
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidatingLengthOfHeiIdListIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void checkHeis(List<String> heis) throws ErrorResponseException {
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
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void checkHeis(List<String> heis) throws ErrorResponseException {
            ArrayList<String> unique_heis = new ArrayList<>(new HashSet<>(heis));
            super.checkHeis(unique_heis);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-hei-ids> known hei_ids, expect 400.");
  }

  @Test
  public void testWrongMaxHeiIdInManifestIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void checkHeis(List<String> heis) throws ErrorResponseException {
            if (heis.size() > max_hei_ids - 1) {
              throw new ErrorResponseException(
                  createErrorResponse(this.currentRequest, 400, "Exceeded max-hei-ids parameter")
              );
            }
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one known and one unknown hei_id, expect 200 and only one <hei-id> in response.");
  }

  @Test
  public void testReportsAnErrorWhenPassedUnknownParametersIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void extractParamsMultipleParams(Map<String, List<String>> params)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                createErrorResponse(this.currentRequest, 400, "Expected only hei_id parameters")
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with additional parameter, expect 200 and one hei_id response.");
  }

  @Test
  public void testNotReportingAnErrorWhenInvalidHttpMethodIsUsedIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void checkRequestMethod() throws ErrorResponseException {
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsWarning(
        "Trying Combination[-HTTT] with a PUT request. Expecting to receive a valid HTTP 405 error response.");
  }

  @Test
  public void testNotReportingAnErrorWhenNotPassingAnyParameterIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void extractParamsNoParams(Map<String, List<String>> params)
              throws ErrorResponseException {
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request without hei_ids, expect 400.");
  }

  @Test
  public void testReturningEmptyResponseWhenParametersOtherThanHeiIdArePassedIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void extractParamsNoHeiIds(Map<String, List<String>> params)
              throws ErrorResponseException {
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure("Request with single incorrect parameter, expect 400.");
  }

  @Test
  public void testReturningNonEmptyResponseForUnknownHeiIdIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void processNotCoveredHei(String hei, List<InstitutionsResponse.Hei> heis)
              throws ErrorResponseException {
            heis.add(createFakeHeiData(hei));
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request one unknown hei_id, expect 200 and empty response.");
  }

  @Test
  public void testReportingAnErrorWhenUnknownHeiIdIsPassedIsDetected() {
    InstitutionServiceV2Valid service =
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected void processNotCoveredHei(String hei, List<InstitutionsResponse.Hei> heis)
              throws ErrorResponseException {
            throw new ErrorResponseException(
                this.createErrorResponse(this.currentRequest, 400, "Unknown HEI ID encountered")
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
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
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
        new InstitutionServiceV2Valid(institutionsUrlHTTT, this.client,
            validatorKeyStoreSet.getMainKeyStore()) {
          @Override
          protected List<InstitutionsResponse.Hei> processHeis(List<String> heis)
              throws ErrorResponseException {
            return super.processHeis(heis.stream().distinct().collect(Collectors.toList()));
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Override
  protected ApiValidator<InstitutionsSuiteState> getValidator() {
    return validator;
  }
}

