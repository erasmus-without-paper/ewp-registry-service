package eu.erasmuswithoutpaper.registry.validators.iiasvalidator;

import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.FakeInternetService;
import eu.erasmuswithoutpaper.registry.validators.AbstractApiTest;
import eu.erasmuswithoutpaper.registry.validators.CombEntry;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.TestValidationReport;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;

public abstract class IiaValidatorTestBase extends AbstractApiTest<IiaSuiteState> {
  protected static final String iiaIndexUrl = "https://university.example.com/iias/HTTT/index";
  protected static final String iiaGetUrl = "https://university.example.com/iias/HTTT/get";

  @Override
  protected String getManifestFilename() {
    return "iiasvalidator/manifest.xml";
  }

  protected TestValidationReport getRawReport(FakeInternetService service) {
    String url = getUrl();
    SemanticVersion semanticVersion = getVersion();
    HttpSecurityDescription security = getSecurity();
    this.internet.addFakeInternetService(service);
    List<ValidationStepWithStatus> results =
        getValidationReportSteps(url, semanticVersion, security);
    this.internet.removeFakeInternetService(service);
    this.internet.clearAll();
    return new TestValidationReport(results);
  }

  protected abstract String getUrl();

  @Override
  protected SemanticVersion getVersion() {
    return new SemanticVersion(2, 0, 0);
  }

  @Override
  protected HttpSecurityDescription getSecurity() {
    return new HttpSecurityDescription(
        CombEntry.CLIAUTH_HTTPSIG,
        CombEntry.SRVAUTH_TLSCERT,
        CombEntry.REQENCR_TLS,
        CombEntry.RESENCR_TLS
    );
  }
}

