package eu.erasmuswithoutpaper.registry.validators.institutionsvalidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.CombEntry;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep.Failure;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.echovalidator.HttpSecuritySettings;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.collect.Lists;

import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Describes the set of test/steps to be run on an Institutions API implementation in order to
 * properly validate it.
 */
class InstitutionsValidationSuite extends AbstractValidationSuite {

  private static final Logger logger = LoggerFactory.getLogger(InstitutionsValidationSuite.class);
  private int maxHeiIds;

  @Override
  protected Logger getLogger() {
    return logger;
  }

  @Override
  protected KnownElement getKnownElement() {
    return KnownElement.RESPONSE_INSTITUTIONS_V2;
  }

  InstitutionsValidationSuite(InstitutionsValidator validator, EwpDocBuilder docBuilder,
      Internet internet,
      String urlStr, RegistryClient regClient, ManifestRepository repo) {
    super(validator, docBuilder, internet, urlStr, regClient, repo);
  }

  @Override
  public List<ValidationStepWithStatus> getResults() {
    return this.steps;
  }

  @Override
  protected void runTests() throws SuiteBroken {
    validateSecurityMethods();
    for (Combination combination : this.combinationsToValidate) {
      this.validateCombination(combination);
    }
  }

  private int getMaxHeiIds() {
    return getMaxIds("hei-ids");
  }

  @Override
  protected List<CombEntry> getResponseEncryptionMethods(
      HttpSecuritySettings sec, List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsResEncrTls()) {
      ret.add(CombEntry.RESENCR_TLS);
    }
    if (sec.supportsResEncrEwp()) {
      ret.add(CombEntry.RESENCR_EWP);
      warnings.add("It is RECOMMENDED to support only TLS response encryption");
    }
    if (ret.size() == 0) {
      errors.add("Your Echo API does not support ANY of the response encryption "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

  @Override
  protected List<CombEntry> getRequestEncryptionMethods(
      HttpSecuritySettings sec, List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsReqEncrTls()) {
      ret.add(CombEntry.REQENCR_TLS);
    }
    if (sec.supportsReqEncrEwp()) {
      ret.add(CombEntry.REQENCR_EWP);
      warnings.add("It is RECOMMENDED to support only TLS request encryption");
    }
    if (ret.size() == 0) {
      errors.add("Your Echo API does not support ANY of the request encryption "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

  @Override
  protected List<CombEntry> getServerAuthenticationMethods(
      HttpSecuritySettings sec, List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsSrvAuthTlsCert()) {
      ret.add(CombEntry.SRVAUTH_TLSCERT);
    }
    if (sec.supportsSrvAuthHttpSig()) {
      ret.add(CombEntry.SRVAUTH_HTTPSIG);
      if (!sec.supportsSrvAuthTlsCert()) {
        warnings.add("Server which support HTTP Signature Server Authentication "
            + "SHOULD also support TLS Server Certificate Authentication");
      }
    } else {
      notices.add("It is RECOMMENDED for all servers to support "
          + "HTTP Signature Server Authentication.");
    }
    if (ret.size() == 0) {
      errors.add("Your Echo API does not support ANY of the server authentication "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

  @Override
  protected List<CombEntry> getClientAuthenticationMethods(
      HttpSecuritySettings sec, List<String> notices, List<String> warnings, List<String> errors) {
    List<CombEntry> ret = new ArrayList<>();
    if (!sec.supportsCliAuthNone()) {
      notices.add("You may consider allowing this API to by accessed by anonymous clients.");
    } else {
      ret.add(CombEntry.CLIAUTH_NONE);
    }

    if (sec.supportsCliAuthTlsCert()) {
      if (sec.supportsCliAuthTlsCertSelfSigned()) {
        ret.add(CombEntry.CLIAUTH_TLSCERT_SELFSIGNED);
      } else {
        notices.add("Not implemented."); //TODO
      }
    }

    if (sec.supportsCliAuthHttpSig()) {
      ret.add(CombEntry.CLIAUTH_HTTPSIG);
    } else {
      warnings.add("It is RECOMMENDED for all EWP server endpoints to support HTTP "
          + "Signature Client Authentication. Your endpoint doesn't.");
    }

    if (ret.size() < 1) {
      errors.add("Your API does not support ANY of the client authentication "
          + "methods recognized by the Validator.");
    }
    return ret;
  }

  private void validateCombinationAny(Combination combination) throws SuiteBroken {
    this.maxHeiIds = getMaxHeiIds();
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
      protected Optional<Response> innerRun() throws Failure {
        String url = InstitutionsValidationSuite.this.urlToBeValidated;
        List<String> coveredHeiIds =
            InstitutionsValidationSuite.this.fetchHeiIdsCoveredByApiByUrl(url);
        if (coveredHeiIds.isEmpty()) {
          throw new InlineValidationStep.Failure(
              "Manifest file doesn't contain any <hei-id> field. We cannot preform tests.",
              ValidationStepWithStatus.Status.FAILURE, null);
        }
        heis.addAll(coveredHeiIds);
        return Optional.empty();
      }
    });

    testParameters200(combination,
        "Request for one of known HEI IDs, expect 200 OK.",
        Arrays.asList(new Parameter("hei_id", heis.get(0))),
        new InstitutionsVerifier(Collections.singletonList(heis.get(0)))
    );

    testParameters200(combination,
        "Request one unknown HEI ID, expect 200 and empty response.",
        Collections.singletonList(new Parameter("hei_id", fakeHeiId)),
        new InstitutionsVerifier(new ArrayList<>())
    );

    if (maxHeiIds > 1) {
      testParameters200(combination,
          "Request one known and one unknown HEI ID, expect 200 and only one HEI in response.",
          Arrays.asList(
              new Parameter("hei_id", heis.get(0)),
              new Parameter("hei_id", fakeHeiId)
          ),
          new InstitutionsVerifier(Collections.singletonList(heis.get(0)))
      );
    }

    testParametersError(combination,
        "Request without HEI IDs, expect 400.",
        new ArrayList<>(),
        400
    );

    testParametersError(combination,
        "Request more than <max-hei-ids> known HEIs, expect 400.",
        Collections.nCopies(maxHeiIds + 1, new Parameter("hei_id", heis.get(0))),
        400
    );

    testParametersError(combination,
        "Request more than <max-hei-ids> unknown HEI IDs, expect 400.",
        Collections.nCopies(maxHeiIds + 1, new Parameter("hei_id", fakeHeiId)),
        400
    );

    testParameters200(combination,
        "Request exactly <max-hei-ids> known HEI IDs, "
            + "expect 200 and <max-hei-ids> HEI IDs in response.",
        Collections.nCopies(maxHeiIds, new Parameter("hei_id", heis.get(0))),
        new InstitutionsVerifier(Collections.nCopies(maxHeiIds, heis.get(0)))
    );

    testParametersError(combination,
        "Request with single incorrect parameter, expect 400.",
        Arrays.asList(new Parameter("hei_id_param", heis.get(0))),
        400
    );

    testParametersError(combination,
        "Request with additional parameter, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", heis.get(0)),
            new Parameter("hei_id_param", heis.get(0))
        ),
        400
    );
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
                  + expectedHeiIDs,
              Status.FAILURE, response);
        }
      }
      for (String expectedId : expectedHeiIDs) {
        if (!receivedHeiIds.contains(expectedId)) {
          throw new Failure(
              "The response has proper HTTP status and it passed the schema validation. However, "
                  + "the set of returned hei-ids doesn't match what we expect. "
                  + "It should contain the following: " + expectedHeiIDs,
              Status.FAILURE, response);
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
                  + "root-ounit-id is not included in ounit-id list.",
              Status.FAILURE, response);
        }
      }
    }
  }

  @Override
  protected void validateCombinationPost(Combination combination) throws SuiteBroken {
    this.addAndRun(false,
        this.createHttpMethodValidationStep(combination.withChangedHttpMethod("PUT")));
    this.addAndRun(false, this
        .createHttpMethodValidationStep(combination.withChangedHttpMethod("DELETE")));
    validateCombinationAny(combination);
  }

  @Override
  protected void validateCombinationGet(Combination combination) throws SuiteBroken {
    validateCombinationAny(combination);
  }

  @Override
  protected List<ApiVersionDescription> getApiVersions() {
    return Lists.newArrayList(
        new ApiVersionDescription(
            "1.0.0",
            KnownNamespace.APIENTRY_INSTITUTIONS_V1.getNamespaceUri(),
            "institutions",
            ApiVersionStatus.DEPRECATED
        ),
        new ApiVersionDescription(
            "2.0.0",
            KnownNamespace.APIENTRY_INSTITUTIONS_V2.getNamespaceUri(),
            "institutions",
            ApiVersionStatus.ACTIVE
        )
    );
  }

  @Override
  public String getApiPrefix() {
    return "in2";
  }

  @Override
  public String getApiResponsePrefix() {
    return "inr2";
  }
}
