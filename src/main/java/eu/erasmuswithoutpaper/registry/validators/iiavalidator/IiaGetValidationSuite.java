package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an IIAs API GET endpoint implementation in order to
 * properly validate it.
 */
public class IiaGetValidationSuite
    extends AbstractValidationSuite<IiaSuiteState> {

  private static final Logger logger = LoggerFactory.getLogger(IiaGetValidationSuite.class);

  private final ValidatedApiInfo apiInfo;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  IiaGetValidationSuite(ApiValidator<IiaSuiteState> validator,
      IiaSuiteState state, ValidationSuiteConfig config, int version) {
    super(validator, state, config);

    this.apiInfo = new IiaValidatedApiInfo(version, ApiEndpoint.Get);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    ArrayList<String> iiaCodes = new ArrayList<>();
    //Success is required here, we need to fetch iia-codes using this method
    this.addAndRun(true, new InlineValidationStep() {
      @Override
      public String getName() {
        return "Request for one of known iia_ids, expect 200 OK.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        Request request = createRequestWithParameters(this, combination,
            new ParameterList(
                new Parameter("hei_id", IiaGetValidationSuite.this.currentState.selectedHeiId),
                new Parameter(
                    "iia_id", IiaGetValidationSuite.this.currentState.selectedIiaId)
            )
        );
        List<String> expectedIDs =
            Collections.singletonList(IiaGetValidationSuite.this.currentState.selectedIiaId);
        Response response = makeRequestAndVerifyResponse(
            this, combination, request,
            partnerIiaIdVerifierFactory.expectResponseToContainExactly(expectedIDs)
        );
        List<String> codes = selectFromDocument(
            makeXmlFromBytes(response.getBody()),
            "/iias-get-response/iia/partner/iia-id[text()=\""
                + IiaGetValidationSuite.this.currentState.selectedIiaId
                + "\"]/../iia-code"
        );
        if (!codes.isEmpty()) {
          iiaCodes.add(codes.get(0));
        }
        return Optional.of(response);
      }
    });

    if (!iiaCodes.isEmpty()) {
      generalTestsIdsAndCodes(combination, this.currentState.selectedHeiId, "iia",
          this.currentState.selectedIiaId, this.currentState.maxIiaIds, iiaCodes.get(0),
          this.currentState.maxIiaCodes, partnerIiaIdVerifierFactory);
    } else {
      generalTestsIds(combination, "hei_id", this.currentState.selectedHeiId, "iia",
          this.currentState.selectedIiaId, this.currentState.maxIiaIds, true,
          partnerIiaIdVerifierFactory);
    }
  }

  private VerifierFactory partnerIiaIdVerifierFactory
      = new VerifierFactory(Arrays.asList("iia", "partner[1]", "iia-id"));
}
