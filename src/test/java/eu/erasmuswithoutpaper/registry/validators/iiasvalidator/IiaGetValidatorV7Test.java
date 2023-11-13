package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;

import org.junit.jupiter.api.Test;

public class IiaGetValidatorV7Test extends IiaGetValidatorV6Test {
  @Override
  protected String getManifestFilename() {
    return "iiasvalidator/manifest-v7.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(7, 0, 0);
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    IiasServiceValidV7 service = new IiasServiceValidV7(iiaIndexUrl, iiaGetUrl, this.client);
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }
}
