package eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator;


import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameterValue;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameters;
import eu.erasmuswithoutpaper.registry.validators.imobilitiesvalidator.get.IMobilitiesGetValidator;

import org.springframework.beans.factory.annotation.Autowired;

import https.github_com.erasmus_without_paper.ewp_specs_api_imobilities.blob.stable_v2.endpoints.get_response.StudentMobility;
import org.junit.jupiter.api.Test;

public class IMobilitiesGetValidatorV2Test extends AbstractApiTest<IMobilitiesSuiteState> {
  protected static final String getUrl = "https://university.example.com/imobilities/HTTT/get";
  @Autowired
  protected IMobilitiesGetValidator validator;

  @Override
  protected String getManifestFilename() {
    return "imobilities/manifest-v2.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(2, 0, 0);
  }

  protected ValidationParameters getValidationParameters() {
    return new ValidationParameters(List.of(new ValidationParameterValue("omobility_id", "sm2")));
  }

  @Override
  protected ApiValidator<IMobilitiesSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return getUrl;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    IMobilitiesServiceV2Valid service =
        new IMobilitiesServiceV2Valid(getUrl, this.client, serviceKeyStore);
    TestValidationReport report = this.getRawReport(service, getValidationParameters());
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidatingOMobilitiesIdListIsDetected() {
    IMobilitiesServiceV2Valid service =
        new IMobilitiesServiceV2Valid(getUrl, client, serviceKeyStore) {
          @Override
          protected void errorMaxOmobilityIdsExceeded(RequestData requestData) {
            // Do nothing
          }
        };
    TestValidationReport report = this.getRawReport(service, getValidationParameters());
    assertThat(report)
        .containsFailure("Request more than <max-omobility-ids> known omobility_ids, expect 400.");
  }

  @Test
  public void testNotReportingMissingOMobilityIdParameterAsAnErrorIsDetected() {
    IMobilitiesServiceV2Valid service =
        new IMobilitiesServiceV2Valid(getUrl, client, serviceKeyStore) {
          @Override
          protected void handleNoOMobilityId(RequestData requestData)
              throws ErrorResponseException {
            throw new ErrorResponseException(createIMobilitiesGetResponse(new ArrayList<>()));
          }
        };
    TestValidationReport report = this.getRawReport(service, getValidationParameters());
    assertThat(report).containsFailure("Request without parameters, expect 400.");
  }

  @Test
  public void testReturningWrongOmobilityIsDetected() {
    IMobilitiesServiceV2Valid service =
        new IMobilitiesServiceV2Valid(getUrl, client, serviceKeyStore) {
          @Override
          protected StudentMobility processCoveredOMobilityId(RequestData requestData,
              String receivingHeiId, String omobilityId) {
            Optional<String> anotherOmobilityId = this.mobilitiesCoveredByHeiIds.get(receivingHeiId)
                .keySet().stream().filter(s -> !s.equals(omobilityId)).findFirst();
              return anotherOmobilityId.map(s -> this.mobilitiesCoveredByHeiIds.get(receivingHeiId)
                  .get(s).mobility).orElse(null);
          }
        };
    TestValidationReport report = this.getRawReport(service, getValidationParameters());
    assertThat(report).containsFailure(
        "Request one known and one unknown omobility_id, expect 200 and only one omobility in"
            + " response");
  }

  @Test
  public void testReturningDataForUnknownOMobilityIdIsDetected() {
    IMobilitiesServiceV2Valid service =
        new IMobilitiesServiceV2Valid(getUrl, client, serviceKeyStore) {
          @Override
          protected StudentMobility processNotCoveredOMobilityId(RequestData requestData,
              String receivingHeiId, String omobilityId) {
            return this.mobilitiesCoveredByHeiIds.get(receivingHeiId).values().iterator()
                .next().mobility;
          }
        };
    TestValidationReport report = this.getRawReport(service, getValidationParameters());
    assertThat(report)
        .containsFailure("Request one unknown omobility_id, expect 200 and empty response.");
    assertThat(report).containsFailure(
        "Request one known and one unknown omobility_id, expect 200 and only one omobility in "
            + "response.");
  }

  @Test
  public void testTooLargeMaxIiaIdsInManifestIsDetected() {
    IMobilitiesServiceV2Valid service =
        new IMobilitiesServiceV2Valid(getUrl, client, serviceKeyStore) {
          @Override
          protected int getMaxOMobilityIds() {
            return super.getMaxOMobilityIds() - 1;
          }
        };
    TestValidationReport report = this.getRawReport(service, getValidationParameters());
    assertThat(report).containsFailure("Request exactly <max-omobility-ids> known omobility_ids, "
        + "expect 200 and non-empty response.");
  }

  @Test
  public void testSendingDataThatCallerHasNoAccessToIsDetected2() {
    IMobilitiesServiceV2Valid service =
        new IMobilitiesServiceV2Valid(getUrl, client, serviceKeyStore) {
          @Override
          protected boolean isCalledPermittedToSeeHeiData(String omobilityId,
              RequestData requestData) {
            return true;
          }
        };
    TestValidationReport report = this.getRawReport(service, getValidationParameters());
    assertThat(report).containsWarning(
        "Request omobility_id as other EWP participant, " + "expect 200 OK and empty response.");
  }
}
