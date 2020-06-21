package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator.update.OMobilityLAsUpdateValidator;

import org.springframework.beans.factory.annotation.Autowired;
import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;
import org.junit.Test;

public class OMobilityLAsUpdateValidatorTest extends OMobilityLAsValidatorTestBase {
  @Autowired
  protected OMobilityLAsUpdateValidator validator;

  @Override
  protected ApiValidator<OMobilityLAsSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return omobilitylasUpdateUrl;
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    OMobilityLAsServiceV030Valid service = new OMobilityLAsServiceV030Valid(
        omobilitylasIndexUrl, omobilitylasGetUrl, omobilitylasUpdateUrl, this.client,
        this.serviceKeyStore.getCoveredHeiIDs().get(0));
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
    assertThat(report).containsSkipped(
        "Send update-components-studied-v1 request, which is unsupported, expect 400."
    );
    assertThat(report).containsSkipped(
        "Send approve-components-studied-proposal-v1 request, which is unsupported, expect 400."
    );
  }
}

