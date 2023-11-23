package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import eu.erasmuswithoutpaper.registry.iia.ElementHashException;
import eu.erasmuswithoutpaper.registry.iia.HashComparisonResult;
import eu.erasmuswithoutpaper.registry.iia.IiaHashService;
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
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Describes the set of test/steps to be run on an IIAs API GET endpoint implementation in order to
 * properly validate it.
 */
public class IiaGetValidationSuite
    extends AbstractValidationSuite<IiaSuiteState> {

  private static final Logger logger = LoggerFactory.getLogger(IiaGetValidationSuite.class);

  private final ValidatedApiInfo apiInfo;
  private final IiaHashService iiaHashService;
  private final VerifierFactory partnerIiaIdVerifierFactory = new VerifierFactory(
      Arrays.asList("iia", "partner[1]", "iia-id"));

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  IiaGetValidationSuite(ApiValidator<IiaSuiteState> validator, IiaSuiteState state,
      ValidationSuiteConfig config, int version, IiaHashService iiaHashService) {
    super(validator, state, config);

    this.apiInfo = new IiaValidatedApiInfo(version, ApiEndpoint.GET);
    this.iiaHashService = iiaHashService;
  }

  @Override
  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is IiaSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    final ArrayList<byte[]> responses = new ArrayList<>();
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
    List<String> iiaCodes = selectFromDocument(makeXmlFromBytes(response),
        "/iias-get-response/iia/partner/iia-id[text()=\""
            + currentState.selectedIiaId + "\"]/../iia-code"
    );

    if (!iiaCodes.isEmpty()) {
      generalTestsIdsAndCodes(combination, this.currentState.selectedHeiId, "iia",
          this.currentState.selectedIiaId, this.currentState.maxIiaIds, iiaCodes.get(0),
          this.currentState.maxIiaCodes, partnerIiaIdVerifierFactory);
    } else {
      generalTestsIds(combination, "hei_id", this.currentState.selectedHeiId, "iia",
          this.currentState.selectedIiaId, this.currentState.maxIiaIds, true,
          partnerIiaIdVerifierFactory);
    }

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
              iiaHashService.checkCooperationConditionsHash(
                  new InputSource(new ByteArrayInputStream(response)));
          for (HashComparisonResult comparisonResult : hashComparisonResults) {
            if (!comparisonResult.isCorrect()) {
              throw new Failure(String.format(
                  "conditions-hash is invalid. Extracted: %s, expected: %s, string hashed: %s",
                  comparisonResult.getHashExtracted(), comparisonResult.getHashExpected(),
                  comparisonResult.getHashedString()), Status.FAILURE, false);
            }
          }
        } catch (ElementHashException | ParserConfigurationException | IOException | SAXException
            | XPathExpressionException e) {
          throw new Failure("Cannot read conditions-hash: " + e.getMessage(), Status.ERROR, false);
        }

        return Optional.empty();
      }
    });
  }
}
