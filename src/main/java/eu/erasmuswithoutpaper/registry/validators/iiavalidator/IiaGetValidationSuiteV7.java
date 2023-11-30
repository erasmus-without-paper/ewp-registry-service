package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import eu.erasmuswithoutpaper.registry.iia.IiaHashService;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IIAs API GET endpoint implementation in order to
 * properly validate it.
 */
public class IiaGetValidationSuiteV7 extends IiaGetValidationSuiteV6 {

  private static final Logger logger = LoggerFactory.getLogger(IiaGetValidationSuiteV7.class);

  @Override
  protected Logger getLogger() {
    return logger;
  }

  IiaGetValidationSuiteV7(ApiValidator<IiaSuiteState> validator, IiaSuiteState state,
      ValidationSuiteConfig config, int version, IiaHashService iiaHashService) {
    super(validator, state, config, version, iiaHashService);
  }

  @Override
  // FindBugs is not smart enough to infer that actual type of this.currentState
  // is IiaSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void testIds(Combination combination, byte[] response) throws SuiteBroken {
    generalTestsIds(combination, "hei_id", this.currentState.selectedHeiId, "iia",
        this.currentState.selectedIiaId, this.currentState.maxIiaIds, true,
        partnerIiaIdVerifierFactory);
  }

}