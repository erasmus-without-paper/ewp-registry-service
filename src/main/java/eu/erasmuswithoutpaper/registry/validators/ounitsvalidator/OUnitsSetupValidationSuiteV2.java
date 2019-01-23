package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.CombEntry;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.echovalidator.HttpSecuritySettings;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

class OUnitsSetupValidationSuiteV2
    extends AbstractSetupValidationSuite<OUnitsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(OUnitsSetupValidationSuiteV2.class);

  OUnitsSetupValidationSuiteV2(ApiValidator<OUnitsSuiteState> validator,
      EwpDocBuilder docBuilder, Internet internet, RegistryClient regClient,
      ManifestRepository repo, OUnitsSuiteState state) {
    super(validator, docBuilder, internet, regClient, repo, state);
  }

  private int getMaxOunitIds() {
    return getMaxIds("ounit-ids");
  }

  private int getMaxOunitCodes() {
    return getMaxIds("ounit-codes");
  }


  protected static HttpSecurityDescription getDescriptionFromSecuritySettings(
      HttpSecuritySettings securitySettings) {
    CombEntry cliauth = CombEntry.CLIAUTH_NONE;
    if (securitySettings.supportsCliAuthTlsCertSelfSigned()) {
      cliauth = CombEntry.CLIAUTH_TLSCERT_SELFSIGNED;
    } else if (securitySettings.supportsCliAuthHttpSig()) {
      cliauth = CombEntry.CLIAUTH_HTTPSIG;
    } else if (securitySettings.supportsCliAuthNone()) {
      cliauth = CombEntry.CLIAUTH_NONE;
    } else if (securitySettings.supportsCliAuthTlsCert()) {
      //TODO
      throw new NotImplementedException();
    }

    CombEntry srvauth = CombEntry.SRVAUTH_TLSCERT;
    if (securitySettings.supportsSrvAuthTlsCert()) {
      srvauth = CombEntry.SRVAUTH_TLSCERT;
    } else if (securitySettings.supportsSrvAuthHttpSig()) {
      srvauth = CombEntry.SRVAUTH_HTTPSIG;
    }

    CombEntry reqencr = CombEntry.REQENCR_TLS;
    if (securitySettings.supportsReqEncrTls()) {
      reqencr = CombEntry.REQENCR_TLS;
    } else if (securitySettings.supportsReqEncrEwp()) {
      reqencr = CombEntry.REQENCR_EWP;
    }

    CombEntry resencr = CombEntry.RESENCR_TLS;
    if (securitySettings.supportsResEncrTls()) {
      resencr = CombEntry.RESENCR_TLS;
    } else if (securitySettings.supportsResEncrEwp()) {
      resencr = CombEntry.RESENCR_EWP;
    }

    return new HttpSecurityDescription(cliauth, srvauth, reqencr, resencr);
  }

  private final List<String> heis = new ArrayList<>();
  private final List<String> institutionsUrls = new ArrayList<>();

  private void getCoveredHeiIds() throws SuiteBroken {
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Get hei-ids covered by host managing this url.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        String url = OUnitsSetupValidationSuiteV2.this.currentState.url;
        List<String> coveredHeiIds =
            OUnitsSetupValidationSuiteV2.this.fetchHeiIdsCoveredByApiByUrl(url);
        if (coveredHeiIds.isEmpty()) {
          throw new Failure(
              "Catalogue doesn't contain any hei-ids covered by this url. We cannot preform tests.",
              Status.FAILURE, null
          );
        }

        heis.addAll(coveredHeiIds);
        for (String hei : coveredHeiIds) {
          List<String> urls =
              OUnitsSetupValidationSuiteV2.this.getApiUrlForHei("institutions", hei);
          if (!urls.isEmpty()) {
            institutionsUrls.add(urls.get(0));
          } else {
            institutionsUrls.add(null);
          }
        }
        return Optional.empty();
      }
    });
  }

  private HttpSecurityDescription getSecurityDescriptionFromApiEntry(Element apiEntry,
      HttpSecurityDescription preferedSecurityDesc) {
    Element httpSecurityElem = $(apiEntry).xpath("*[local-name()='http-security']").get(0);
    HttpSecuritySettings securitySettings = new HttpSecuritySettings(httpSecurityElem);

    // We are going to connect to institutions API, try to use the same security method,
    // but if it is not compatible select one of available.
    HttpSecurityDescription securityDescription = preferedSecurityDesc ;
    if (preferedSecurityDesc == null || !preferedSecurityDesc .isCompatible(securitySettings)) {
      securityDescription = getDescriptionFromSecuritySettings(securitySettings);
    }
    return securityDescription;
  }

  private void findInstitutionThatCoveresAnyOunit(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.setup(new InlineValidationStep() {
      @Override
      public String getName() {
        return "Use institutions API to obtain list of OUnits for one of covered HEI IDs.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        // Iterate through every (institutions url, hei id) pair we obtained earlier.
        // Find first pair such that call to `institution url` with `hei id` as a parameter
        // returns non-empty list of OUnit Ids.
        // Tests will be performed on that list of OUnit IDs.
        for (int i = 0; i < institutionsUrls.size(); i++) {
          final String url = institutionsUrls.get(i);
          final String heiId = heis.get(i);
          if (url == null) {
            continue;
          }

          Element apiEntry = OUnitsSetupValidationSuiteV2.this.getApiEntryFromUrl(url);
          if (apiEntry == null) {
            continue;
          }

          Request request = createRequestWithParameters(this,
              new Combination(
                  "GET", url, apiEntry,
                  getSecurityDescriptionFromApiEntry(apiEntry, securityDescription)
              ),
              Arrays.asList(new Parameter("hei_id", heiId))
          );
          Response response = null;
          try {
            response = OUnitsSetupValidationSuiteV2.this.internet.makeRequest(request);
            expect200(response);
          } catch (IOException | Failure e) {
            continue;
          }
          List<String> coveredOunits = selectFromDocument(
              makeXmlFromBytes(response.getBody()),
              "/institutions-response/hei/ounit-id"
          );
          if (!coveredOunits.isEmpty()) {
            //Found not empty
            OUnitsSetupValidationSuiteV2.this.currentState.ounitIds.addAll(coveredOunits);
            OUnitsSetupValidationSuiteV2.this.currentState.selectedHeiId = heiId;
            break;
          }
        }

        if (OUnitsSetupValidationSuiteV2.this.currentState.ounitIds.isEmpty()) {
          throw new Failure(
              "Cannot fetch any ounits.",
              Status.FAILURE, null
          );
        }

        return Optional.empty();
      }
    });
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OUnitsSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxOunitIds = getMaxOunitIds();
    this.currentState.maxOunitCodes = getMaxOunitCodes();
    this.currentState.ounitIds = new ArrayList<>();

    getCoveredHeiIds();
    findInstitutionThatCoveresAnyOunit(securityDescription);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected KnownElement getKnownElement() {
    return KnownElement.RESPONSE_OUNITS_V2;
  }

  @Override
  protected String getApiNamespace() {
    return KnownNamespace.APIENTRY_OUNITS_V2.getNamespaceUri();
  }

  @Override
  protected String getApiName() {
    return "organizational-units";
  }

  @Override
  protected String getApiVersion() {
    return "2.0.0";
  }

  @Override
  public String getApiPrefix() {
    return "ou2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "our2";
  }
}
