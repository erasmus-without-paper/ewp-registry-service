package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;


import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaGetValidator;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;

import org.springframework.beans.factory.annotation.Autowired;

import https.github_com.erasmus_without_paper.ewp_specs_api_iias.blob.stable_v2.endpoints.get_response.IiasGetResponse;
import org.junit.jupiter.api.Test;

public class IiaGetValidatorV2Test extends IiaValidatorTestBase {
  @Autowired
  protected IiaGetValidator validator;

  @Override
  protected ApiValidator<IiaSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return iiaGetUrl;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidatingIiaIdListIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorMaxIdsExceeded(RequestData requestData)
          throws ErrorResponseException {
        //Do nothing
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-iia-ids> known iia_ids, expect 400.");
  }

  @Test
  public void testNotValidatingIiaCodeListIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorMaxCodesExceeded(RequestData requestData)
          throws ErrorResponseException {
        //Do nothing
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-iia-codes> known iia_codes, expect 400.");
  }

  @Test
  public void testNotReportingMissingRequiredParametersAsAnErrorIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorNoParams(RequestData requestData)
          throws ErrorResponseException {
        throw new ErrorResponseException(createIiasGetResponse(new ArrayList<>()));
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without hei_id and iia_ids, expect 400.");
  }

  @Test
  public void testNotReportingMissingHeiIdParameterAsAnErrorIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorNoHeiId(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIiasGetResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without hei_id, expect 400.");
  }

  @Test
  public void testNotReportingMissingIiaIdAndIiaCodeAsAnErrorIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIiasGetResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without iia_ids and iia_codes, expect 400.");
  }

  @Test
  public void testIgnoringAdditionalHeiIdsIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorMultipleHeiIds(RequestData requestData) throws ErrorResponseException {
        //Ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct hei_id twice, expect 400.");
  }

  @Test
  public void testHandlingBothIdsAndCodesIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
        //Ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct iia_id and correct iia_code, expect 400.");
  }

  @Test
  public void testIgnoringCodesWhenIdsAndCodesArePassedIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
        requestData.iiaCodes.clear();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct iia_id and correct iia_code, expect 400.");
  }

  @Test
  public void testIgnoringIdsWhenIdsAndCodesArePassedIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorIdsAndCodes(RequestData requestData) throws ErrorResponseException {
        requestData.iiaIds.clear();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with correct iia_id and correct iia_code, expect 400.");
  }

  @Test
  public void testReturningCorrectDataWhenNoHeiIdIsPassedIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorNoHeiId(RequestData requestData) throws ErrorResponseException {
        requestData.heiId = this.coveredHeiIds.get(0);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without hei_id, expect 400.");
  }

  @Test
  public void testReturningCorrectDataWhenNeitherIiaIdNorIiaCodeIsPassedIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorNoIdsNorCodes(RequestData requestData) throws ErrorResponseException {
        String id = this.iias.get(0).getPartner().get(0).getIiaId();
        requestData.iiaIds = Arrays.asList(id);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request without iia_ids and iia_codes, expect 400.");
  }

  @Test
  public void testNotReportingErrorWhenUnknownHeiIdIsPassedIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createIiasGetResponse(new ArrayList<>())
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request for one of known iia_ids with unknown hei_id, expect 400.");
  }

  @Test
  public void testNonEmptyResponseWhenUnknownHeiIdIsPassedIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
        requestData.heiId = this.coveredHeiIds.get(0);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request for one of known iia_ids with unknown hei_id, expect 400.");
  }

  @Test
  public void testReturningWrongIiaIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected String handleKnownIiaId(String iiaId) {
        return iias.stream().filter(
            i -> !i.getPartner().get(0).getIiaId().equals(iiaId)).findFirst().get().getPartner()
            .get(0).getIiaId();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request for one of known iia_ids, expect 200 OK.");
  }

  @Test
  public void testReturningDataForUnknownIiaIdIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected String handleUnknownIiaId(String iiaId,
          List<IiasGetResponse.Iia> selectedIias) {
        return selectedIias.get(0).getPartner().get(0).getIiaId();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one unknown iia_id, expect 200 and empty response.");
    assertThat(report).containsFailure(
        "Request one known and one unknown iia_id, expect 200 and only one iia in response.");
  }

  @Test
  public void testReturningDataForUnknownIiaCodeIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected String handleUnknownIiaCode(String iiaCode,
          List<IiasGetResponse.Iia> selectedIias) {
        return selectedIias.get(0).getPartner().get(0).getIiaCode();
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request one known and one unknown iia_code, expect 200 and only one iia in response.");
  }

  @Test
  public void testReturningSingleOunitForMultipleEqualOunitIdsIsAccepted() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected List<IiasGetResponse.Iia> filterIiasByCode(List<IiasGetResponse.Iia> selectedIias,
          RequestData requestData) {
        return new ArrayList<>(new HashSet<>(super.filterIiasByCode(selectedIias, requestData)));
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testTooLargeMaxIiaIdsInManifestIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected int getMaxIiaIds() {
        return super.getMaxIiaIds() - 1;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request exactly <max-iia-ids> known iia_ids, "
            + "expect 200 and non-empty response.");
  }

  @Test
  public void testTooLargeMaxIiaCodesInManifestIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected int getMaxIiaCodes() {
        return super.getMaxIiaCodes() - 1;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request exactly <max-iia-codes> known iia_codes, "
            + "expect 200 and non-empty response.");
  }
}
