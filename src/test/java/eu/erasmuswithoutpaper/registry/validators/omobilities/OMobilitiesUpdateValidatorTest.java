package eu.erasmuswithoutpaper.registry.validators.omobilities;

import static eu.erasmuswithoutpaper.registry.validators.TestValidationReportAsset.assertThat;

import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.update.OMobilitiesUpdateValidator;

import org.springframework.beans.factory.annotation.Autowired;

import org.junit.jupiter.api.Test;

public class OMobilitiesUpdateValidatorTest extends OMobilitiesValidatorTestBase {
  @Autowired
  protected OMobilitiesUpdateValidator validator;

  @Override
  protected ApiValidator<OMobilitiesSuiteState> getValidator() {
    return validator;
  }

  @Override
  protected String getUrl() {
    return omobilitiesUpdateUrl;
  }

  @Override
  protected String getManifestFilename() {
    return "omobilities/manifest-v3.xml";
  }

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(3, 0, 0);
  }

  @Test
  public void testValidationOnValidServiceIsSuccessful() {
    OMobilitiesServiceV3Valid service =
        new OMobilitiesServiceV3Valid(omobilitiesIndexUrl, omobilitiesGetUrl, omobilitiesUpdateUrl,
            this.client, this.serviceKeyStore.getCoveredHeiIDs().get(0));
    TestValidationReport report = this.getRawReport(service);
    assertThat(report).isCorrect();
  }
}
