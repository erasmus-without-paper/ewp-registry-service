package eu.erasmuswithoutpaper.registry.validators.iiavalidator.get;

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
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.InListVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.ListEqualVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IIAs API GET endpoint implementation in order to
 * properly validate it.
 */
class IiaGetValidationSuiteV2
    extends AbstractValidationSuite<IiaSuiteState> {

  private static final Logger logger = LoggerFactory.getLogger(IiaGetValidationSuiteV2.class);

  private static final ValidatedApiInfo apiInfo = new IiaGetValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  IiaGetValidationSuiteV2(ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    ArrayList<String> iiaCodes = new ArrayList<>();
    //Success is required here, we need to fetch ounit-codes using this method
    this.addAndRun(true, new InlineValidationStep() {
      @Override
      public String getName() {
        return "Request for one of known iia_ids, expect 200 OK.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        Request request = createRequestWithParameters(this, combination,
            Arrays.asList(
                new Parameter("hei_id", IiaGetValidationSuiteV2.this.currentState.selectedHeiId),
                new Parameter(
                    "iia_id", IiaGetValidationSuiteV2.this.currentState.selectedIiaId)
            )
        );
        List<String> expectedIDs =
            Collections.singletonList(IiaGetValidationSuiteV2.this.currentState.selectedIiaId);
        Response response = verifyResponse(
            this, combination, request, new IiaIdsVerifier(expectedIDs)
        );
        List<String> codes = selectFromDocument(
            makeXmlFromBytes(response.getBody()),
            "/iias-get-response/iia/partner/iia-id[text()=\""
                + IiaGetValidationSuiteV2.this.currentState.selectedIiaId
                + "\"]/../iia-code"
        );
        iiaCodes.add(codes.get(0));
        return Optional.of(response);
      }
    });

    generalTestsIdsAndCodes(combination,
        this.currentState.selectedHeiId,
        "iia",
        this.currentState.selectedIiaId, this.currentState.maxIiaIds,
        iiaCodes.get(0), this.currentState.maxIiaCodes,
        IiaIdsVerifier::new,
        InListIiaIdsVerifier::new
    );

    testParametersError(
        combination,
        "Request with correct hei_id and incorrect hei_id, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("hei_id", fakeId),
            new Parameter("iia_id", IiaGetValidationSuiteV2.this.currentState.selectedIiaId)
        ),
        400,
        Status.WARNING
    );
  }

  private static class IiaIdsVerifier extends ListEqualVerifier {
    IiaIdsVerifier(List<String> expected) {
      super(expected);
    }

    @Override
    protected List<String> getSelector() {
      return Arrays.asList("iia", "partner[1]", "iia-id");
    }
  }

  private static class InListIiaIdsVerifier extends InListVerifier {
    InListIiaIdsVerifier(List<String> expected) {
      super(expected);
    }

    @Override
    protected List<String> getSelector() {
      return Arrays.asList("iia", "partner[1]", "iia-id");
    }
  }
}
