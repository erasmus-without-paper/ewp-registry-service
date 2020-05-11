package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaIndexValidator;
import eu.erasmuswithoutpaper.registry.validators.types.IiasGetResponseV2;
import org.springframework.beans.factory.annotation.Autowired;

import org.junit.Test;

public class IiaIndexValidatorV2Test extends IiaValidatorTestBase {
  @Autowired
  protected IiaIndexValidator validator;

  @Override
  protected ApiValidator<IiaSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return iiaIndexUrl;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }

  @Test
  public void testIgnoringAdditionalHeiIdIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorMultipleHeiIds(RequestData requestData) throws ErrorResponseException {
        requestData.heiId = this.coveredHeiIds.get(0);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with known hei_id and unknown hei_id, expect 400.");
  }

  @Test
  public void testUsingCoveredHeiIdInsteadOfUnknownHeiIdIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorUnknownHeiId(RequestData requestData) throws ErrorResponseException {
        requestData.heiId = this.coveredHeiIds.get(0);
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report)
        .containsFailure("Request with unknown hei_id, expect 400.");
  }

  /*
  @Test
  public void testNotAcceptingSouthernHemisphereAcademicYearIdFormatIsDetected() {
    IiasServiceV2Valid service = new IiasServiceV2Valid(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected boolean checkReceivingAcademicYearId(String receivingAcademicYear) {
        String receivingAcademicYearPattern = "([0-9]{4})/([0-9]{4})";
        Matcher matcher = Pattern.compile(receivingAcademicYearPattern)
            .matcher(receivingAcademicYear);
        if (!matcher.matches()) {
          return false;
        }
        return Integer.valueOf(matcher.group(1)) + 1 == Integer.valueOf(matcher.group(2));
      }
    };
    TestValidationReport report = this.getRawReport(service);

    assertThat(report).containsFailure(
        "Request with known hei_id and receiving_academic_year_id in southern hemisphere format, "
            + "expect 200 OK.");
  }
  */

  @Test
  public void testNotCheckingAcademicYearIdIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected boolean checkReceivingAcademicYearId(String receivingAcademicYear) {
        return receivingAcademicYear.length() == 9;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with receiving_academic_year_id in incorrect format, expect 400.");
  }

  @Test
  public void testAcceptingEqualHeiIdAndPartnerHeiIdIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorHeiIdsEqual(RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(createIiasIndexResponse(new ArrayList<>()));
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known hei_id equal to partner_hei_id, expect 400.");
  }

  @Test
  public void testCheckingPartnerHeiIdIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorPartnerHeiIdUnknown(
          RequestData requestData) throws ErrorResponseException {
        throw new ErrorResponseException(
            createErrorResponse(requestData.request, 400, "partner_hei_id unknown")
        );
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known hei_id and unknown partner_hei_id, expect 200 OK and empty list.");
  }

  @Test
  public void testIgnoringMultipleModifiedSinceIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected void errorMultipleModifiedSince(
          RequestData requestData) throws ErrorResponseException {
        //Ignore
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with multiple modified_since parameters, expect 400.");
  }

  @Test
  public void testReturnsEmptyResponseWhenPartnerIdIsValidIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected boolean filterPartnerHeiId(IiasGetResponseV2.Iia.Partner partner, String hei_id) {
        return false;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request known hei_id and known partner_hei_id, expect 200 OK and non-empty response.");
  }

  @Test
  public void testReturnsEmptyResponseWhenReceivingAcademicYearIdIsUsed() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected boolean filterAcademicYear(String academicYear,
          List<String> requestedAcademicYears) {
        return false;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known hei_id and known receiving_academic_year_id parameter, "
            + "expect 200 OK and non-empty response.");
  }

  @Test
  public void testNotValidatingReceivingAcademicYearIdIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected boolean filterAcademicYear(String academicYear,
          List<String> requestedAcademicYears) {
        return true;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known hei_id and unknown receiving_academic_year_id parameter, "
            + "expect 200 OK and empty response.");
  }

  @Test
  public void testNotUsingModifiedSinceIsDetected() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected List<IiasGetResponseV2.Iia> filterIiasByModifiedSince(
          List<IiasGetResponseV2.Iia> selectedIias, RequestData requestData) {
        return selectedIias;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsWarning(
        "Request with known hei_id and modified_since in the future, expect 200 OK "
            + "and empty response");
  }

  @Test
  public void testReturnsEmptyResponseWhenModifiedSinceIsUsed() {
    IiasServiceValidV2 service = new IiasServiceValidV2(iiaIndexUrl, iiaGetUrl, this.client) {
      @Override
      protected List<IiasGetResponseV2.Iia> filterIiasByModifiedSince(
          List<IiasGetResponseV2.Iia> selectedIias, RequestData requestData) {
        if (requestData.modifiedSince != null) {
          return new ArrayList<>();
        }
        return selectedIias;
      }
    };
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).containsFailure(
        "Request with known hei_id and modified_since far in the past, expect 200 OK "
            + "and non-empty response.");
  }
}

