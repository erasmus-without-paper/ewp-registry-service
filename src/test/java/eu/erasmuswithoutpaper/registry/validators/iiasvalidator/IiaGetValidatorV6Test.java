package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaGetValidator;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

public class IiaGetValidatorV6Test extends IiaValidatorTestBase {
  @Autowired
  protected IiaGetValidator validator;

  @Override
  protected String getManifestFilename() {
    return "iiasvalidator/manifest-v6.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(6, 0, 0);
  }

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
    IiasServiceValidV6 service = new IiasServiceValidV6(iiaIndexUrl, iiaGetUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }
}
