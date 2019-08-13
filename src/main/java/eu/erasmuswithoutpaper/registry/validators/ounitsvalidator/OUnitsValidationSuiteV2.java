package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.verifiers.InListVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.ListEqualVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class OUnitsValidationSuiteV2
    extends AbstractValidationSuite<OUnitsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(OUnitsValidationSuiteV2.class);

  private static final ValidatedApiInfo apiInfo = new OUnitsValidatedApiInfo();

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }


  OUnitsValidationSuiteV2(ApiValidator<OUnitsSuiteState> validator,
      OUnitsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

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
            Arrays.asList(
                new Parameter("hei_id", OUnitsValidationSuiteV2.this.currentState.selectedHeiId),
                new Parameter(
                    "ounit_id", OUnitsValidationSuiteV2.this.currentState.selectedOunitId)
            )
        );
        List<String> expectedIDs =
            Collections.singletonList(OUnitsValidationSuiteV2.this.currentState.selectedOunitId);
        Response response = verifyResponse(
            this, combination, request, new OUnitIdsVerifier(expectedIDs)
        );
        List<String> codes = selectFromDocument(
            makeXmlFromBytes(response.getBody()),
            "/ounits-response/ounit/ounit-id[text()=\""
                + OUnitsValidationSuiteV2.this.currentState.selectedOunitId
                + "\"]/../ounit-code"
        );
        ounitCodes.add(codes.get(0));
        return Optional.of(response);
      }
    });

    generalTestsIdsAndCodes(
        combination, "ounit", this.currentState.selectedHeiId, this.currentState.selectedOunitId,
        ounitCodes.get(0), this.currentState.maxOunitIds, this.currentState.maxOunitCodes,
        OUnitIdsVerifier::new,
        InListOUnitIdsVerifier::new
    );

    testParametersError(
        combination,
        "Request with correct hei_id and incorrect hei_id, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("hei_id", this.fakeId),
            new Parameter("ounit_id", OUnitsValidationSuiteV2.this.currentState.selectedOunitId)
        ),
        400,
        Status.WARNING
    );
  }

  private static class OUnitIdsVerifier extends ListEqualVerifier {
    OUnitIdsVerifier(List<String> expected) {
      super(expected);
    }

    @Override
    protected List<String> getSelector() {
      return Arrays.asList("ounit", "ounit-id");
    }
  }

  private static class InListOUnitIdsVerifier extends InListVerifier {
    InListOUnitIdsVerifier(List<String> expected) {
      super(expected);
    }

    @Override
    protected List<String> getSelector() {
      return Arrays.asList("ounit", "ounit-id");
    }
  }
}
