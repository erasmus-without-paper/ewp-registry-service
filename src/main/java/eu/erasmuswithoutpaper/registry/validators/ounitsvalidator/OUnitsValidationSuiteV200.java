package eu.erasmuswithoutpaper.registry.validators.ounitsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep.Failure;
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
class OUnitsValidationSuiteV200
    extends AbstractValidationSuite<OUnitsSuiteState> {

  private static final Logger logger =
      LoggerFactory.getLogger(OUnitsValidationSuiteV200.class);

  OUnitsValidationSuiteV200(ApiValidator<OUnitsSuiteState> validator,
      EwpDocBuilder docBuilder, Internet internet, RegistryClient regClient,
      ManifestRepository repo, OUnitsSuiteState state) {
    super(validator, docBuilder, internet, regClient, repo, state);
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is OUnitsSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  protected void validateCombinationAny(Combination combination)
      throws SuiteBroken {
    final String fakeId = "this-is-some-unknown-and-unexpected-hei-id";

    ArrayList<String> ounitCodes = new ArrayList<>();
    //Success is required here, we need to fetch ounit-codes using this method
    this.addAndRun(true, new InlineValidationStep() {
      @Override
      public String getName() {
        return "Request for one of known OUnit IDs, expect 200 OK.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        Request request = createRequestWithParameters(this, combination,
            Arrays.asList(
                new Parameter("hei_id", OUnitsValidationSuiteV200.this.currentState.selectedHeiId),
                new Parameter(
                    "ounit_id", OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0))
            )
        );
        List<String> expectedIDs =
            Collections.singletonList(OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0));
        Response response = verifyResponse(
            this, combination, request, new OUnitIdsVerifier(expectedIDs)
        );
        List<String> codes = selectFromDocument(
            makeXmlFromBytes(response.getBody()),
            "/ounits-response/ounit/ounit-id[text()=\""
                + OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)
                + "\"]/../ounit-code"
        );
        ounitCodes.add(codes.get(0));
        return Optional.of(response);
      }
    });

    testParameters200(
        combination,
        "Request for one of known OUnit Codes, expect 200 OK.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("ounit_code", ounitCodes.get(0))
        ),
        new OUnitIdsVerifier(
            Collections.singletonList(OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)))
    );

    testParameters200(
        combination,
        "Request one unknown ounit_id, expect 200 and empty response.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("ounit_id", fakeId)
        ),
        new OUnitIdsVerifier(new ArrayList<>())
    );

    if (this.currentState.maxOunitIds > 1) {
      testParameters200(
          combination,
          "Request one known and one unknown Ounit ID, expect 200 and"
              + " only one Ounit in response.",
          Arrays.asList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter(
                  "ounit_id", OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)),
              new Parameter("ounit_id", fakeId)
          ),
          new OUnitIdsVerifier(Collections
              .singletonList(OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)))
      );
    }

    if (this.currentState.maxOunitCodes > 1) {
      testParameters200(
          combination,
          "Request one known and one unknown OUnit Code, expect 200 and"
              + " only one OUnit in response.",
          Arrays.asList(
              new Parameter("hei_id", this.currentState.selectedHeiId),
              new Parameter("ounit_code", ounitCodes.get(0)),
              new Parameter("ounit_code", fakeId)
          ),
          new OUnitIdsVerifier(Collections
              .singletonList(OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)))
      );
    }

    testParametersError(
        combination,
        "Request without HEI IDs and OUnit Ids, expect 400.",
        new ArrayList<>(),
        400
    );

    testParametersError(
        combination,
        "Request without HEI IDs, expect 400.",
        Arrays.asList(
            new Parameter("ounit_code", ounitCodes.get(0))
        ),
        400
    );

    testParametersError(
        combination,
        "Request without OUnit IDs and codes, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId)
        ),
        400
    );

    testParametersError(
        combination,
        "Request for one of known OUnit IDs with unknown HEI ID, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", fakeId),
            new Parameter("ounit_id", OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0))
        ),
        400
    );

    testParametersError(
        combination,
        "Request for one of known OUnit Codes with unknown HEI ID, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", fakeId),
            new Parameter("ounit_code", ounitCodes.get(0))
        ),
        400
    );


    testParametersError(
        combination,
        "Request more than <max-ounit-ids> known OUnit IDs, expect 400.",
        concatArrays(
            Arrays.asList(new Parameter("hei_id", this.currentState.selectedHeiId)),
            Collections.nCopies(
                this.currentState.maxOunitIds + 1,
                new Parameter(
                    "ounit_id", OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0))
            )
        ),
        400
    );

    testParametersError(
        combination,
        "Request more than <max-ounit-codes> known OUnit Codes, expect 400.",
        concatArrays(
            Arrays.asList(new Parameter("hei_id", this.currentState.selectedHeiId)),
            Collections.nCopies(
                this.currentState.maxOunitCodes + 1,
                new Parameter("ounit_code", ounitCodes.get(0))
            )
        ),
        400
    );

    testParametersError(
        combination,
        "Request more than <max-ounit-ids> unknown OUnit IDs, expect 400.",
        concatArrays(
            Arrays.asList(new Parameter("hei_id", this.currentState.selectedHeiId)),
            Collections
                .nCopies(this.currentState.maxOunitIds + 1, new Parameter("ounit_id", fakeId))
        ),
        400
    );

    testParametersError(
        combination,
        "Request more than <max-ounit-codes> unknown OUnit Codes, expect 400.",
        concatArrays(
            Arrays.asList(new Parameter("hei_id", this.currentState.selectedHeiId)),
            Collections.nCopies(
                this.currentState.maxOunitCodes + 1,
                new Parameter("ounit_code", fakeId)
            )
        ),
        400
    );

    testParameters200(
        combination,
        "Request exactly <max-ounit-ids> known OUnit IDs, expect 200 and <max-ounit-ids>"
            + " OUnit IDs in response.",
        concatArrays(
            Arrays.asList(new Parameter("hei_id", this.currentState.selectedHeiId)),
            Collections
                .nCopies(
                    this.currentState.maxOunitIds, new Parameter("ounit_id",
                        OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)
                    ))
        ),
        new OUnitIdsVerifier(Collections.nCopies(this.currentState.maxOunitIds,
            OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)
        ))
    );

    testParameters200(
        combination,
        "Request exactly <max-ounit-codes> known OUnit Codes, expect 200 and <max-ounit-codes>"
            + " OUnit Codes in response.",
        concatArrays(
            Arrays.asList(new Parameter("hei_id", this.currentState.selectedHeiId)),
            Collections.nCopies(
                this.currentState.maxOunitCodes,
                new Parameter("ounit_code", ounitCodes.get(0))
            )
        ),
        new OUnitIdsVerifier(
            Collections.nCopies(
                this.currentState.maxOunitCodes,
                OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)
            )
        )
    );

    testParametersError(
        combination,
        "Request with single incorrect parameter, expect 400.",
        Arrays.asList(
            new Parameter(
                "ounit_id_param", OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0))
        ),
        400
    );

    testParameters200(
        combination,
        "Request with additional parameter, expect 200 and one OUnit in response.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("ounit_id", OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)),
            new Parameter(
                "ounit_id_param", OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0))
        ),
        new OUnitIdsVerifier(
            Collections.singletonList(OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)))
    );

    testParametersError(
        combination,
        "Request with correct ounit_id and correct ounit_code, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("ounit_id", OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0)),
            new Parameter("ounit_code", ounitCodes.get(0))
        ),
        400
    );

    testParametersError(
        combination,
        "Request with correct hei_id twice, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("ounit_id", OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0))
        ),
        400
    );

    testParametersError(
        combination,
        "Request with correct hei_id and incorrect hei_id, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", this.currentState.selectedHeiId),
            new Parameter("hei_id", fakeId),
            new Parameter("ounit_id", OUnitsValidationSuiteV200.this.currentState.ounitIds.get(0))
        ),
        400
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

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected KnownElement getKnownElement() {
    return KnownElement.RESPONSE_OUNITS_V2;
  }

  @Override
  protected String getApiNamespace() {
    return KnownNamespace.APIENTRY_OUNITS_V2.getNamespaceUri();
  }

  @Override
  protected String getApiName() {
    return "ounits";
  }

  @Override
  protected String getApiVersion() {
    return "2.0.0";
  }

  @Override
  public String getApiPrefix() {
    return "ou2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "our2";
  }


  private static class OUnitIdsVerifier implements Verifier {
    private final List<String> expectedOUnitIDs;

    OUnitIdsVerifier(List<String> expectedOUnitIDs) {
      this.expectedOUnitIDs = expectedOUnitIDs;
    }

    @Override
    public void verify(AbstractValidationSuite suite, Match root, Response response)
        throws Failure {
      List<String> idsGot = new ArrayList<>();
      String nsPrefix = suite.getApiResponsePrefix() + ":";
      for (Match entry : root.xpath(nsPrefix + "ounit/" + nsPrefix + "ounit-id").each()) {
        idsGot.add(entry.text());
      }
      for (String idGot : idsGot) {
        if (!expectedOUnitIDs.contains(idGot)) {
          throw new Failure(
              "The response has proper HTTP status and it passed the schema validation. However, "
                  + "the set of returned hei-ids doesn't match what we expect. It contains "
                  + "<ounit-id>" + idGot + "</ounit-id>, "
                  + "but it shouldn't. It should contain the following: "
                  + expectedOUnitIDs,
              Status.FAILURE, response
          );
        }
      }
      for (String idExpected : expectedOUnitIDs) {
        if (!idsGot.contains(idExpected)) {
          throw new Failure(
              "The response has proper HTTP status and it passed the schema validation. However, "
                  + "the set of returned ounit_ids doesn't match what we expect. "
                  + "It should contain the following: " + expectedOUnitIDs,
              Status.FAILURE, response
          );
        }
      }
    }
  }

  @SafeVarargs
  public static <T> List<T> concatArrays(List<T>... lists) {
    return Stream.of(lists).flatMap(List::stream).collect(Collectors.toList());
  }
}
