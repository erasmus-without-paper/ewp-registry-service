package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.get;

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

public class IiaGetSetupValidationSuiteV2
    extends AbstractSetupValidationSuite<IiaSuiteState> {

  private static final Logger logger = LoggerFactory.getLogger(IiaGetSetupValidationSuiteV2.class);
  private static final ValidatedApiInfo apiInfo = new IiaGetValidatedApiInfoV2();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }


  /**
   * Creates a validation suite for IIAs v2 Get endpoint.
   */
  public IiaGetSetupValidationSuiteV2(ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state,
      ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  protected static final String HEI_ID_PARAMETER = "hei_id";
  private static final String IIA_ID_PARAMETER = "iia_id";

  /**
   * Returns parameters used for validating IIAs v2 Get.
   */
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
    this.currentState.selectedHeiId = getParameterValue(HEI_ID_PARAMETER, this::getSelectedHeiId);
    this.currentState.selectedIiaId = getParameterValue(IIA_ID_PARAMETER,
        () -> getIiaId(securityDescription));
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private String getSelectedHeiId() throws SuiteBroken {
    return getCoveredHeiIds(this.currentState.url).get(0);
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private String getIiaId(HttpSecurityDescription securityDescription) throws SuiteBroken {
    String indexUrl = getApiUrlForHei(
        this.currentState.selectedHeiId, this.getApiInfo().getApiName(), ApiEndpoint.Index,
        "Retrieving 'index' endpoint url from catalogue.",
        "Couldn't find 'index' endpoint url in the catalogue. Is manifest correct?");

    HeiIdAndString foundIiaId = getCoveredIiaIds(
        Collections.singletonList(
            new HeiIdAndUrl(
                this.currentState.selectedHeiId,
                indexUrl,
                ApiEndpoint.Index
            )
        ),
        securityDescription
    );
    return foundIiaId.string;
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
