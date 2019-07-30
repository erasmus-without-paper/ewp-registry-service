package eu.erasmuswithoutpaper.registry.validators.iiavalidator.get;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IiaGetSetupValidationSuiteV2
    extends AbstractSetupValidationSuite<IiaSuiteState> {

  private static final Logger logger = LoggerFactory.getLogger(IiaGetSetupValidationSuiteV2.class);
  private static final ValidatedApiInfo apiInfo = new IiaGetValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }


  IiaGetSetupValidationSuiteV2(ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state,
      ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  protected static final String HEI_ID_PARAMETER = "hei_id";
  private static final String IIA_ID_PARAMETER = "iia_id";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(HEI_ID_PARAMETER),
        new ValidationParameter(IIA_ID_PARAMETER, Collections.singletonList(HEI_ID_PARAMETER))
    );
  }

  private int getMaxIiaIds() {
    return getMaxIds("iia-ids");
  }

  private int getMaxIiaCodes() {
    return getMaxIds("iia-codes");
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.maxIiaIds = getMaxIiaIds();
    this.currentState.maxIiaCodes = getMaxIiaCodes();

    List<String> coveredHeiIds = new ArrayList<>();
    if (this.currentState.parameters.contains(HEI_ID_PARAMETER)) {
      coveredHeiIds.add(this.currentState.parameters.get(HEI_ID_PARAMETER));
    } else {
      coveredHeiIds = getCoveredHeiIds(this.currentState.url);
    }

    this.currentState.selectedHeiId = coveredHeiIds.get(0);

    String indexUrl = getApiUrlForHei(
        this.currentState.selectedHeiId, this.getApiInfo().getApiName(), ApiEndpoint.Index,
        "Retrieving 'index' endpoint url from catalogue.",
        "Couldn't find 'index' endpoint url in the catalogue. Is manifest correct?");

    if (this.currentState.parameters.contains(IIA_ID_PARAMETER)) {
      this.currentState.selectedIiaId = this.currentState.parameters.get(IIA_ID_PARAMETER);
    } else {
      this.currentState.selectedIiaId = getCoveredIiaIds(
          Collections.singletonList(
              new HeiIdAndUrl(
                  this.currentState.selectedHeiId,
                  indexUrl,
                  ApiEndpoint.Index
              )
          ),
          securityDescription
      ).string;
    }
  }

  private HeiIdAndString getCoveredIiaIds(
      List<HeiIdAndUrl> heiIdAndUrls,
      HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    return findResponseWithString(
        heiIdAndUrls,
        securityDescription,
        "/iias-index-response/iia-id",
        "Find iia-id to work with.",
        "We tried to find iia-id to perform tests on, but index endpoint doesn't report "
            + "any iia-id, cannot continue tests."
    );
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }
}
