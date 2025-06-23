package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.index;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesSuiteState;
import eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator.OMobilitiesValidatedApiInfo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


class OMobilitiesIndexSetupValidationSuiteV2
    extends AbstractSetupValidationSuite<OMobilitiesSuiteState> {

  private final ValidatedApiInfo apiInfo;

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  private static final String RECEIVING_HEI_ID_PARAMETER = "receiving_hei_id";
  private static final String SENDING_HEI_ID_PARAMETER = "sending_hei_id";
  protected static final String NOT_PERMITTED_HEI_ID_PARAMETER = "not_permitted_hei_id";

  private static final String NOT_PERMITTED_HEI_ID_DESCRIPTION =
      "Will be used as sending_hei_id parameter to test if it is not possible "
      + "for real hei id to access restricted data. Defaults to uw.edu.pl (but if sending_hei_id "
      + "is uw.edu.pl then uma.es is used).";


  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(RECEIVING_HEI_ID_PARAMETER),
        new ValidationParameter(SENDING_HEI_ID_PARAMETER)
            .dependsOn(RECEIVING_HEI_ID_PARAMETER),
        new ValidationParameter(NOT_PERMITTED_HEI_ID_PARAMETER)
            .withDescription(NOT_PERMITTED_HEI_ID_DESCRIPTION)
    );
  }

  OMobilitiesIndexSetupValidationSuiteV2(ApiValidator<OMobilitiesSuiteState> validator,
      OMobilitiesSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, false);

    this.apiInfo = new OMobilitiesValidatedApiInfo(version, ApiEndpoint.INDEX);
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OMobilitiesSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    this.currentState.receivingHeiId = getParameterValue(RECEIVING_HEI_ID_PARAMETER,
        this::getReceivingHeiId);
    this.currentState.sendingHeiId = getParameterValue(SENDING_HEI_ID_PARAMETER,
        this::getSendingHeiId);
    this.currentState.notPermittedHeiId = getParameterValue(NOT_PERMITTED_HEI_ID_PARAMETER,
        () -> getOtherRealHeiId(this.currentState.sendingHeiId));
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected String getOtherRealHeiId(String knownRealHeiId) throws SuiteBroken {
    String realHeiId1 = "uw.edu.pl";
    String realHeiId2 = "uma.es";
    if (knownRealHeiId.equals(realHeiId1)) {
      return realHeiId2;
    } else {
      return realHeiId1;
    }
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private String getReceivingHeiId() throws SuiteBroken {
    if (!this.validatorKeyStore.getCoveredHeiIDs().isEmpty()) {
      return this.validatorKeyStore.getCoveredHeiIDs().get(0);
    }
    return "";
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private String getSendingHeiId() throws SuiteBroken {
    return getCoveredHeiIds(this.currentState.url).get(0);
  }

}
