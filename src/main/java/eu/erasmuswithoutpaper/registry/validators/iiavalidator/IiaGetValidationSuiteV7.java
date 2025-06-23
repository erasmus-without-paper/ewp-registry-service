package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.Collections;

import eu.erasmuswithoutpaper.registry.iia.IiaHashService;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Describes the set of test/steps to be run on an IIAs API GET endpoint implementation in order to
 * properly validate it.
 */
public class IiaGetValidationSuiteV7 extends IiaGetValidationSuiteV6 {

  IiaGetValidationSuiteV7(ApiValidator<IiaSuiteState> validator, IiaSuiteState state,
      ValidationSuiteConfig config, int version, IiaHashService iiaHashService) {
    super(validator, state, config, version, iiaHashService);
  }

  @Override
  // FindBugs is not smart enough to infer that actual type of this.currentState
  // is IiaSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void testIds(Combination combination, byte[] response) throws SuiteBroken {
    String id = this.currentState.selectedIiaId;
    int maxIds = this.currentState.maxIiaIds;

    testParameters200(combination, "Request one unknown iia_id, expect 200 and empty response.",
        new ParameterList(new Parameter("iia_id", FAKE_ID)),
        partnerIiaIdVerifierFactory.expectResponseToBeEmpty());

    testParameters200(combination,
        "Request one known and one unknown iia_id, expect 200 and only one iia in response.",
        new ParameterList(new Parameter("iia_id", id), new Parameter("iia_id", FAKE_ID)),
        partnerIiaIdVerifierFactory.expectResponseToContainExactly(Collections.singletonList(id)),
        maxIds == 1, "max-ids is equal to 1."

    );

    testParametersError(combination, "Request without iia_ids, expect 400.", new ParameterList(),
        400);

    testParametersError(combination, "Request more than <max-iia-ids> known iia_ids, expect 400.",
        new ParameterList(Collections.nCopies(maxIds + 1, new Parameter("iia_id", id))), 400);

    testParametersError(combination, "Request more than <max-iia-ids> unknown iia_ids, expect 400.",
        new ParameterList(Collections.nCopies(maxIds + 1, new Parameter("iia_id", FAKE_ID))), 400);

    testParameters200(combination,
        "Request exactly <max-iia-ids> known iia_ids, expect 200 and non-empty response.",
        new ParameterList(Collections.nCopies(maxIds, new Parameter("iia_id", id))),
        partnerIiaIdVerifierFactory.expectResponseToContain(Collections.singletonList(id)));

    testParametersError(combination, "Request with single incorrect parameter, expect 400.",
        new ParameterList(new Parameter("iia_id_param", id)), 400);
  }

  @Override
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected ParameterList getParameterList() {
    return new ParameterList(new Parameter("iia_id", currentState.selectedIiaId));
  }
}
