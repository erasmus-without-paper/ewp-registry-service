package eu.erasmuswithoutpaper.registry.validators.mtinstitutionsvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.types.MtInstitutionsResponse;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;

public class MtInstitutionsValidatorTest extends AbstractApiTest {
  private static final String mtInstitutionsUrl = "https://university.example.com/mt_institutions";

  @Autowired
  protected MtInstitutionsValidator validator;

  @Override
  protected String getManifestFilename() {
    return "mtvalidator/manifest.xml";
  }

  @Override
  protected String getUrl() {
    return mtInstitutionsUrl;
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(0, 1, 0);
  }

  @Override
  protected ApiValidator GetValidator() {
    return validator;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    MtInstitutionsV010Valid service = new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotValidationLengthOfPicListIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          @Override
          protected void ErrorMaxPicsExceeded(
              RequestData requestData) throws ErrorResponseException {
            //Do nothing
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request more than <max-ids> known PICs, expect 400.")
        .containsFailure("Request more than <max-ids> unknown PICs, expect 400.");
  }

  @Test
  public void testReturningWrongPicIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          @Override
          protected List<MtInstitutionsResponse.Hei> ProcessPics(
              RequestData requestData) throws ErrorResponseException {
            List<MtInstitutionsResponse.Hei> result = super.ProcessPics(requestData);
            MtInstitutionsResponse.Hei replaced = new MtInstitutionsResponse.Hei();
            replaced.setMailingAddress(result.get(0).getMailingAddress());
            replaced.setErasmusCharter(result.get(0).getErasmusCharter());
            replaced.setErasmus(result.get(0).getErasmus());
            replaced.setPic("other-pic");
            result.set(0, replaced);
            return result;
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known pic and eche_at_date, expect 200 and non empty response.");
  }

  @Test
  public void testReportingAnErrorWhenPassedUnknownParameterIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          @Override
          protected void HandleUnexpectedParams(
              RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createErrorResponse(requestData.request, 400, "Unknown param")
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known pic, eche_at_date and invalid parameter, expect 200.");
  }

  @Test
  public void testCollapsingEqualPicsIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          @Override
          protected List<MtInstitutionsResponse.Hei> ProcessPics(
              RequestData requestData) throws ErrorResponseException {
            requestData.pic = requestData.pic.stream().distinct().collect(Collectors.toList());
            return super.ProcessPics(requestData);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with correct pic twice, expect 200 and two elements in response.");
  }

  @Test
  public void testNotReportingAnErrorWhenOnlyIncorrectParametersAreProvidedIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          @Override
          protected void ErrorNoPic(RequestData requestData) throws ErrorResponseException {
            // ignore
          }

          @Override
          protected void ErrorNoEcheAtDate(RequestData requestData) throws ErrorResponseException {
            requestData.echeAtDate = coveredPics.get(0).getErasmusCharter().getStartDate();
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with single incorrect parameter, expect 400.");
  }


  @Test
  public void testNotIgnoringUnknownPicParametersIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          @Override
          protected List<MtInstitutionsResponse.Hei> ProcessPics(
              RequestData requestData) throws ErrorResponseException {
            requestData.pic = Collections
                .nCopies(requestData.pic.size(), coveredPics.get(0).getPic());
            return super.ProcessPics(requestData);
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with unknown pic parameter, expect 200 and empty response.")
        .containsFailure(
            "Request one known and one unknown pic, expect 200 and only one pic in response.");
  }

  @Test
  public void testNotReportingAnErrorWhenNoParametersAreProvidedIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          @Override
          protected void ErrorNoParams(RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createMtInstitutionsReponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without any parameter, expect 400.");
  }

  @Test
  public void testNotValidatingFormatOfEcheAtDateIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          @Override
          protected XMLGregorianCalendar ErrorDateFormat(RequestData requestData)
              throws ErrorResponseException {
            try {
              return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(2019, 1, 1, 0);
            } catch (DatatypeConfigurationException e) {
              return null;
            }
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with invalid value of eche_at_date, expect 400.");
  }

  @Test
  public void testAcceptingEcheAtDateAsDateAndTimeIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          @Override
          protected XMLGregorianCalendar CheckDateFormat(RequestData requestData, String date)
              throws ErrorResponseException {
            DatatypeFactory factoryInstance = null;
            try {
              factoryInstance = DatatypeFactory.newInstance();
            } catch (DatatypeConfigurationException e) {
              return null;
            }
            try {
              return factoryInstance.newXMLGregorianCalendar(date);
            } catch (IllegalArgumentException e) {
              return factoryInstance.newXMLGregorianCalendarDate(2019, 1, 1, 0);
            }
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with eche_at_date being a date in wrong format, expect 400.");
  }

  @Test
  public void notReportingAnErrorWhenNoPicIsProvidedIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          protected void ErrorNoPic(RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createMtInstitutionsReponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without pic, expect 400.");
  }

  @Test
  public void notReportingAnErrorWhenNoEcheAtDateIsProvidedIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          protected void ErrorNoEcheAtDate(RequestData requestData) throws ErrorResponseException {
            throw new ErrorResponseException(
                createMtInstitutionsReponse(new ArrayList<>())
            );
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without eche_at_date, expect 400.");
  }

  @Test
  public void testTooLargeMaxIdsProvidedInManifestIsDetected() {
    MtInstitutionsV010Valid service =
        new MtInstitutionsV010Valid(mtInstitutionsUrl, this.client) {
          protected int getMaxIds() {
            return super.getMaxIds() - 1;
          }
        };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request exactly <max-ids> known PICs, expect 200 and <max-ids> PICs in response.");
  }
}

