package eu.erasmuswithoutpaper.registry.validators.mtdictionariesvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;

public class MtDictionariesValidatorTest extends AbstractApiTest<MtDictionariesSuiteState> {
  private static final String mtDictionariesUrl = "https://university.example.com/mt_dictionaries";

  @Autowired
  protected MtDictionariesValidator validator;

  @Override
  protected String getManifestFilename() {
    return "mtvalidator/manifest.xml";
  }

  @Override
  protected String getUrl() {
    return mtDictionariesUrl;
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(1, 0, 0);
  }

  @Override
  protected ApiValidator<MtDictionariesSuiteState> getValidator() {
    return validator;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testNotReportingAnErrorWhenNoDictionaryIsProvidedIsDetected() {
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
      @Override
      protected void errorNoDictionary(RequestData requestData) throws ErrorResponseException {
        requestData.dictionary = coveredTerms.keySet().iterator().next().dictionary;
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without dictionary, expect 400."
    );
  }

  @Test
  public void testNotReportingAnErrorWhenMultipleDictionariesAreProvidedIsDetected() {
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
      @Override
      protected void errorMultipleDictionaries(
          RequestData requestData) throws ErrorResponseException {
        // ignore
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with correct dictionary twice, expect 400."
    );
  }

  @Test
  public void testNotReportingAnErrorWhenUnknownDictionaryIsProvidedIsDetected() {
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
      @Override
      protected void errorInvalidDictionary(RequestData requestData) throws ErrorResponseException {
        // ignore
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with unknown dictionary parameter, expect 400."
    );
  }

  @Test
  public void testDefaultDateIsUsedWhenCallYearIsNotProvidedIsDetected() {
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
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
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
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
  public void testDefaultDateIsUsedWhenCallYearIsZeroIsDetected() {
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
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
  public void testDefaultDateIsUsedWhenCallYearIsNegativeIsDetected() {
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
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
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
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
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
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
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
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
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
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
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
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
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
      @Override
      protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "Unknown param")
        );
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known dictionary, call_year and invalid parameter, expect 200."
    );
  }

  @Test
  public void testNotReportingAnErrorWhenOnlyUnknownParametersAreProvidedIsDetected() {
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
      @Override
      protected void errorNoDictionary(RequestData requestData) throws ErrorResponseException {
        // ignore
      }

      @Override
      protected void errorNoCallYear(RequestData requestData) throws ErrorResponseException {
        // ignore
      }

      @Override
      protected void handleUnexpectedParams(RequestData requestData) throws ErrorResponseException {
        if (requestData.callYear == null && requestData.dictionary == null) {
          throw new ErrorResponseException(createMtDictionariesReponse(new ArrayList<>()));
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
    MtDictionariesV010Valid service = new MtDictionariesV010Valid(mtDictionariesUrl, this.client) {
      @Override
      protected void errorNoParams(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(createMtDictionariesReponse(new ArrayList<>()));
      }
    };

    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request without any parameter, expect 400."
    );
  }

}

