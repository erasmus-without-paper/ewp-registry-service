package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OUnitsSetupValidationSuite
    extends AbstractSetupValidationSuite<OUnitsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(OUnitsSetupValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  private static final String HEI_ID_PARAMETER = "hei_id";
  private static final String OUNIT_ID_PARAMETER = "ounit_id";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(HEI_ID_PARAMETER),
        new ValidationParameter(OUNIT_ID_PARAMETER, Arrays.asList(HEI_ID_PARAMETER))
    );
  }

  OUnitsSetupValidationSuite(ApiValidator<OUnitsSuiteState> validator,
      OUnitsSuiteState state,
      ValidationSuiteConfig config,
      int version) {
    super(validator, state, config, false);

    this.apiInfo = new OUnitsValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  private int getMaxOunitIds() {
    return getMaxIds("ounit-ids");
  }

  private int getMaxOunitCodes() {
    return getMaxIds("ounit-codes");
  }

  private HeiIdAndString findInstitutionThatCoversAnyOUnit(
      HttpSecurityDescription securityDescription,
      List<HeiIdAndUrl> heiIdAndInstitutionsUrls)
      throws SuiteBroken {
    return findResponseWithString(
        heiIdAndInstitutionsUrls,
        securityDescription,
        "/institutions-response/hei/ounit-id",
        "Use Institutions API to obtain list of OUnits for one of covered HEI IDs.",
        "We tried to find ounit-id to perform tests on, but Institutions API doesn't report any "
            + "OUnit for any of HEIs that we checked."
    );
  }

  private List<HeiIdAndUrl> getInstitutionsUrl(List<String> heiIds) throws SuiteBroken {
    return getApiUrlsForHeis(
        heiIds,
        "institutions",
        ApiEndpoint.NO_ENDPOINT,
        "Find Institutions API for any of covered HEIs.",
        "To perform tests we need any ounit-id. We have to use Institutions API for that, "
            + "but the Catalogue doesn't contain entries for this API for any of hei-ids that we "
            + "checked."
    );
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OUnitsSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxOunitIds = getMaxOunitIds();
    this.currentState.maxOunitCodes = getMaxOunitCodes();

    List<String> coveredHeiIds = new ArrayList<>();
    if (this.currentState.parameters.contains(HEI_ID_PARAMETER)) {
      coveredHeiIds.add(this.currentState.parameters.get(HEI_ID_PARAMETER));
    } else {
      coveredHeiIds = getCoveredHeiIds(this.currentState.url);
    }

    HeiIdAndString heiIdAndOunitId = new HeiIdAndString();
    if (this.currentState.parameters.contains(OUNIT_ID_PARAMETER)) {
      heiIdAndOunitId.heiId = this.currentState.parameters.get(HEI_ID_PARAMETER);
      heiIdAndOunitId.string = this.currentState.parameters.get(OUNIT_ID_PARAMETER);
    } else {
      List<HeiIdAndUrl> heiIdsAndUrls = getInstitutionsUrl(coveredHeiIds);
      heiIdAndOunitId = findInstitutionThatCoversAnyOUnit(securityDescription, heiIdsAndUrls);
    }

    this.currentState.selectedHeiId = heiIdAndOunitId.heiId;
    this.currentState.selectedOunitId = heiIdAndOunitId.string;
  }
}
