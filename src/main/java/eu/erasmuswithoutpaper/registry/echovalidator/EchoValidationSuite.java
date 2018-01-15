package eu.erasmuswithoutpaper.registry.echovalidator;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildError;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.echovalidator.InlineValidationStep.Failure;
import eu.erasmuswithoutpaper.registry.echovalidator.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.internet.HttpSigRsaPublicKey;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.collect.Lists;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
import net.adamcin.httpsig.api.DefaultKeychain;
import net.adamcin.httpsig.api.DefaultVerifier;
import net.adamcin.httpsig.api.RequestContent;
import net.adamcin.httpsig.api.VerifyResult;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger logger = LoggerFactory.getLogger(EchoValidationSuite.class);

  private final EchoValidator parentEchoValidator;
  private final String urlToBeValidated;
  private final List<ValidationStepWithStatus> steps;
  private int echoApiVersionDetected = 0;
  private Element matchedApiEntry;
  private List<SecMethodsCombination> combinationsToValidate;

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

  private void checkEdgeCasesForAxxx(SecMethodsCombination combination) throws SuiteBroken {
    assert combination.getCliAuth().equals(SecMethod.CLIAUTH_NONE);

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " (no client authentication). "
            + "Expecting a valid HTTP 401 or HTTP 403 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(combination,
            this.request, Lists.newArrayList(401, 403)));
      }
    });
  }

  private void checkEdgeCasesForHxxx(SecMethodsCombination combination) throws SuiteBroken {
    assert combination.getCliAuth().equals(SecMethod.CLIAUTH_HTTPSIG);

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
        KeyPair otherKeyPair =
            EchoValidationSuite.this.parentEchoValidator.getUnregisteredKeyPair();
        // Replace the previously set Authorization header with a different one.
        this.request.recomputeAndAttachHttpSigAuthorizationHeader("Unknown-ID", otherKeyPair,
            Lists.newArrayList("(request-target)", "host", "date", "digest", "x-request-id"));
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(combination,
            this.request, Lists.newArrayList(401, 403)));
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
        KeyPair otherKeyPair =
            EchoValidationSuite.this.parentEchoValidator.getUnregisteredKeyPair();
        // Replace the previously set Authorization header with a different one.
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId, otherKeyPair,
            Lists.newArrayList("(request-target)", "host", "date", "digest", "x-request-id"));
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(combination,
            this.request, Lists.newArrayList(400, 401)));
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
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(combination,
            this.request, Lists.newArrayList(400, 401)));
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
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(combination,
            this.request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
            Lists.newArrayList()));
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
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(combination,
              this.request, Lists.newArrayList(400, 401)));
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
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(combination,
            this.request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
            Lists.newArrayList()));
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
        return Optional
            .of(EchoValidationSuite.this.makeRequestAndExpectError(combination, this.request, 403));
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
        try {
          return Optional.of(
              EchoValidationSuite.this.makeRequestAndExpectError(combination, this.request, 400));
        } catch (Failure f) {
          if (f.getAttachedServerResponse().isPresent()
              && f.getAttachedServerResponse().get().getStatus() == 200) {
            throw f.withChangedStatus(Status.WARNING);
          } else {
            throw f;
          }
        }
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
          return Optional.of(
              EchoValidationSuite.this.makeRequestAndExpectError(combination, this.request, 400));
        } catch (Failure f) {
          // We don't want this to be a FAILURE. WARNING is enough.
          if (f.getStatus().equals(Status.FAILURE)) {
            throw new Failure(f.getMessage(), Status.WARNING,
                f.getAttachedServerResponse().orElse(null));
          } else {
            throw f;
          }
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
        return Optional
            .of(EchoValidationSuite.this.makeRequestAndExpectError(combination, this.request, 400));
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
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(combination,
            this.request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
            Lists.newArrayList("b", "b")));
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with \"SHA\" request digest. "
            + "This algorithm is deprecated, so we are expecting to receive a "
            + "valid HTTP 400 response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination,
            "POST", EchoValidationSuite.this.urlToBeValidated);
        this.request.setBody("echo=b&echo=b".getBytes(StandardCharsets.UTF_8));
        this.request.putHeader("Digest", "SHA=" + Base64.getEncoder()
            .encodeToString(DigestUtils.getSha1Digest().digest(this.request.getBodyOrEmpty())));
        Authorization authz = Authorization.parse(this.request.getHeader("Authorization"));
        KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse();
        this.request.recomputeAndAttachHttpSigAuthorizationHeader(authz.getKeyId(), keyPair,
            authz.getHeaders());
        try {
          return Optional.of(
              EchoValidationSuite.this.makeRequestAndExpectError(combination, this.request, 400));
        } catch (Failure f) {
          if (f.getAttachedServerResponse().isPresent()
              && f.getAttachedServerResponse().get().getStatus() == 200) {
            throw f.withChangedStatus(Status.WARNING);
          } else {
            throw f;
          }
        }
      }
    });
  }

  private void checkEdgeCasesForSxxx(SecMethodsCombination combination) throws SuiteBroken {
    assert combination.getCliAuth().equals(SecMethod.CLIAUTH_TLSCERT_SELFSIGNED);

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with an unknown TLS client certificate "
            + "(a random one, that has never been published in the Registry). "
            + "Expecting to receive a valid HTTP 401 or HTTP 403 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        KeyPair otherKeyPair =
            EchoValidationSuite.this.parentEchoValidator.getUnregisteredKeyPair();
        X509Certificate otherCert =
            EchoValidationSuite.this.parentEchoValidator.generateCertificate(otherKeyPair);
        this.request.setClientCertificate(otherCert, otherKeyPair);
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(combination,
            this.request, Lists.newArrayList(401, 403)));
      }
    });
  }

  private void checkEdgeCasesForxHxx(SecMethodsCombination combination) throws SuiteBroken {

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with no valid algorithm in Accept-Signature header. "
            + "Expecting to receive unsigned response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        this.request.putHeader("Accept-Signature", "unknown-algorithm");
        SecMethodsCombination relaxedCombination =
            new SecMethodsCombination(combination.getCliAuth(), SecMethod.SRVAUTH_TLSCERT,
                combination.getReqEncr(), combination.getResEncr());
        if (combination.getCliAuth().equals(SecMethod.CLIAUTH_NONE)) {
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(relaxedCombination,
              this.request, Lists.newArrayList(401, 403)));
        } else {
          return Optional
              .of(EchoValidationSuite.this.makeRequestAndExpectHttp200(relaxedCombination,
                  this.request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
                  Lists.newArrayList()));
        }
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with multiple algorithms in Accept-Signature header "
            + "(one of which is rsa-sha256). Expecting to receive a signed response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        this.request = EchoValidationSuite.this.createValidRequestForCombination(combination, "GET",
            EchoValidationSuite.this.urlToBeValidated);
        this.request.putHeader("Accept-Signature", "rsa-sha256, unknown-algorithm");
        if (combination.getCliAuth().equals(SecMethod.CLIAUTH_NONE)) {
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(combination,
              this.request, Lists.newArrayList(401, 403)));
        } else {
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(combination,
              this.request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              Lists.newArrayList()));
        }
      }
    });

  }

  /**
   * Helper method for creating simple request method validation steps (e.g. run a PUT request and
   * expect error, run a POST and expect success).
   */
  private InlineValidationStep createHttpMethodValidationStep(SecMethodsCombination combination,
      String httpMethod, boolean expectSuccess) {
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
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(combination,
              this.request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              Collections.<String>emptyList()));
        } else {
          return Optional.of(
              EchoValidationSuite.this.makeRequestAndExpectError(combination, this.request, 405));
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

  private Response makeRequestAndExpectError(SecMethodsCombination combination, Request request,
      int status) throws Failure {
    return this.makeRequestAndExpectError(combination, request, Lists.newArrayList(status));
  }

  /**
   * Make the request and check if the response contains a valid error of expected type.
   *
   * @param request The request to be made.
   * @param statuses Expected HTTP response statuses (any of those).
   * @throws Failure If HTTP status differs from expected, or if the response body doesn't contain a
   *         proper error response.
   */
  private Response makeRequestAndExpectError(SecMethodsCombination combination, Request request,
      List<Integer> statuses) throws Failure {
    Response response;
    try {
      response = this.internet.makeRequest(request);
    } catch (IOException e) {
      logger.debug(
          "Problems retrieving response from server: " + ExceptionUtils.getFullStackTrace(e));
      throw new Failure("Problems retrieving response from server: " + e.getMessage(), Status.ERROR,
          null);
    }
    final List<String> notices = this.validateResponseCommons(combination, request, response);
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
      // Simplified matching.
      if (wantDigest == null || (!wantDigest.contains("SHA-256"))) {
        throw new Failure("It is RECOMMENDED for HTTP 401 responses to contain a proper "
            + "Want-Digest header with at least the SHA-256 value.", Status.WARNING, response);
      }
    }
    StringBuilder sb = new StringBuilder();
    if (notices.size() > 0) {
      sb.append("Notices:\n");
      for (String message : notices) {
        sb.append("- ").append(message).append('\n');
      }
    }
    if (sb.length() > 0) {
      throw new Failure(sb.toString(), Status.NOTICE, response);
    }
    return response;
  }

  /**
   * Make the request and make sure that the response contains a valid HTTP 200 Echo API response.
   *
   * @param request The request to be made.
   * @param heiIdsExpected The expected contents of the hei-id list.
   * @param echoValuesExpected The expected contents of the echo list.
   * @throws Failure If some expectations are not met.
   */
  private Response makeRequestAndExpectHttp200(SecMethodsCombination combination, Request request,
      List<String> heiIdsExpected, List<String> echoValuesExpected) throws Failure {
    Response response;
    try {
      response = this.internet.makeRequest(request);
    } catch (IOException e) {
      logger.debug(
          "Problems retrieving response from server: " + ExceptionUtils.getFullStackTrace(e));
      throw new Failure("Problems retrieving response from server: " + e.getMessage(), Status.ERROR,
          null);
    }
    final List<String> notices = this.validateResponseCommons(combination, request, response);
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
      throw new Failure("The response has proper HTTP status and it passed the schema validation. "
          + "However, there's something wrong with the echo values produced. "
          + "We expected the response to contain the following echo values: " + echoValuesExpected
          + ", but the following values were found instead: " + echoValuesGot, Status.FAILURE,
          response);
    }
    StringBuilder sb = new StringBuilder();
    if (notices.size() > 0) {
      sb.append("Notices:\n");
      for (String message : notices) {
        sb.append("- ").append(message).append('\n');
      }
    }
    if (sb.length() > 0) {
      throw new Failure(sb.toString(), Status.NOTICE, response);
    }
    return response;
  }

  private List<String> validateResponseCommons(SecMethodsCombination combination, Request request,
      Response response) throws Failure {

    // Verify X-Request-Id

    String reqReqId = request.getHeader("X-Request-Id");
    String resReqId = response.getHeader("X-Request-Id");
    if (resReqId != null) {
      // If present in response, then it should also be present in the request.
      if (reqReqId == null) {
        throw new Failure("The request didn't contain the X-Request-Id header, so "
            + "the response also shouldn't.", Status.WARNING, response);
      } else {
        // Both should be equal.
        if (!reqReqId.equals(resReqId)) {
          throw new Failure("Expecting the response to contain exactly the same X-Request-Id "
              + "as has been sent in the request.", Status.FAILURE, response);
        }
      }
    } else {
      // Not present in the response. This might be a failure, but this depends on a particular
      // combination (and should be validated when validating this particular combination).
    }

    List<String> notices = new ArrayList<>();
    if (combination.getSrvAuth().equals(SecMethod.SRVAUTH_TLSCERT)) {
      if (response.getHeader("Signature") != null) {
        notices.add("Response contains the Signature header, even though the client "
            + "didn't ask for it. In general, there's nothing wrong with that, but "
            + "you might want to tweak your implementation to save some computing time.");
      }
    } else if (combination.getSrvAuth().equals(SecMethod.SRVAUTH_HTTPSIG)) {
      this.validateResponseCommonsForxHxx(combination, request, response);
    }

    return notices;
  }

  private void validateResponseCommonsForxHxx(SecMethodsCombination combination, // NOPMD
      Request request, Response response) throws Failure {

    // Verify X-Request-Id

    if ((request.getHeader("X-Request-Id") != null)
        && (response.getHeader("X-Request-Id") == null)) {
      throw new Failure("HTTP Signature Server Authentication requires the server to "
          + "include the correlated X-Request-Id, whenever it has been included "
          + "in the request.", Status.FAILURE, response);
    }
    /*
     * We don't need to check if X-Request-Id headers are equal. It has already been checked.
     */

    // Verify signature parameters

    String sigHeader = response.getHeader("Signature");
    if (sigHeader == null) {
      throw new Failure("Expecting the response to contain the Signature header", Status.FAILURE,
          response);
    }
    /*
     * Our library parses only the Authorization header (not the Signature header), so we will
     * reformat the value to match.
     */
    Authorization authz = Authorization.parse("Signature " + sigHeader);
    if (authz == null) {
      throw new Failure(
          "Could not parse response's Signature header, make sure it's in a proper format",
          Status.FAILURE, response);
    }
    if (!authz.getAlgorithm().equals(Algorithm.RSA_SHA256)) {
      throw new Failure("Expecting the response's Signature to use the rsa-sha256 algorithm, "
          + "but " + authz.getAlgorithm().getName() + " found instead.", Status.FAILURE, response);
    }
    Set<String> signedHeaders = new HashSet<>(authz.getHeaders());
    List<String> headersThatShouldBeSigned = Lists.newArrayList("digest");
    if (response.getHeader("X-Request-Id") != null) {
      headersThatShouldBeSigned.add("x-request-id");
    }
    if (response.getHeader("X-Request-Signature") != null) {
      headersThatShouldBeSigned.add("x-request-signature");
    }
    for (String headerName : headersThatShouldBeSigned) {
      if (!signedHeaders.contains(headerName)) {
        throw new Failure("Expecting the response's Signature to cover the \"" + headerName
            + "\" header, but it doesn't.", Status.FAILURE, response);
      }
    }
    if (signedHeaders.contains("date") || signedHeaders.contains("original-date")) {
      // Okay!
    } else {
      throw new Failure(
          "Expecting the response's Signature to cover the \"date\" header "
              + "or the \"original-date\" header (or both), but it doesn't cover any of them.",
          Status.FAILURE, response);
    }

    // Verify X-Request-Signature

    String reqAuthHeader = request.getHeader("Authorization");
    Authorization reqAuthz = Authorization.parse(reqAuthHeader);
    if (response.getHeader("X-Request-Signature") != null) {
      if (reqAuthz == null) {
        throw new Failure(
            "X-Request-Signature response header should be present only "
                + "when HTTP Signature Client Authentication has been used in the request.",
            Status.FAILURE, response);
      }
      if (!response.getHeader("X-Request-Signature").equals(reqAuthz.getSignature())) {
        throw new Failure("X-Request-Signature response header doesn't match the actual "
            + "HTTP Signature of the orginal request", Status.FAILURE, response);
      }
    } else if (reqAuthz != null) {
      throw new Failure("Missing X-Request-Signature response header.", Status.FAILURE, response);
    }

    // Verify the date(s)

    List<String> dateHeadersToVerify = new ArrayList<>();
    if (response.getHeader("Date") != null) {
      dateHeadersToVerify.add("Date");
    }
    if (response.getHeader("Original-Date") != null) {
      dateHeadersToVerify.add("Original-Date");
    }
    if (dateHeadersToVerify.size() == 0) {
      throw new Failure("Expecting the response to contain the \"Date\" "
          + "header or the \"Original-Date\" (or both).", Status.FAILURE, response);
    }
    for (String headerName : dateHeadersToVerify) {
      String errorMessage = Utils.findErrorsInHttpSigDateHeader(response.getHeader(headerName));
      if (errorMessage != null) {
        throw new Failure("The value of response's \"" + headerName
            + "\" header failed verification: " + errorMessage, Status.FAILURE, response);
      }
    }

    // Look up the key

    RSAPublicKey serverKey = this.regClient.findRsaPublicKey(authz.getKeyId());
    if (serverKey == null) {
      throw new Failure(
          "The keyId extracted from the response's Signature header "
              + "doesn't match any of the keys published in the Registry",
          Status.FAILURE, response);
    }
    if (!this.regClient.isApiCoveredByServerKey(this.matchedApiEntry, serverKey)) {
      throw new Failure("The keyId extracted from the response's Signature header "
          + "has been found in the Registry, but it doesn't cover the Echo API "
          + "endpoint which has generated the response. Make sure that you have "
          + "included your key in a proper manifest section.", Status.FAILURE, response);
    }

    // Verify the signature

    DefaultKeychain keychain = new DefaultKeychain();
    keychain.add(new HttpSigRsaPublicKey(serverKey));
    DefaultVerifier verifier = new DefaultVerifier(keychain);

    RequestContent.Builder rcb = new RequestContent.Builder();
    rcb.setRequestTarget(request.getMethod(), request.getPathPseudoHeader());
    for (Map.Entry<String, String> entry : response.getHeaders().entrySet()) {
      rcb.addHeader(entry.getKey(), entry.getValue());
    }
    // The library needs this challenge object in order to verify signature. So,
    // we need to create a "fake" instance just for that.
    Challenge challenge = new Challenge("Not verified", headersThatShouldBeSigned,
        Lists.newArrayList(Algorithm.RSA_SHA256));
    VerifyResult verifyResult = verifier.verifyWithResult(challenge, rcb.build(), authz);
    if (!verifyResult.equals(VerifyResult.SUCCESS)) {
      throw new Failure("Invalid HTTP Signature in response: " + verifyResult.toString(),
          Status.FAILURE, response);
    }

    // Verify the digest

    String digestHeader = response.getHeader("Digest");
    if (digestHeader == null) {
      throw new Failure("Missing response header: Digest", Status.FAILURE, response);
    }
    String expectedSha256Digest = Utils.computeDigest(response.getBody());
    Map<String, String> attrs = this.parseDigestHeaderValue(digestHeader);
    if (!attrs.containsKey("SHA-256")) {
      throw new Failure("Missing SHA-256 digest in Digest header", Status.FAILURE, response);
    }
    String got = attrs.get("SHA-256");
    if (!got.equals(expectedSha256Digest)) {
      throw new Failure("Response SHA-256 digest mismatch. Expected: " + expectedSha256Digest,
          Status.FAILURE, response);
    }
  }

  private void validateSecMethodCombination(SecMethodsCombination combination) throws SuiteBroken {

    /*
     * Make sure that properly executed GET and POST requests work. This steps varies on particular
     * combination.
     */

    if (!combination.getCliAuth().equals(SecMethod.CLIAUTH_NONE)) {
      this.addAndRun(false, this.createHttpMethodValidationStep(combination, "GET", true));
      this.addAndRun(false, this.createHttpMethodValidationStep(combination, "POST", true));
    }

    /* Try to "break" specific security method implementations. */

    if (combination.getCliAuth().equals(SecMethod.CLIAUTH_NONE)) {
      this.checkEdgeCasesForAxxx(combination);
    } else if (combination.getCliAuth().equals(SecMethod.CLIAUTH_TLSCERT_SELFSIGNED)) {
      this.checkEdgeCasesForSxxx(combination);
    } else if (combination.getCliAuth().equals(SecMethod.CLIAUTH_HTTPSIG)) {
      this.checkEdgeCasesForHxxx(combination);
    } else {
      // Shouldn't happen.
      throw new RuntimeException("Unsupported combination");
    }
    if (combination.getSrvAuth().equals(SecMethod.SRVAUTH_TLSCERT)) {
      // Not much to validate.
    } else if (combination.getSrvAuth().equals(SecMethod.SRVAUTH_HTTPSIG)) {
      this.checkEdgeCasesForxHxx(combination);
    } else {
      // Shouldn't happen.
      throw new RuntimeException("Unsupported combination");
    }

    if (!combination.getCliAuth().equals(SecMethod.CLIAUTH_NONE)) {
      this.addAndRun(false, this.createHttpMethodValidationStep(combination, "PUT", false));
      this.addAndRun(false, this.createHttpMethodValidationStep(combination, "DELETE", false));

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
          this.request = EchoValidationSuite.this.createValidRequestForCombination(combination,
              "GET", EchoValidationSuite.this.urlToBeValidated + "?echo=a&echo=b&echo=a");
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(combination,
              this.request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              expectedEchoValues));
        }
      });

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
          if (combination.getCliAuth().equals(SecMethod.CLIAUTH_HTTPSIG)) {
            // We have changed the body, so we need to regenerate the digest and signature.
            this.request.recomputeAndAttachDigestHeader();
            String keyId = Authorization.parse(this.request.getHeader("Authorization")).getKeyId();
            this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId,
                EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse(),
                Lists.newArrayList("(request-target)", "date", "host", "digest", "x-request-id"));
          }
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(combination,
              this.request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              expectedEchoValues));
        }
      });

      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Trying " + combination + " POST request with a list of echo values [a, b, a], "
              + "plus an additional GET echo=c&echo=c parameters. Expecting the GET parameters "
              + "to be ignored. (It's a POST request, so all parameters are passed via POST body.)";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          this.request = EchoValidationSuite.this.createValidRequestForCombination(combination,
              "POST", EchoValidationSuite.this.urlToBeValidated);
          // Update the body
          this.request.setBody("echo=a&echo=b&echo=a".getBytes(StandardCharsets.UTF_8));
          // Update the URL
          String url = this.request.getUrl();
          url += url.contains("?") ? "&" : "?";
          url += "echo=c&echo=c";
          this.request.setUrl(url);
          // Expected result
          ArrayList<String> expectedEchoValues = new ArrayList<>();
          expectedEchoValues.add("a");
          expectedEchoValues.add("b");
          expectedEchoValues.add("a");
          if (combination.getCliAuth().equals(SecMethod.CLIAUTH_HTTPSIG)) {
            // We have changed the body, so we need to regenerate the digest and signature.
            this.request.recomputeAndAttachDigestHeader();
            String keyId = Authorization.parse(this.request.getHeader("Authorization")).getKeyId();
            this.request.recomputeAndAttachHttpSigAuthorizationHeader(keyId,
                EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse(),
                Lists.newArrayList("(request-target)", "date", "host", "digest", "x-request-id"));
          }
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(combination,
              this.request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              expectedEchoValues));
        }
      });
    }
  }

  protected Request createValidRequestForCombination(SecMethodsCombination combination,
      String httpMethod, String url) {

    Request request = new Request(httpMethod, url);

    if (httpMethod.equals("POST")) {
      request.putHeader("Content-Type", "application/x-www-form-urlencoded");
    }

    // cliauth

    if (combination.getCliAuth().equals(SecMethod.CLIAUTH_NONE)) {
      // pass
    } else if (combination.getCliAuth().equals(SecMethod.CLIAUTH_TLSCERT_SELFSIGNED)) {
      request.setClientCertificate(this.parentEchoValidator.getTlsClientCertificateInUse(),
          this.parentEchoValidator.getTlsKeyPairInUse());
    } else if (combination.getCliAuth().equals(SecMethod.CLIAUTH_HTTPSIG)) {
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

    if (combination.getSrvAuth().equals(SecMethod.SRVAUTH_TLSCERT)) {
      // pass
    } else if (combination.getSrvAuth().equals(SecMethod.SRVAUTH_HTTPSIG)) {
      request.putHeader("Accept-Signature", "rsa-sha256");
    } else {
      throw new RuntimeException("Not supported");
    }

    // reqencr

    if (combination.getReqEncr().equals(SecMethod.REQENCR_TLS)) {
      // pass
    } else {
      throw new RuntimeException("Not supported");
    }

    // resencr

    if (combination.getResEncr().equals(SecMethod.RESENCR_TLS)) {
      // pass
    } else {
      throw new RuntimeException("Not supported");
    }

    return request;
  }

  protected Map<String, String> parseDigestHeaderValue(String value) {
    Map<String, String> result = new HashMap<>();
    for (String pair : value.split(",")) {
      String[] parts = pair.split("=", 2);
      if (parts.length == 2) {
        result.put(parts[0].trim(), parts[1].trim());
      }
    }
    return result;
  }

  List<ValidationStepWithStatus> getResults() {
    return this.steps;
  }

  void run() {

    try {

      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Check if our client credentials have been served long enough.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          if (new Date().getTime() - EchoValidationSuite.this.parentEchoValidator
              .getCredentialsGenerationDate().getTime() < 10 * 60 * 1000) {
            throw new Failure(
                "Our client credentials are quite fresh. This means that many Echo APIs will "
                    + "(correctly) return error responses in places where we expect HTTP 200. "
                    + "This notice will disappear once our credentials are 10 minutes old.",
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
        this.validateSecMethodCombination(new SecMethodsCombination(SecMethod.CLIAUTH_NONE,
            SecMethod.SRVAUTH_TLSCERT, SecMethod.REQENCR_TLS, SecMethod.RESENCR_TLS));
        this.validateSecMethodCombination(
            new SecMethodsCombination(SecMethod.CLIAUTH_TLSCERT_SELFSIGNED,
                SecMethod.SRVAUTH_TLSCERT, SecMethod.REQENCR_TLS, SecMethod.RESENCR_TLS));
      } else if (this.echoApiVersionDetected == 2) {

        this.addAndRun(false, new InlineValidationStep() {

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
            notices.addAll(sec.getNotices());

            // We will create four lists of security methods - one per type. Each added item
            // will represent one "validatable" security method.

            // cliauth

            List<SecMethod> cliAuthMethodsToValidate = new ArrayList<>();
            if (sec.supportsCliAuthNone()) {
              warnings.add("Anonymous Client Authentication SHOULD NOT be enabled for Echo API.");
            }
            // Even though, we will still run some tests on it.
            cliAuthMethodsToValidate.add(SecMethod.CLIAUTH_NONE);
            if (sec.supportsCliAuthTlsCert()) {
              if (sec.supportsCliAuthTlsCertSelfSigned()) {
                cliAuthMethodsToValidate.add(SecMethod.CLIAUTH_TLSCERT_SELFSIGNED);
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
              cliAuthMethodsToValidate.add(SecMethod.CLIAUTH_HTTPSIG);
            } else {
              warnings.add("It is RECOMMENDED for all EWP server endpoints to support HTTP "
                  + "Signature Client Authentication. Your endpoint doesn't.");
            }
            if (cliAuthMethodsToValidate.size() <= 1) {
              errors.add("Your Echo API does not support ANY of the client authentication "
                  + "methods recognized by the Validator.");
            }

            // srvauth

            List<SecMethod> srvAuthMethodsToValidate = new ArrayList<>();
            if (sec.supportsSrvAuthTlsCert()) {
              srvAuthMethodsToValidate.add(SecMethod.SRVAUTH_TLSCERT);
            }
            if (sec.supportsSrvAuthHttpSig()) {
              srvAuthMethodsToValidate.add(SecMethod.SRVAUTH_HTTPSIG);
              if (!sec.supportsSrvAuthTlsCert()) {
                warnings.add("Server which support HTTP Signature Server Authentication "
                    + "SHOULD also support TLS Server Certificate Authentication");
              }
            } else {
              notices.add("It is RECOMMENDED for all servers to support "
                  + "HTTP Signature Server Authentication.");
            }
            if (srvAuthMethodsToValidate.size() == 0) {
              errors.add("Your Echo API does not support ANY of the server authentication "
                  + "methods recognized by the Validator.");
            }

            // reqencr

            List<SecMethod> reqEncrMethodsToValidate = new ArrayList<>();
            if (sec.supportsReqEncrTls()) {
              reqEncrMethodsToValidate.add(SecMethod.REQENCR_TLS);
            }
            if (sec.supportsReqEncrEwp()) {
              notices.add("Echo API Validator is currently NOT validating ewp-rsa-aes128gcm "
                  + "request encryption.");
            }
            if (reqEncrMethodsToValidate.size() == 0) {
              errors.add("Your Echo API does not support ANY of the request encryption "
                  + "methods recognized by the Validator.");
            }

            // resencr

            List<SecMethod> resEncrMethodsToValidate = new ArrayList<>();
            if (sec.supportsResEncrTls()) {
              resEncrMethodsToValidate.add(SecMethod.RESENCR_TLS);
            }
            if (sec.supportsResEncrEwp()) {
              notices.add("Echo API Validator is currently NOT validating ewp-rsa-aes128gcm "
                  + "response encryption.");
            }
            if (resEncrMethodsToValidate.size() == 0) {
              errors.add("Your Echo API does not support ANY of the response encryption "
                  + "methods recognized by the Validator.");
            }

            // Generate all possible combinations of validatable security methods.

            EchoValidationSuite.this.combinationsToValidate = new ArrayList<>();
            for (SecMethod cliauth : cliAuthMethodsToValidate) {
              for (SecMethod srvauth : srvAuthMethodsToValidate) {
                for (SecMethod reqencr : reqEncrMethodsToValidate) {
                  for (SecMethod resencr : resEncrMethodsToValidate) {
                    EchoValidationSuite.this.combinationsToValidate
                        .add(new SecMethodsCombination(cliauth, srvauth, reqencr, resencr));
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

        for (SecMethodsCombination combination : this.combinationsToValidate) {
          this.validateSecMethodCombination(combination);
        }
      }

    } catch (SuiteBroken e) {
      // Ignore.
    } catch (RuntimeException e) {
      this.steps.add(new GenericErrorFakeStep(e));
    }
  }

}
