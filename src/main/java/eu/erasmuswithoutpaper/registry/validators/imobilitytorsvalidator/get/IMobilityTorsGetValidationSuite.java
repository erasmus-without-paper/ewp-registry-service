package eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.get;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsSuiteState;
import eu.erasmuswithoutpaper.registry.validators.imobilitytorsvalidator.IMobilityTorsValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Describes the set of test/steps to be run on an IMobility ToRs API get endpoint implementation
 * in order to properly validate it.
 */
class IMobilityTorsGetValidationSuite
    extends AbstractValidationSuite<IMobilityTorsSuiteState> {

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new IMobilityTorsValidatedApiInfo(version, ApiEndpoint.GET);
  }

  IMobilityTorsGetValidationSuite(ApiValidator<IMobilityTorsSuiteState> validator,
      IMobilityTorsSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IMobilityTorsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {

    testParameters200(
        combination,
        "Request for one of known omobility_ids, expect 200 OK.",
        new ParameterList(
            new Parameter("receiving_hei_id",
                this.currentState.receivingHeiId),
            new Parameter(
                "omobility_id",
                this.currentState.omobilityId)
        ),
        omobilityIdVerifierFactory.expectResponseToContainExactly(
            Arrays.asList(this.currentState.omobilityId)
        )
    );

    generalTestsIds(combination,
        "receiving_hei_id", this.currentState.receivingHeiId,
        "omobility",
        this.currentState.omobilityId, this.currentState.maxOmobilityIds,
        false,
        omobilityIdVerifierFactory
    );
  }

  private VerifierFactory omobilityIdVerifierFactory =
      new VerifierFactory(Arrays.asList("tor", "omobility-id"));
}
