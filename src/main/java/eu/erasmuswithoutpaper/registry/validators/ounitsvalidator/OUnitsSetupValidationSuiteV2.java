package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

class OUnitsSetupValidationSuiteV2
    extends AbstractSetupValidationSuite<OUnitsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(OUnitsSetupValidationSuiteV2.class);
  private final List<String> heis = new ArrayList<>();
  private final List<String> institutionsUrls = new ArrayList<>();

  OUnitsSetupValidationSuiteV2(ApiValidator<OUnitsSuiteState> validator,
      OUnitsSuiteState state,
      ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  private int getMaxOunitIds() {
    return getMaxIds("ounit-ids");
  }

  private int getMaxOunitCodes() {
    return getMaxIds("ounit-codes");
  }

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
              Status.NOTICE, null
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

  private void findInstitutionThatCoversAnyOunit(HttpSecurityDescription securityDescription)
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

          Request request = createRequestWithParameters(
              this,
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
              Status.NOTICE, null
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
    findInstitutionThatCoversAnyOunit(securityDescription);
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
  public String getApiPrefix() {
    return "ou2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "our2";
  }
}
