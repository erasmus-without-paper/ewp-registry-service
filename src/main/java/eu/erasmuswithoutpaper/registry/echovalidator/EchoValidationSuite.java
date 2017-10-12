package eu.erasmuswithoutpaper.registry.echovalidator;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.documentbuilder.BuildError;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.echovalidator.InlineValidationStep.Failure;
import eu.erasmuswithoutpaper.registry.echovalidator.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.collect.Lists;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
import org.apache.commons.codec.digest.DigestUtils;
import org.joox.Match;
import org.w3c.dom.Element;

/**
 * Describes the set of test/steps to be run on an Echo API implementation in order to properly
 * validate it.
 */
class EchoValidationSuite {

  /**
   * This is a "fake" {@link ValidationStepWithStatus} which is dynamically added to the list of
   * steps whenever some unexpected runtime exception occurs.
   */
  private static final class GenericErrorFakeStep implements ValidationStepWithStatus {

    private final RuntimeException cause;

    public GenericErrorFakeStep(RuntimeException cause) {
      this.cause = cause;
    }

    @Override
    public Optional<Request> getClientRequest() {
      return Optional.empty();
    }

    @Override
    public String getMessage() {
      return this.cause.getMessage();
    }

    @Override
    public String getName() {
      return "Other error occurred. Please contact the developers.";
    }

    @Override
    public Optional<String> getServerDeveloperErrorMessage() {
      return Optional.empty();
    }

    @Override
    public Optional<Response> getServerResponse() {
      return Optional.empty();
    }

    @Override
    public Status getStatus() {
      return Status.ERROR;
    }
  }

  /**
   * Thrown when a validation step fails so badly, that no other steps should be run.
   */
  @SuppressWarnings("serial")
  private static class SuiteBroken extends Exception {
  }

  private final EchoValidator parentEchoValidator;
  private final String urlToBeValidated;
  private final List<ValidationStepWithStatus> steps;
  private int echoApiVersionDetected = 0;
  private Element matchedApiEntry;
  private List<KnownHttpSecurityMethodsCombination> combinationsToValidate;

  private final EwpDocBuilder docBuilder;
  private final Internet internet;
  private final RegistryClient regClient;

  EchoValidationSuite(EchoValidator echoValidator, EwpDocBuilder docBuilder, Internet internet,
      String urlStr, RegistryClient regClient) {
    this.parentEchoValidator = echoValidator;
    this.urlToBeValidated = urlStr;
    this.steps = new ArrayList<>();
    this.docBuilder = docBuilder;
    this.internet = internet;
    this.regClient = regClient;
  }

  /**
   * Add a new step (to the public list of steps been run) and run it.
   *
   * @param requireSuccess If true, then a {@link SuiteBroken} exception will be raised, if this
   *        steps fails.
   * @param step The step to be added and run.
   * @throws SuiteBroken If the step, which was required to succeed, fails.
   */
  private void addAndRun(boolean requireSuccess, InlineValidationStep step) throws SuiteBroken {
    this.steps.add(step);
    Status status = step.run();
    if (requireSuccess && (status.compareTo(Status.FAILURE) >= 0)) {
      // Note, that NOTICE and WARNING are still acceptable.
      throw new SuiteBroken();
    }
  }

  private Response assertError(Request request, int status) throws Failure {
    return this.assertError(request, Lists.newArrayList(status));
  }

  /**
   * Make the request and make sure that the response contains an error.
   *
   * @param request The request to be made.
   * @param statuses Expected HTTP response statuses (any of those).
   * @throws Failure If HTTP status differs from expected, or if the response body doesn't contain a
   *         proper error response.
   */
  private Response assertError(Request request, List<Integer> statuses) throws Failure {
    try {
      Response response = this.internet.makeRequest(request);
      if (!statuses.contains(response.getStatus())) {
        int gotFirstDigit = response.getStatus() / 100;
        int expectedFirstDigit = statuses.get(0) / 100;
        Status failureStatus =
            (gotFirstDigit == expectedFirstDigit) ? Status.WARNING : Status.FAILURE;
        throw new Failure(
            "HTTP "
                + String.join(" or HTTP ",
                    statuses.stream().map(Object::toString).collect(Collectors.toList()))
                + " expected, but HTTP " + response.getStatus() + " received.",
            failureStatus, response);
      }
      BuildParams params = new BuildParams(response.getBody());
      params.setExpectedKnownElement(KnownElement.COMMON_ERROR_RESPONSE);
      BuildResult result = this.docBuilder.build(params);
      if (!result.isValid()) {
        throw new Failure(
            "HTTP response status was okay, but the content has failed Schema validation. "
                + "It is recommended to return a proper <error-response> in case of errors. "
                + this.formatDocBuildErrors(result.getErrors()),
            Status.WARNING, response);
      }
      if (response.getStatus() == 401) {
        String wwwauth = response.getHeader("WWW-Authenticate");
        if (wwwauth == null) {
          throw new Failure("Per HTTP specs, HTTP 401 responses MUST contain a "
              + "WWW-Authenticate header. See here: "
              + "https://tools.ietf.org/html/rfc7235#section-4.1", Status.FAILURE, response);
        }
        Challenge parsed = Challenge.parse(wwwauth);
        if (parsed != null) {
          if (parsed.getRealm() == null || (!parsed.getRealm().equals("EWP"))) {
            throw new Failure("Your WWW-Authenticate header should contain the \"realm\" property "
                + "with \"EWP\" value.", Status.WARNING, response);
          }
          if (!parsed.getAlgorithms().isEmpty()
              && (!parsed.getAlgorithms().contains(Algorithm.RSA_SHA256))) {
            throw new Failure(
                "Your WWW-Authenticate describes required Signature algorithms, "
                    + "but the list doesn't contain the required rsa-sha256 algorithm.",
                Status.WARNING, response);
          }
          if (!parsed.getHeaders().isEmpty() && (!parsed.getHeaders().containsAll(
              Lists.newArrayList("(request-target)", "host", "digest", "x-request-id", "date")))) {
            throw new Failure(
                "If you want to include the \"headers\" "
                    + "property in your WWW-Authenticate header, then it should contain at least "
                    + "all required values: (request-target), host, digest, x-request-id and date",
                Status.WARNING, response);
          }
        }
        String wantDigest = response.getHeader("Want-Digest");
        if (wantDigest == null || (!wantDigest.contains("SHA-256"))) {
          throw new Failure(
              "It is RECOMMENDED for HTTP 401 responses to contain a proper "
                  + "Want-Digest header with at least the SHA-256 value.",
              Status.WARNING, response);
        }
      }
      return response;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Make the request and make sure that the response contains a valid HTTP 200 Echo API response.
   *
   * @param request The request to be made.
   * @param heiIdsExpected The expected contents of the hei-id list.
   * @param echoValuesExpected The expected contents of the echo list.
   * @throws Failure If some expectations are not met.
   */
  private Response assertHttp200(Request request, List<String> heiIdsExpected,
      List<String> echoValuesExpected) throws Failure {
    try {
      Response response = this.internet.makeRequest(request);
      if (response.getStatus() != 200) {
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP 200 expected, but HTTP " + response.getStatus() + " received.");
        if (response.getStatus() == 403) {
          sb.append(" Make sure you validate clients' credentials against a fresh "
              + "Registry catalogue version.");
        }
        throw new Failure(sb.toString(), Status.FAILURE, response);
      }
      BuildParams params = new BuildParams(response.getBody());
      if (this.echoApiVersionDetected == 1) {
        params.setExpectedKnownElement(KnownElement.RESPONSE_ECHO_V1);
      } else {
        params.setExpectedKnownElement(KnownElement.RESPONSE_ECHO_V2);
      }
      BuildResult result = this.docBuilder.build(params);
      if (!result.isValid()) {
        throw new Failure(
            "HTTP response status was okay, but the content has failed Schema validation. "
                + this.formatDocBuildErrors(result.getErrors()),
            Status.FAILURE, response);
      }
      Match root = $(result.getDocument().get()).namespaces(KnownNamespace.prefixMap());
      List<String> heiIdsGot = new ArrayList<>();
      String nsPrefix = (this.echoApiVersionDetected == 1) ? "er1:" : "er2:";
      for (Match entry : root.xpath(nsPrefix + "hei-id").each()) {
        heiIdsGot.add(entry.text());
      }
      for (String heiIdGot : heiIdsGot) {
        if (!heiIdsExpected.contains(heiIdGot)) {
          throw new Failure(
              "The response has proper HTTP status and it passed the schema validation. However, "
                  + "the set of returned hei-ids doesn't match what we expect. It contains <hei-id>"
                  + heiIdGot + "</hei-id>, but it shouldn't. It should contain the following: "
                  + heiIdsExpected,
              Status.FAILURE, response);
        }
      }
      for (String heiIdExpected : heiIdsExpected) {
        if (!heiIdsGot.contains(heiIdExpected)) {
          throw new Failure(
              "The response has proper HTTP status and it passed the schema validation. However, "
                  + "the set of returned hei-ids doesn't match what we expect. "
                  + "It should contain the following: " + heiIdsExpected,
              Status.FAILURE, response);
        }
      }
      List<String> echoValuesGot = root.xpath(nsPrefix + "echo").texts();
      if (!echoValuesGot.equals(echoValuesExpected)) {
        throw new Failure(
            "The response has proper HTTP status and it passed the schema validation. "
                + "However, there's something wrong with the echo values produced. "
                + "We expected the response to contain the following echo values: "
                + echoValuesExpected + ", but the following values were found instead: "
                + echoValuesGot,
            Status.FAILURE, response);
      }
      return response;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Helper method for creating simple request method validation steps (e.g. run a PUT request and
   * expect error, run a POST and expect success).
   */
  private InlineValidationStep createHttpMethodValidationStep(
      KnownHttpSecurityMethodsCombination combination, String httpMethod, boolean expectSuccess) {
    return new InlineValidationStep() {

      @Override
      public String getName() {
        if (expectSuccess) {
          return "Trying " + combination + " with a " + httpMethod + " request, "
              + "and without any additional parameters. Expecting to "
              + "receive a valid HTTP 200 Echo API response with proper hei-ids, and "
              + "without any echo values.";
        } else {
          return "Trying " + combination + " with a " + httpMethod + " request. "
              + "Expecting to receive a valid HTTP 405 error response.";
        }
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination,
            httpMethod, EchoValidationSuite.this.urlToBeValidated);
        if (expectSuccess) {
          return Optional.of(EchoValidationSuite.this.assertHttp200(this.request,
              EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              Collections.<String>emptyList()));
        } else {
          return Optional.of(EchoValidationSuite.this.assertError(this.request, 405));
        }
      }
    };
  }

  private String formatDocBuildErrors(List<BuildError> errors) {
    StringBuilder sb = new StringBuilder();
    sb.append("Our document parser has reported the following errors:");
    for (int i = 0; i < errors.size(); i++) {
      sb.append("\n" + (i + 1) + ". ");
      sb.append("(Line " + errors.get(i).getLineNumber() + ") " + errors.get(i).getMessage());
    }
    return sb.toString();
  }

  private void validateCombination(KnownHttpSecurityMethodsCombination combination)
      throws SuiteBroken {

    /*
     * Make sure that properly executed GET and POST requests work. This steps varies on particular
     * combination.
     */

    this.addAndRun(false, this.createHttpMethodValidationStep(combination, "GET", true));
    this.addAndRun(false, this.createHttpMethodValidationStep(combination, "POST", true));

    /*
     * Try a couple of invalid client authentication requests, and make sure that the server handles
     * them as expected.
     */

    if (combination.getCliAuth().equals(KnownMethodId.CLIAUTH_TLSCERT_SELFSIGNED)) {

      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Trying " + combination + " with an unknown TLS client certificate "
              + "(a random one, that has never been published in the Registry). "
              + "Expecting to receive a valid HTTP 401 or HTTP 403 error response.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          this.request = EchoValidationSuite.this.createValidRequestForCombination(combination,
              "GET", EchoValidationSuite.this.urlToBeValidated);
          KeyPair otherKeyPair = EchoValidationSuite.this.parentEchoValidator.generateKeyPair();
          X509Certificate otherCert =
              EchoValidationSuite.this.parentEchoValidator.generateCertificate(otherKeyPair);
          this.request.setClientCertificate(otherCert, otherKeyPair);
          return Optional
              .of(EchoValidationSuite.this.assertError(this.request, Lists.newArrayList(401, 403)));
        }
      });
    } else if (combination.getCliAuth().equals(KnownMethodId.CLIAUTH_HTTPSIG)) {
      this.validateHxxx(combination);
    } else {
      // Shouldn't happen.
      throw new RuntimeException("Unsupported combination");
    }


    /////////////////////////////////////////

    this.addAndRun(false, this.createHttpMethodValidationStep(combination, "PUT", false));

    /////////////////////////////////////////

    this.addAndRun(false, this.createHttpMethodValidationStep(combination, "DELETE", false));

    /////////////////////////////////////////

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " GET request with a list of echo values [a, b, a]. "
            + "Expecting to receive a valid HTTP 200 Echo API response, "
            + "with proper hei-id and matching echo values.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        ArrayList<String> expectedEchoValues = new ArrayList<>();
        expectedEchoValues.add("a");
        expectedEchoValues.add("b");
        expectedEchoValues.add("a");
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated + "?echo=a&echo=b&echo=a");
        return Optional.of(EchoValidationSuite.this.assertHttp200(this.request,
            EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(), expectedEchoValues));
      }
    });

    /////////////////////////////////////////

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " POST request with a list of echo values [a, b, a]. "
            + "Expecting to receive a valid HTTP 200 Echo API response, "
            + "with proper hei-id and matching echo values.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination,
            "POST", EchoValidationSuite.this.urlToBeValidated);
        this.request.setBody("echo=a&echo=b&echo=a".getBytes(StandardCharsets.UTF_8));
        ArrayList<String> expectedEchoValues = new ArrayList<>();
        expectedEchoValues.add("a");
        expectedEchoValues.add("b");
        expectedEchoValues.add("a");
        if (combination.getCliAuth().equals(KnownMethodId.CLIAUTH_HTTPSIG)) {
          // We have changed the body, so we need to regenerate the digest and signature.
          this.request.recomputeAndAttachDigestHeader();
          String keyId = Authorization.parse(this.request.getHeader("Authorization")).getKeyId();
          this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId,
              EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse(),
              Lists.newArrayList("(request-target)", "date", "host", "digest", "x-request-id"));
        }
        return Optional.of(EchoValidationSuite.this.assertHttp200(this.request,
            EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(), expectedEchoValues));
      }
    });

    /////////////////////////////////////////
  }

  private void validateHxxx(KnownHttpSecurityMethodsCombination combination) throws SuiteBroken {
    assert combination.getCliAuth().equals(KnownMethodId.CLIAUTH_HTTPSIG);

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with an unknown RSA client key "
            + "(a random one, that has never been published in the Registry). "
            + "Expecting to receive a valid HTTP 401 or HTTP 403 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        KeyPair otherKeyPair = EchoValidationSuite.this.parentEchoValidator.generateKeyPair();
        // Replace the previously set Authorization header with a different one.
        this.request.recomputeAndAttachHttpSigAuthorizationHeader("Unknown-ID", otherKeyPair,
            Lists.newArrayList("(request-target)", "host", "date", "digest", "x-request-id"));
        return Optional
            .of(EchoValidationSuite.this.assertError(this.request, Lists.newArrayList(401, 403)));
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with a known keyId, but invalid signature. "
            + "Expecting to receive a valid HTTP 400 or HTTP 401 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        // Leave keyId as is, but use a new random key for signature generation.
        String keyId = Authorization.parse(this.request.getHeader("Authorization")).getKeyId();
        KeyPair otherKeyPair = EchoValidationSuite.this.parentEchoValidator.generateKeyPair();
        // Replace the previously set Authorization header with a different one.
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId, otherKeyPair,
            Lists.newArrayList("(request-target)", "host", "date", "digest", "x-request-id"));
        return Optional
            .of(EchoValidationSuite.this.assertError(this.request, Lists.newArrayList(400, 401)));
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with missing headers that were supposed to be signed. "
            + "Expecting to receive a valid HTTP 400 or HTTP 401 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        String keyId = Authorization.parse(this.request.getHeader("Authorization")).getKeyId();
        KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse();
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId, keyPair,
            Lists.newArrayList("(request-target)", "host", "date", "digest", "x-request-id",
                "missing-header-that-should-exist"));
        return Optional
            .of(EchoValidationSuite.this.assertError(this.request, Lists.newArrayList(400, 401)));
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with Original-Date (instead of Date). "
            + "Expecting to receive a valid HTTP 200 response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        String value = this.request.getHeader("Date");
        this.request.removeHeader("Date");
        this.request.putHeader("Original-Date", value);
        String keyId = Authorization.parse(this.request.getHeader("Authorization")).getKeyId();
        KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse();
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId, keyPair, Lists
            .newArrayList("(request-target)", "host", "original-date", "digest", "x-request-id"));
        return Optional.of(EchoValidationSuite.this.assertHttp200(this.request,
            EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(), Lists.newArrayList()));
      }
    });

    List<String> stdHeaders =
        Lists.newArrayList("(request-target)", "host", "date", "digest", "x-request-id");
    for (String headerToSkip : stdHeaders) {
      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Trying " + combination + " with unsigned " + headerToSkip + " header. "
              + "Expecting to receive a valid HTTP 400 or HTTP 401 error response.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          this.request = EchoValidationSuite.this.createValidRequestForCombination(combination,
              "GET", EchoValidationSuite.this.urlToBeValidated);
          List<String> headersToSign = new ArrayList<>(stdHeaders);
          headersToSign.remove(headerToSkip);
          String keyId = Authorization.parse(this.request.getHeader("Authorization")).getKeyId();
          KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse();
          this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId, keyPair, headersToSign);
          return Optional
              .of(EchoValidationSuite.this.assertError(this.request, Lists.newArrayList(400, 401)));
        }
      });
    }

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with some extra unknown, but properly signed headers. "
            + "Expecting to receive a valid HTTP 200 response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        this.request.putHeader("Some-Custom-Header", "Value");
        Authorization authz = Authorization.parse(this.request.getHeader("Authorization"));
        String keyId = Authorization.parse(this.request.getHeader("Authorization")).getKeyId();
        KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse();
        List<String> headersToSign = new ArrayList<>(authz.getHeaders());
        headersToSign.add("some-custom-header");
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId, keyPair, headersToSign);
        return Optional.of(EchoValidationSuite.this.assertHttp200(this.request,
            EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(), Lists.newArrayList()));
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " signed with a server key, instead of a client key. "
            + "Expecting to receive a valid HTTP 403 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        Authorization authz = Authorization.parse(this.request.getHeader("Authorization"));
        KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getServerRsaKeyPairInUse();
        String keyId = DigestUtils.sha256Hex(keyPair.getPublic().getEncoded());
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId, keyPair,
            authz.getHeaders());
        return Optional.of(EchoValidationSuite.this.assertError(this.request, 403));
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with an unsynchronized clock "
            + "(Original-Date 20 minutes in the past). "
            + "Expecting to receive a valid HTTP 400 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
        dateTime = dateTime.minusMinutes(20);
        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(dateTime);
        this.request.putHeader("Original-Date", date);

        Authorization authz = Authorization.parse(this.request.getHeader("Authorization"));
        List<String> headers = new ArrayList<>(authz.getHeaders());
        headers.add("original-date");
        headers.remove("date");
        String keyId = Authorization.parse(this.request.getHeader("Authorization")).getKeyId();
        KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse();
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId, keyPair, headers);
        return Optional.of(EchoValidationSuite.this.assertError(this.request, 400));
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with non-canonical X-Request-ID. "
            + "Expecting to receive a valid HTTP 400 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        this.request.putHeader("X-Request-Id",
            this.request.getHeader("X-Request-Id").replaceAll("-", "").toUpperCase(Locale.US));
        Authorization authz = Authorization.parse(this.request.getHeader("Authorization"));
        KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse();
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(authz.getKeyId(), keyPair,
            authz.getHeaders());
        try {
          return Optional.of(EchoValidationSuite.this.assertError(this.request, 400));
        } catch (Failure f) {
          // We want this to be a warning, not a failure.
          throw new Failure(f.getMessage(), Status.WARNING,
              f.getAttachedServerResponse().orElse(null));
        }
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with an invalid Digest. "
            + "Expecting to receive a valid HTTP 400 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination,
            "POST", EchoValidationSuite.this.urlToBeValidated);
        this.request.setBody("echo=a&echo=b&echo=a".getBytes(StandardCharsets.UTF_8));
        this.request.recomputeAndAttachDigestHeader();
        // This digest is valid for the previous body only.
        this.request.setBody("echo=a&echo=b&echo=c".getBytes(StandardCharsets.UTF_8));
        Authorization authz = Authorization.parse(this.request.getHeader("Authorization"));
        KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse();
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(authz.getKeyId(), keyPair,
            authz.getHeaders());
        return Optional.of(EchoValidationSuite.this.assertError(this.request, 400));
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with multiple Digests (one of which is SHA-256). "
            + "Expecting to receive a valid HTTP 200 response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination,
            "POST", EchoValidationSuite.this.urlToBeValidated);
        this.request.setBody("echo=b&echo=b".getBytes(StandardCharsets.UTF_8));
        this.request.recomputeAndAttachDigestHeader();
        // Add a second Digest
        String digestHeader = this.request.getHeader("Digest");
        digestHeader += ",Unknown-Digest-Algorithm=SomeValue";
        this.request.putHeader("Digest", digestHeader);
        Authorization authz = Authorization.parse(this.request.getHeader("Authorization"));
        KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse();
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(authz.getKeyId(), keyPair,
            authz.getHeaders());
        return Optional.of(EchoValidationSuite.this.assertHttp200(this.request,
            EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
            Lists.newArrayList("b", "b")));
      }
    });
  }

  protected Request createValidRequestForCombination(
      KnownHttpSecurityMethodsCombination combination, String httpMethod, String url) {

    Request request = new Request(httpMethod, url);

    if (httpMethod.equals("POST")) {
      request.putHeader("Content-Type", "application/x-www-form-urlencoded");
    }

    // cliauth

    if (combination.getCliAuth().equals(KnownMethodId.CLIAUTH_NONE)) {
      // pass
    } else if (combination.getCliAuth().equals(KnownMethodId.CLIAUTH_TLSCERT_SELFSIGNED)) {
      request.setClientCertificate(this.parentEchoValidator.getTlsClientCertificateInUse(),
          this.parentEchoValidator.getTlsKeyPairInUse());
    } else if (combination.getCliAuth().equals(KnownMethodId.CLIAUTH_HTTPSIG)) {
      String date =
          DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneId.of("UTC")));
      URL parsed;
      try {
        parsed = new URL(url);
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      }
      request.putHeader("Host", parsed.getHost());
      request.putHeader("Date", date);
      request.putHeader("X-Request-Id", UUID.randomUUID().toString());
      request.recomputeAndAttachDigestHeader();
      KeyPair myKeyPair = this.parentEchoValidator.getClientRsaKeyPairInUse();
      String myKeyId = DigestUtils.sha256Hex(myKeyPair.getPublic().getEncoded());
      request.recomputeAndAttachHttpSigAuthorizationHeader(myKeyId, myKeyPair,
          Lists.newArrayList("(request-target)", "host", "date", "digest", "x-request-id"));
    } else {
      throw new RuntimeException("Not supported");
    }

    // srvauth

    if (combination.getSrvAuth().equals(KnownMethodId.SRVAUTH_TLSCERT)) {
      // pass
    } else {
      throw new RuntimeException("Not supported");
    }

    // reqencr

    if (combination.getReqEncr().equals(KnownMethodId.REQENCR_TLS)) {
      // pass
    } else {
      throw new RuntimeException("Not supported");
    }

    // resencr

    if (combination.getResEncr().equals(KnownMethodId.RESENCR_TLS)) {
      // pass
    } else {
      throw new RuntimeException("Not supported");
    }

    return request;
  }

  List<ValidationStepWithStatus> getResults() {
    return this.steps;
  }

  void run() {

    try {

      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Check if our TLS client certificate has been served long enough.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          if (new Date().getTime() - EchoValidationSuite.this.parentEchoValidator
              .getCredentialsGenerationDate().getTime() < 10 * 60 * 1000) {
            throw new Failure(
                "Our TLS client certificate is quite fresh. This means that many Echo APIs will "
                    + "(correctly) return error responses in places where we expect HTTP 200. "
                    + "This notice will disappear once the certificate is 10 minutes old.",
                Status.NOTICE, null);
          }
          return Optional.empty();
        }
      });

      /////////////////////////////////////////

      this.addAndRun(true, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Verifying the format of the URL. Expecting a valid HTTPS-scheme URL.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          if (!EchoValidationSuite.this.urlToBeValidated.startsWith("https://")) {
            throw new Failure("It needs to be HTTPS.", Status.FAILURE, null);
          }
          try {
            new URL(EchoValidationSuite.this.urlToBeValidated);
          } catch (MalformedURLException e) {
            throw new Failure("Exception while parsing URL format: " + e, Status.FAILURE, null);
          }
          return Optional.empty();
        }
      });

      /////////////////////////////////////////

      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Accessing your Echo API without any form of client authentication. "
              + "Expecting to receive a valid HTTP 401 or HTTP 403 error response.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          this.request = new Request("GET", EchoValidationSuite.this.urlToBeValidated);
          return Optional
              .of(EchoValidationSuite.this.assertError(this.request, Lists.newArrayList(401, 403)));
        }
      });

      /////////////////////////////////////////

      this.addAndRun(true, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Verifying if the URL is properly registered.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          final String NS1 = KnownNamespace.APIENTRY_ECHO_V1.getNamespaceUri();
          final String NS2 = KnownNamespace.APIENTRY_ECHO_V2.getNamespaceUri();
          ApiSearchConditions conds = new ApiSearchConditions();
          conds.setApiClassRequired(NS1, "echo", "1.0.0");
          Collection<Element> v1entries = EchoValidationSuite.this.regClient.findApis(conds);
          conds.setApiClassRequired(NS2, "echo", "2.0.0");
          Collection<Element> v2entries = EchoValidationSuite.this.regClient.findApis(conds);
          int matchedApiEntries = 0;
          for (Element entry : v1entries) {
            if ($(entry).find("url").text().equals(EchoValidationSuite.this.urlToBeValidated)) {
              EchoValidationSuite.this.echoApiVersionDetected = 1;
              EchoValidationSuite.this.matchedApiEntry = entry;
              matchedApiEntries++;
            }
          }
          for (Element entry : v2entries) {
            if ($(entry).find("url").text().equals(EchoValidationSuite.this.urlToBeValidated)) {
              EchoValidationSuite.this.echoApiVersionDetected = 2;
              EchoValidationSuite.this.matchedApiEntry = entry;
              matchedApiEntries++;
            }
          }
          if (EchoValidationSuite.this.echoApiVersionDetected == 0) {
            throw new Failure("Could not find this URL in the Registry Catalogue. "
                + "Make sure that it is properly registered "
                + "(as declared in Echo API's `manifest-entry.xsd` file): "
                + EchoValidationSuite.this.urlToBeValidated, Status.FAILURE, null);
          }
          if (matchedApiEntries > 1) {
            throw new Failure(
                "Multiple (" + Integer.toString(matchedApiEntries)
                    + ") API entries found for this URL. "
                    + "Results of the remaining tests might be non-determinictic.",
                Status.WARNING, null);
          }
          if (EchoValidationSuite.this.echoApiVersionDetected == 1) {
            throw new Failure(
                "Version 1 of Echo API is deprecated. " + "You should implement Version 2 instead.",
                Status.WARNING, null);
          }
          return Optional.empty();
        }
      });

      if (this.echoApiVersionDetected == 1) {
        this.validateCombination(new KnownHttpSecurityMethodsCombination(
            KnownMethodId.CLIAUTH_TLSCERT_SELFSIGNED, KnownMethodId.SRVAUTH_TLSCERT,
            KnownMethodId.REQENCR_TLS, KnownMethodId.RESENCR_TLS));
      } else if (this.echoApiVersionDetected == 2) {

        this.addAndRun(true, new InlineValidationStep() {

          @Override
          public String getName() {
            return "Querying for supported security methods. Validating http-security integrity.";
          }

          @Override
          protected Optional<Response> innerRun() throws Failure {

            List<String> notices = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            // Parse http-security element. Record all warnings.

            Element httpSecurityElem = $(EchoValidationSuite.this.matchedApiEntry)
                .namespaces(KnownNamespace.prefixMap()).xpath("e2:http-security").get(0);
            HttpSecuritySettings sec = new HttpSecuritySettings(httpSecurityElem);
            warnings.addAll(sec.getWarnings());

            // We will create four lists of security methods - one per type. Each added item
            // will represent one "validatable" security method.

            // cliauth

            List<KnownMethodId> cliAuthMethodsToValidate = new ArrayList<>();
            if (sec.supportsCliAuthNone()) {
              warnings.add("Anonymous Client Authentication SHOULD NOT be enabled for Echo API.");
            }
            if (sec.supportsCliAuthTlsCert()) {
              if (sec.supportsCliAuthTlsCertSelfSigned()) {
                cliAuthMethodsToValidate.add(KnownMethodId.CLIAUTH_TLSCERT_SELFSIGNED);
              } else {
                notices.add("Echo API Validator is able to validate TLS Client Authentication "
                    + "ONLY with a self-signed client certificate. You Echo API endpoint declares "
                    + "that it does not support self-signed Client Certificates. Therefore, TLS "
                    + "Client Authentication tests will be skipped.");
              }
            } else {
              notices.add("This endpoint does not support TLS Certificate Client Authentication. "
                  + "It is not required to support it, but it might be a good idea to keep "
                  + "supporting it for some time (until all clients begin to support HTTPSIG).");
            }
            if (sec.supportsCliAuthHttpSig()) {
              cliAuthMethodsToValidate.add(KnownMethodId.CLIAUTH_HTTPSIG);
            } else {
              warnings.add("It is RECOMMENDED for all EWP server endpoints to support HTTP "
                  + "Signature Client Authentication. Your endpoint doesn't.");
            }
            if (cliAuthMethodsToValidate.size() == 0) {
              errors.add("Your Echo API does not support ANY of the client authentication "
                  + "methods recognized by the Validator.");
            }

            // srvauth

            List<KnownMethodId> srvAuthMethodsToValidate = new ArrayList<>();
            if (sec.supportsSrvAuthTlsCert()) {
              srvAuthMethodsToValidate.add(KnownMethodId.SRVAUTH_TLSCERT);
            }
            if (sec.supportsSrvAuthHttpSig()) {
              notices.add("Echo API Validator does not yet support testing HTTP Signature "
                  + "Server Authentication. Support is planned be implemented soon.");
            }
            if (srvAuthMethodsToValidate.size() == 0) {
              errors.add("Your Echo API does not support ANY of the server authentication "
                  + "methods recognized by the Validator.");
            }

            // reqencr

            List<KnownMethodId> reqEncrMethodsToValidate = new ArrayList<>();
            if (sec.supportsReqEncrTls()) {
              reqEncrMethodsToValidate.add(KnownMethodId.REQENCR_TLS);
            }
            if (reqEncrMethodsToValidate.size() == 0) {
              errors.add("Your Echo API does not support ANY of the request encryption "
                  + "methods recognized by the Validator.");
            }

            // resencr

            List<KnownMethodId> resEncrMethodsToValidate = new ArrayList<>();
            if (sec.supportsResEncrTls()) {
              resEncrMethodsToValidate.add(KnownMethodId.RESENCR_TLS);
            }
            if (resEncrMethodsToValidate.size() == 0) {
              errors.add("Your Echo API does not support ANY of the response encryption "
                  + "methods recognized by the Validator.");
            }

            // Generate all possible combinations of validatable security methods.

            EchoValidationSuite.this.combinationsToValidate = new ArrayList<>();
            for (KnownMethodId cliauth : cliAuthMethodsToValidate) {
              for (KnownMethodId srvauth : srvAuthMethodsToValidate) {
                for (KnownMethodId reqencr : reqEncrMethodsToValidate) {
                  for (KnownMethodId resencr : resEncrMethodsToValidate) {
                    EchoValidationSuite.this.combinationsToValidate
                        .add(new KnownHttpSecurityMethodsCombination(cliauth, srvauth, reqencr,
                            resencr));
                  }
                }
              }
            }

            // Determine the status. If not success, then raise a proper exception.

            StringBuilder sb = new StringBuilder();
            Status status = Status.SUCCESS;
            if (errors.size() > 0) {
              status = Status.ERROR;
              sb.append("Errors:\n");
              for (String message : errors) {
                sb.append("- ").append(message).append('\n');
              }
              sb.append('\n');
            }
            if (warnings.size() > 0) {
              if (status.equals(Status.SUCCESS)) {
                status = Status.WARNING;
              }
              sb.append("Warnings:\n");
              for (String message : warnings) {
                sb.append("- ").append(message).append('\n');
              }
              sb.append('\n');
            }
            if (notices.size() > 0) {
              if (status.equals(Status.SUCCESS)) {
                status = Status.NOTICE;
              }
              sb.append("Notices:\n");
              for (String message : notices) {
                sb.append("- ").append(message).append('\n');
              }
              sb.append('\n');
            }
            if (!status.equals(Status.SUCCESS)) {
              throw new Failure(sb.toString(), status, null);
            }

            return Optional.empty();
          }
        });

        for (KnownHttpSecurityMethodsCombination combination : this.combinationsToValidate) {
          this.validateCombination(combination);
        }
      }

    } catch (SuiteBroken e) {
      // Ignore.
    } catch (RuntimeException e) {
      this.steps.add(new GenericErrorFakeStep(e));
    }
  }

}
