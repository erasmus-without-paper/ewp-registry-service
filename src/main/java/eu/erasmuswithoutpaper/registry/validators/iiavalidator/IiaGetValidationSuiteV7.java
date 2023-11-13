package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.iia.HashComparisonResult;
import eu.erasmuswithoutpaper.registry.iia.IiaHashService;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

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
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    final ArrayList<byte[]> responses = new ArrayList<>();
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
                new Parameter("hei_id", currentState.selectedHeiId),
                new Parameter("iia_id", currentState.selectedIiaId)
            )
        );
        List<String> expectedIDs = Collections.singletonList(currentState.selectedIiaId);
        Response response = makeRequestAndVerifyResponse(
            this, combination, request,
            partnerIiaIdVerifierFactory.expectResponseToContainExactly(expectedIDs)
        );
        responses.add(response.getBody());

        return Optional.of(response);
      }
    });

    byte[] response = responses.get(0);

    generalTestsIds(combination, "hei_id", this.currentState.selectedHeiId, "iia",
        this.currentState.selectedIiaId, this.currentState.maxIiaIds, true,
        partnerIiaIdVerifierFactory);

    this.addAndRun(false, new InlineValidationStep() {
      @Override
      public String getName() {
        return "Verify conditions-hash.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        try {
          List<HashComparisonResult> hashComparisonResults =
              iiaHashService.checkHash(
                  new InputSource(new ByteArrayInputStream(response)));
          for (HashComparisonResult comparisonResult : hashComparisonResults) {
            if (!comparisonResult.isCorrect()) {
              throw new Failure(String.format(
                  "conditions-hash is invalid. Extracted: %s, expected: %s, string hashed: %s",
                  comparisonResult.getHashExtracted(), comparisonResult.getHashExpected(),
                  comparisonResult.getHashedString()), Status.FAILURE, false);
            }
          }
        } catch (Exception e) {
          throw new Failure("Error validating hash: " + e.getMessage(), Status.ERROR, false);
        }

        return Optional.empty();
      }
    });
  }
}