package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class OUnitsValidationSuite
    extends AbstractValidationSuite<OUnitsSuiteState> {

  private final ValidatedApiInfo apiInfo;

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  OUnitsValidationSuite(ApiValidator<OUnitsSuiteState> validator,
      OUnitsSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new OUnitsValidatedApiInfo(version, ApiEndpoint.NO_ENDPOINT);
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OUnitsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {

    ArrayList<String> ounitCodes = new ArrayList<>();
    //Success is required here, we need to fetch ounit-codes using this method
    this.addAndRun(true, new InlineValidationStep() {
      @Override
      public String getName() {
        return "Request for one of known ounit-ids, expect 200 OK.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        Request request = createRequestWithParameters(this, combination,
            new ParameterList(
                new Parameter("hei_id", currentState.selectedHeiId),
                new Parameter("ounit_id", currentState.selectedOunitId)
            )
        );
        List<String> expectedIDs =
            Collections.singletonList(currentState.selectedOunitId);
        Response response = makeRequestAndVerifyResponse(
            this, combination, request, ounitIdVerifier.expectResponseToContainExactly(expectedIDs)
        );
        List<String> codes = selectFromDocument(
            makeXmlFromBytes(response.getBody()),
            "/ounits-response/ounit/ounit-id[text()=\""
                + currentState.selectedOunitId
                + "\"]/../ounit-code"
        );
        ounitCodes.add(codes.get(0));
        return Optional.of(response);
      }
    });

    generalTestsIdsAndCodes(combination,
        this.currentState.selectedHeiId,
        "ounit",
        this.currentState.selectedOunitId, this.currentState.maxOunitIds,
        ounitCodes.get(0), this.currentState.maxOunitCodes,
        ounitIdVerifier
    );
  }

  private VerifierFactory ounitIdVerifier = new VerifierFactory(Arrays.asList("ounit", "ounit-id"));
}
