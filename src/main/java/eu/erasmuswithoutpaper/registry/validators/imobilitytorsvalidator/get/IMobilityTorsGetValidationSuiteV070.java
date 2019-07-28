package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.get;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.InListVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.ListEqualVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.NoopVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IMobility ToRs API get endpoint implementation
 * in order to properly validate it.
 */
class IMobilityTorsGetValidationSuiteV070
    extends AbstractValidationSuite<IMobilityTorsSuiteState> {

  private static final Logger logger = LoggerFactory
      .getLogger(IMobilityTorsGetValidationSuiteV070.class);

  private static final ValidatedApiInfo apiInfo = new IMobilityTorsGetValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  IMobilityTorsGetValidationSuiteV070(ApiValidator<IMobilityTorsSuiteState> validator,
      IMobilityTorsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IMobilityTorsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {

    testParameters200(
        combination,
        "Request for one of known omobility_ids, expect 200 OK.",
        Arrays.asList(
            new Parameter("receiving_hei_id",
                IMobilityTorsGetValidationSuiteV070.this.currentState.receivingHeiId),
            new Parameter(
                "omobility_id",
                IMobilityTorsGetValidationSuiteV070.this.currentState.omobilityId)
        ),
        new NoopVerifier()
    );

    generalTestsIds(combination,
        "receiving_hei_id", this.currentState.receivingHeiId,
        "omobility",
        this.currentState.omobilityId, this.currentState.maxOmobilityIds,
        false,
        IMobilityTorsEqualVerifier::new,
        IMobilityTorsInListVerifier::new
    );

    testParametersError(
        combination,
        "Request with correct receiving_hei_id and incorrect receiving_hei_id, expect 400.",
        Arrays.asList(
            new Parameter("receiving_hei_id", this.currentState.receivingHeiId),
            new Parameter("receiving_hei_id", fakeId),
            new Parameter("omobility_id",
                IMobilityTorsGetValidationSuiteV070.this.currentState.omobilityId)
        ),
        400
    );
  }

  private static class IMobilityTorsEqualVerifier extends ListEqualVerifier {

    protected IMobilityTorsEqualVerifier(List<String> expected) {
      super(expected);
    }

    @Override
    protected List<String> getSelector() {
      return Arrays.asList("tor", "omobility-id");
    }
  }

  private static class IMobilityTorsInListVerifier extends InListVerifier {
    public IMobilityTorsInListVerifier(List<String> wantedValue) {
      super(wantedValue);
    }

    @Override
    protected List<String> getSelector() {
      return Arrays.asList("tor", "omobility-id");
    }
  }
}
