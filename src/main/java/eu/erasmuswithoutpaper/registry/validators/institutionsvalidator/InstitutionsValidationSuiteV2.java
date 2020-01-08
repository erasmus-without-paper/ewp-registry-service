package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep.Failure;
import eu.erasmuswithoutpaper.registry.validators.ValidatedApiInfo;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.verifiers.InListVerifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.ListEqualVerifier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class InstitutionsValidationSuiteV2
    extends AbstractValidationSuite<InstitutionsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(InstitutionsValidationSuiteV2.class);

  private static final ValidatedApiInfo apiInfo = new InstitutionsValidatedApiInfo();

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  public ValidatedApiInfo getApiInfo() {
    return apiInfo;
  }

  InstitutionsValidationSuiteV2(ApiValidator<InstitutionsSuiteState> validator,
      InstitutionsSuiteState state, ValidationSuiteConfig config) {
    super(validator, state, config);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is InstitutionsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    String fakeHeiId = this.fakeId;

    testParameters200(combination, "Request for one of known hei_ids, expect 200 OK.",
        Arrays.asList(new Parameter("hei_id", currentState.selectedHeiId)),
        new ListEqualInstitutionsVerifier(Collections.singletonList(currentState.selectedHeiId))
    );

    testParameters200(combination, "Request one unknown hei_id, expect 200 and empty response.",
        Collections.singletonList(new Parameter("hei_id", fakeHeiId)),
        new ListEqualInstitutionsVerifier(new ArrayList<>())
    );

    if (this.currentState.maxHeiIds > 1) {
      testParameters200(
          combination,
          "Request one known and one unknown hei_id, expect 200 and only one <hei-id> in response.",
          Arrays.asList(new Parameter("hei_id", currentState.selectedHeiId),
              new Parameter("hei_id", fakeHeiId)
          ),
          new ListEqualInstitutionsVerifier(Collections.singletonList(currentState.selectedHeiId))
      );
    }

    testParametersError(combination, "Request without hei_ids, expect 400.", new ArrayList<>(),
        400
    );

    testParametersError(combination, "Request more than <max-hei-ids> known hei_ids, expect 400.",
        Collections.nCopies(this.currentState.maxHeiIds + 1,
            new Parameter("hei_id", currentState.selectedHeiId)
        ),
        400
    );

    testParametersError(combination, "Request more than <max-hei-ids> unknown hei_ids, expect 400.",
        Collections.nCopies(this.currentState.maxHeiIds + 1, new Parameter("hei_id", fakeHeiId)),
        400
    );

    testParameters200(combination, "Request exactly <max-hei-ids> known hei_ids, "
            + "expect 200 and non-empty response.",
        Collections.nCopies(this.currentState.maxHeiIds,
            new Parameter("hei_id", currentState.selectedHeiId)
        ),
        new InListInstitutionsVerifier(Collections.singletonList(currentState.selectedHeiId))
    );

    testParametersError(combination, "Request with single incorrect parameter, expect 400.",
        Arrays.asList(new Parameter("hei_id_param", currentState.selectedHeiId)), 400
    );
  }


  private static void verifyRootOUnitId(AbstractValidationSuite suite, Match root,
      Response response,
      Status failureStatus)
      throws Failure {
    String nsPrefix = suite.getApiInfo().getResponsePrefix() + ":";

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
                + "root-ounit-id is not included in ounit-id list.", failureStatus, response);
      }
    }
  }

  private static class ListEqualInstitutionsVerifier extends ListEqualVerifier {
    private ListEqualInstitutionsVerifier(List<String> expectedHeiIDs) {
      super(expectedHeiIDs, Arrays.asList("hei", "hei-id"));
    }

    @Override
    protected void verify(AbstractValidationSuite suite, Match root, Response response,
        Status failureStatus)
        throws Failure {
      super.verify(suite, root, response, failureStatus);
      verifyRootOUnitId(suite, root, response, failureStatus);
    }
  }

  private static class InListInstitutionsVerifier extends InListVerifier {
    private InListInstitutionsVerifier(List<String> expectedHeiIDs) {
      super(expectedHeiIDs, Arrays.asList("hei", "hei-id"));
    }

    @Override
    protected void verify(AbstractValidationSuite suite, Match root, Response response,
        Status failureStatus)
        throws Failure {
      super.verify(suite, root, response, failureStatus);
      verifyRootOUnitId(suite, root, response, failureStatus);
    }
  }
}
