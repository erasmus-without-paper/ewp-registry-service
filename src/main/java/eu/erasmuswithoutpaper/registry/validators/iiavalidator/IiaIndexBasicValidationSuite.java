package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.Arrays;

import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IIAs API index endpoint implementation in order
 * to properly validate it.
 */
public class IiaIndexBasicValidationSuite extends AbstractValidationSuite<IiaSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(IiaIndexBasicValidationSuite.class);
  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  IiaIndexBasicValidationSuite(ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new IiaValidatedApiInfo(version, ApiEndpoint.Index);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    testParameters200(
        combination,
        "Request one known hei_id, expect 200 OK.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId)
        ),
        iiaIdVerifierFactory.expectCorrectResponse()
    );

    testParametersError(
        combination,
        "Request with known hei_id and unknown hei_id, expect 400.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("hei_id", fakeId)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with unknown hei_id, expect 400.",
        new ParameterList(
            new Parameter("hei_id", fakeId)
        ),
        400
    );

    /*
    testParameters200(
        combination,
        "Request with known hei_id and receiving_academic_year_id in southern hemisphere "
            + "format, expect 200 OK.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("receiving_academic_year_id", "2010/2010")
        ),
        iiaIdVerifierFactory.expectCorrectResponse()
    );
     */

    testParameters200(
        combination,
        "Request with known hei_id and receiving_academic_year_id in northern hemisphere "
            + "format, expect 200 OK.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("receiving_academic_year_id", "2010/2011")
        ),
        iiaIdVerifierFactory.expectCorrectResponse()
    );

    testParametersError(
        combination,
        "Request with receiving_academic_year_id in incorrect format, expect 400.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("receiving_academic_year_id", "test/test")
        ),
        400
    );

    testParametersError(
        combination,
        "Request with known hei_id equal to partner_hei_id, expect 400.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("partner_hei_id", this.currentState.selectedHeiId)
        ),
        400
    );

    testParameters200(
        combination,
        "Request with known hei_id and unknown partner_hei_id, expect 200 OK and empty list.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("partner_hei_id", fakeId)
        ),
        iiaIdVerifierFactory.expectResponseToBeEmpty()
    );

    testParametersError(
        combination,
        "Request with multiple modified_since parameters, expect 400.",
        new ParameterList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("modified_since", "2004-02-12T15:19:21+01:00"),
            new Parameter("modified_since", "2004-02-13T15:19:21+01:00")
        ),
        400
    );
  }

  private VerifierFactory iiaIdVerifierFactory = new VerifierFactory(Arrays.asList("iia-id"));
}
