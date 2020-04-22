package eu.erasmuswithoutpaper.registry.validators.iiavalidator.v2.get;

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
import eu.erasmuswithoutpaper.registry.validators.iiavalidator.IiaSuiteState;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IIAs API GET endpoint implementation in order to
 * properly validate it.
 */
public class IiaGetValidationSuiteV2
    extends AbstractValidationSuite<IiaSuiteState> {

  private static final Logger logger = LoggerFactory.getLogger(IiaGetValidationSuiteV2.class);

  private static final ValidatedApiInfo apiInfo = new IiaGetValidatedApiInfoV2();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  public IiaGetValidationSuiteV2(ApiValidator<IiaSuiteState> validator,
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
        Response response = makeRequestAndVerifyResponse(
            this, combination, request,
            partnerIiaIdVerifierFactory.expectResponseToContainExactly(expectedIDs)
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
        partnerIiaIdVerifierFactory
    );

    testParametersError(
        combination,
        "Request with correct hei_id and incorrect hei_id, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("hei_id", fakeId),
            new Parameter("iia_id", IiaGetValidationSuiteV2.this.currentState.selectedIiaId)
        ),
        400
    );
  }

  private VerifierFactory partnerIiaIdVerifierFactory
      = new VerifierFactory(Arrays.asList("iia", "partner[1]", "iia-id"));
}
