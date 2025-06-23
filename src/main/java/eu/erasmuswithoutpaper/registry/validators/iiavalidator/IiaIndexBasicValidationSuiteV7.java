package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Describes the set of test/steps to be run on an IIAs API index endpoint implementation in order
 * to properly validate it.
 */
public class IiaIndexBasicValidationSuiteV7 extends AbstractValidationSuite<IiaSuiteState> {

  @Override
  protected ValidatedApiInfo createApiInfo(int version) {
    return new IiaValidatedApiInfo(version, ApiEndpoint.INDEX);
  }

  IiaIndexBasicValidationSuiteV7(ApiValidator<IiaSuiteState> validator, IiaSuiteState state,
      ValidationSuiteConfig config, int version) {
    super(validator, state, config, version);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(
        combination,
        "Correct request, expect 200 OK.",
        new ParameterList(),
        iiaIdVerifierFactory.expectCorrectResponse()
    );

    testParameters200(
        combination,
        "Request with receiving_academic_year_id in northern hemisphere format, expect 200 OK.",
        new ParameterList(
            new Parameter("receiving_academic_year_id", "2010/2011")
        ),
        iiaIdVerifierFactory.expectCorrectResponse()
    );

    testParametersError(
        combination,
        "Request with receiving_academic_year_id in incorrect format, expect 400.",
        new ParameterList(
            new Parameter("receiving_academic_year_id", "test/test")
        ),
        400
    );

    testParametersError(
        combination,
        "Request with multiple modified_since parameters, expect 400.",
        new ParameterList(
            new Parameter("modified_since", "2004-02-12T15:19:21+01:00"),
            new Parameter("modified_since", "2004-02-13T15:19:21+01:00")
        ),
        400
    );
  }

  private VerifierFactory iiaIdVerifierFactory = new VerifierFactory(Arrays.asList("iia-id"));
}