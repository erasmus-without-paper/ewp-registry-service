package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep.Failure;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class InstitutionsValidationSuiteV200 extends InstitutionsValidationSuiteBase {

  private static final Logger logger =
      LoggerFactory.getLogger(InstitutionsValidationSuiteV200.class);

  InstitutionsValidationSuiteV200(ApiValidator<InstitutionsSuiteState> validator,
      EwpDocBuilder docBuilder, Internet internet, RegistryClient regClient,
      ManifestRepository repo, InstitutionsSuiteState state) {
    super(validator, docBuilder, internet, regClient, repo, state);
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is InstitutionsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    final List<String> heis = new ArrayList<>();
    String fakeHeiId = "this-is-some-unknown-and-unexpected-hei-id-its-very-long"
        + "-but-sill-technically-correct-and-i-dont-think-that-anyone-would-use-it-as"
        + "-a-hei-id-even-in-development";

    this.addAndRun(true, new InlineValidationStep() {
      @Override
      public String getName() {
        return "Check if this host covers any institution.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        String url = InstitutionsValidationSuiteV200.this.currentState.url;
        List<String> coveredHeiIds =
            InstitutionsValidationSuiteV200.this.fetchHeiIdsCoveredByApiByUrl(url);
        if (coveredHeiIds.isEmpty()) {
          throw new InlineValidationStep.Failure(
              "Manifest file doesn't contain any <hei-id> field. We cannot preform tests.",
              ValidationStepWithStatus.Status.FAILURE, null
          );
        }
        heis.addAll(coveredHeiIds);
        return Optional.empty();
      }
    });

    testParameters200(combination, "Request for one of known HEI IDs, expect 200 OK.",
        Arrays.asList(new Parameter("hei_id", heis.get(0))),
        new InstitutionsVerifier(Collections.singletonList(heis.get(0)))
    );

    testParameters200(combination, "Request one unknown HEI ID, expect 200 and empty response.",
        Collections.singletonList(new Parameter("hei_id", fakeHeiId)),
        new InstitutionsVerifier(new ArrayList<>())
    );

    if (this.currentState.maxHeiIds > 1) {
      testParameters200(
          combination,
          "Request one known and one unknown HEI ID, expect 200 and only one HEI in response.",
          Arrays.asList(new Parameter("hei_id", heis.get(0)), new Parameter("hei_id", fakeHeiId)),
          new InstitutionsVerifier(Collections.singletonList(heis.get(0)))
      );
    }

    testParametersError(combination, "Request without HEI IDs, expect 400.", new ArrayList<>(),
        400
    );

    testParametersError(combination, "Request more than <max-hei-ids> known HEIs, expect 400.",
        Collections.nCopies(this.currentState.maxHeiIds + 1, new Parameter("hei_id", heis.get(0))),
        400
    );

    testParametersError(combination, "Request more than <max-hei-ids> unknown HEI IDs, expect 400.",
        Collections.nCopies(this.currentState.maxHeiIds + 1, new Parameter("hei_id", fakeHeiId)),
        400
    );

    testParameters200(combination, "Request exactly <max-hei-ids> known HEI IDs, "
            + "expect 200 and <max-hei-ids> HEI IDs in response.",
        Collections.nCopies(this.currentState.maxHeiIds, new Parameter("hei_id", heis.get(0))),
        new InstitutionsVerifier(Collections.nCopies(this.currentState.maxHeiIds, heis.get(0)))
    );

    testParametersError(combination, "Request with single incorrect parameter, expect 400.",
        Arrays.asList(new Parameter("hei_id_param", heis.get(0))), 400
    );

    testParameters200(combination,
        "Request with additional parameter, expect 200 and one hei_id response.", Arrays
            .asList(
                new Parameter("hei_id", heis.get(0)),
                new Parameter("hei_id_param", heis.get(0))
            ),
        new InstitutionsVerifier(Collections.singletonList(heis.get(0)))
    );
  }

  @Override
  protected void validateCombinationPost(Combination combination)
      throws SuiteBroken {
    this.addAndRun(
        false,
        this.createHttpMethodValidationStep(combination.withChangedHttpMethod("PUT"))
    );
    this.addAndRun(
        false,
        this.createHttpMethodValidationStep(combination.withChangedHttpMethod("DELETE"))
    );
    validateCombinationAny(combination);
  }

  @Override
  protected void validateCombinationGet(Combination combination)
      throws SuiteBroken {
    validateCombinationAny(combination);
  }


  private static class InstitutionsVerifier implements Verifier {
    private final List<String> expectedHeiIDs;

    private InstitutionsVerifier(List<String> expectedHeiIDs) {
      this.expectedHeiIDs = expectedHeiIDs;
    }

    @Override
    public void verify(AbstractValidationSuite suite, Match root, Response response)
        throws Failure {
      List<String> receivedHeiIds = new ArrayList<>();
      String nsPrefix = suite.getApiResponsePrefix() + ":";
      for (Match entry : root.xpath(nsPrefix + "hei/" + nsPrefix + "hei-id").each()) {
        receivedHeiIds.add(entry.text());
      }
      for (String receivedId : receivedHeiIds) {
        if (!expectedHeiIDs.contains(receivedId)) {
          throw new Failure(
              "The response has proper HTTP status and it passed the schema validation. However, "
                  + "the set of returned hei-ids doesn't match what we expect. It contains <hei-id>"
                  + receivedId + "</hei-id>, but it shouldn't. It should contain the following: "
                  + expectedHeiIDs, Status.FAILURE, response);
        }
      }
      for (String expectedId : expectedHeiIDs) {
        if (!receivedHeiIds.contains(expectedId)) {
          throw new Failure(
              "The response has proper HTTP status and it passed the schema validation. However, "
                  + "the set of returned hei-ids doesn't match what we expect. "
                  + "It should contain the following: " + expectedHeiIDs, Status.FAILURE, response);
        }
      }

      for (Match entry : root.xpath(nsPrefix + "hei").each()) {
        Match rootOunitId = entry.xpath(nsPrefix + "root-ounit-id").first();
        if (rootOunitId.isEmpty()) {
          continue;
        }

        boolean found = false;

        for (Match ounitId : entry.xpath(nsPrefix + "ounit-id").each()) {
          if (ounitId.text().equals(rootOunitId.text())) {
            found = true;
            break;
          }
        }

        if (!found) {
          throw new Failure(
              "The response has proper HTTP status and it passed the schema validation. However, "
                  + "root-ounit-id is not included in ounit-id list.", Status.FAILURE, response);
        }
      }
    }
  }

}
