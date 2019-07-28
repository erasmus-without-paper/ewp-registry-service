package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.index;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractSetupValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsSuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class IMobilityTorsIndexSetupValidationSuiteV070
    extends AbstractSetupValidationSuite<IMobilityTorsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(IMobilityTorsIndexSetupValidationSuiteV070.class);

  private static final ValidatedApiInfo apiInfo = new IMobilityTorsIndexValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  static final String RECEIVING_HEI_ID_PARAMETER = "receiving_hei_id";
  static final String SENDING_HEI_ID_PARAMETER = "sending_hei_id";

  public static List<ValidationParameter> getParameters() {
    return Arrays.asList(
        new ValidationParameter(RECEIVING_HEI_ID_PARAMETER),
        new ValidationParameter(SENDING_HEI_ID_PARAMETER,
            Collections.singletonList(RECEIVING_HEI_ID_PARAMETER))
    );
  }

  IMobilityTorsIndexSetupValidationSuiteV070(ApiValidator<IMobilityTorsSuiteState> validator,
      IMobilityTorsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  @Override
  protected boolean shouldAnonymousClientBeAllowedToAccessThisApi() {
    return false;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IMobilityTorsSuiteState not just SuiteState
  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void runApiSpecificTests(HttpSecurityDescription securityDescription)
      throws SuiteBroken {
    if (this.currentState.parameters.contains(RECEIVING_HEI_ID_PARAMETER)) {
      this.currentState.receivingHeiId = this.currentState.parameters
          .get(RECEIVING_HEI_ID_PARAMETER);
    } else {
      getReceivingHeiId();
    }

    if (this.currentState.parameters.contains(SENDING_HEI_ID_PARAMETER)) {
      this.currentState.sendingHeiId = this.currentState.parameters.get(SENDING_HEI_ID_PARAMETER);
    } else {
      getSendingHeiId();
    }
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void getReceivingHeiId() throws SuiteBroken {
    this.currentState.receivingHeiId = "test.hei01.uw.edu.pl";
  }

  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void getSendingHeiId() throws SuiteBroken {
    this.currentState.sendingHeiId = "validator-hei01.developers.erasmuswithoutpaper.eu";
  }
}
