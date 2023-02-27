package eu.erasmuswithoutpaper.registry.validators.mtprojectsvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;

public class MtProjectsValidatorTest extends AbstractApiTest<MtProjectsSuiteState> {
  private static final String mtProjectsUrl = "https://university.example.com/mt_projects";

  @Autowired
  protected MtProjectsValidator validator;

  @Override
  protected String getManifestFilename() {
    return "mtvalidator/manifest.xml";
  }

  @Override
  protected String getUrl() {
    return mtProjectsUrl;
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(1, 0, 0);
  }

  @Override
  protected ApiValidator<MtProjectsSuiteState> getValidator() {
    return validator;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotReportingAnErrorWhenNoPicIsProvidedIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void errorNoPic(RequestData requestData) throws ErrorResponseException {
        requestData.pic = coveredProjects.keySet().iterator().next();
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without pic, expect 400."
    );
  }

  @Test
  public void testNotReportingAnErrorWhenMultiplePicsAreProvidedIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void errorMultiplePics(RequestData requestData) throws ErrorResponseException {
        // ignore
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with correct pic twice, expect 400."
    );
  }

  @Test
  public void testNotReportingAnErrorWhenUnknownPicIsProvidedIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void errorInvalidPic(RequestData requestData) throws ErrorResponseException {
        // ignore
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with unknown pic parameter, expect 400."
    );
  }

  @Test
  public void testDefaultDateIsUsedWhenCallYearIsNotProvidedIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void errorNoCallYear(RequestData requestData) throws ErrorResponseException {
        requestData.callYear = 2019;
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without call_year, expect 400."
    );
  }

  @Test
  public void testDefaultDateIsUsedWhenCallYearIsInvalidIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected int errorInvalidCallYearFormat(
          RequestData requestData) throws ErrorResponseException {
        return 2019;
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with invalid value of call_year - not a number, expect 400."
    );
  }

  @Test
  public void testNotAcceptingCallYearIsZeroIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void handleCallYearZero(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Invalid call_year - zero")
        );
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with call_year equal zero, expect 200."
    );
  }

  @Test
  public void testNotAcceptingCallYearIsNegativeIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void handleCallYearNegative(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Invalid call_year - negative")
        );
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with negative call_year, expect 200."
    );
  }

  @Test
  public void testAcceptingAFullDateAsCallYearIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected int parseCallYear(RequestData requestData,
          String callYear) throws ErrorResponseException {
        try {
          return LocalDate.parse(callYear).getYear();
        } catch (DateTimeParseException | IllegalArgumentException e) {
          return super.parseCallYear(requestData, callYear);
        }
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with call_year being a date, expect 400."
    );
  }

  @Test
  public void testAcceptingDateTimeAsCallYearIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected int parseCallYear(RequestData requestData,
          String callYear) throws ErrorResponseException {
        try {
          DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
          return datatypeFactory.newXMLGregorianCalendar(callYear).getYear();
        } catch (DatatypeConfigurationException | IllegalArgumentException e) {
          return super.parseCallYear(requestData, callYear);
        }
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with call_year being a date and time, expect 400."
    );
  }

  @Test
  public void testNotReportingAnErrorWhenNoCallYearIsProvidedIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void errorNoCallYear(RequestData requestData) throws ErrorResponseException {
        requestData.callYear = 2019;
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without call_year, expect 400."
    );
  }

  @Test
  public void testNotReportingAnErrorWhenMultipleCallYearsAreProvidedIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void errorMultipleCallYears(RequestData requestData) throws ErrorResponseException {
        //ignore
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with correct call_year twice, expect 400."
    );
  }

  @Test
  public void testAcceptingOnlyFourDigitCallYearIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected int additionalCallYearCheck(RequestData requestData,
          Integer parsed) throws ErrorResponseException {
        if (1000 <= parsed && parsed <= 9999) {
          return parsed;
        }
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Invalid call_year")
        );
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with valid but strange value of call_year - less than 100, expect 200."
    ).containsFailure(
        "Request with valid but strange value of call_year - more than 1e6, expect 200."
    );
  }

  @Test
  public void testNotIgnoringUnknownParamsIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Unknown param")
        );
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known pic, call_year and invalid parameter, expect 200."
    );
  }

  @Test
  public void testNotReportingAnErrorWhenOnlyUnknownParametersAreProvidedIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void errorNoPic(RequestData requestData) throws ErrorResponseException {
        // ignore
      }

      @Override
      protected void errorNoCallYear(RequestData requestData) throws ErrorResponseException {
        // ignore
      }

      @Override
      protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
        if (requestData.callYear == null && requestData.pic == null) {
          throw new ErrorResponseException(createMtProjectsReponse(new ArrayList<>()));
        }
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with single incorrect parameter, expect 400."
    );
  }

  @Test
  public void testNotReportingAnErrorWhenNoParametersAreProvidedIsDetected() {
    MtProjectsV010Valid service = new MtProjectsV010Valid(mtProjectsUrl, this.client) {
      @Override
      protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(createMtProjectsReponse(new ArrayList<>()));
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without any parameter, expect 400."
    );
  }

}

