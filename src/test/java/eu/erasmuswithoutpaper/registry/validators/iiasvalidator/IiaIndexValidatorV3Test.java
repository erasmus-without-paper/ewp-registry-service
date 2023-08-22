package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaIndexValidator;

import org.springframework.beans.factory.annotation.Autowired;
import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;
import org.junit.jupiter.api.Test;

public class IiaIndexValidatorV3Test extends IiaValidatorTestBase {
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

  @Override
  protected String getManifestFilename() {
    return "iiasvalidator/manifest-v3.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(3, 0, 0);
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    IiasServiceValidV3 service = new IiasServiceValidV3(iiaIndexUrl, iiaGetUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }
}

