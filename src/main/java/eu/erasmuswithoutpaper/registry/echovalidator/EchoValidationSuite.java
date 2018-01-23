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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
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
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.AnonymousRequestSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpCertificateRequestSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigResponseAuthorizer;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpRsaAesRequestEncoder;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpRsaAesResponseDecoder;
import eu.erasmuswithoutpaper.registry.internet.sec.GzipResponseDecoder;
import eu.erasmuswithoutpaper.registry.internet.sec.InvalidResponseError;
import eu.erasmuswithoutpaper.registry.internet.sec.NoopRequestEncoder;
import eu.erasmuswithoutpaper.registry.internet.sec.RequestEncoder;
import eu.erasmuswithoutpaper.registry.internet.sec.RequestSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.TlsResponseAuthorizer;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import eu.erasmuswithoutpaper.rsaaes.BadEwpRsaAesBody;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmDecoder;

import com.google.common.collect.Lists;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Authorization;
import net.adamcin.httpsig.api.Challenge;
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
    public String getMessage() {
      return this.cause.getMessage();
    }

    @Override
    public String getName() {
      return "Other error occurred. Please contact the developers.";
    }

    @Override
    public List<Request> getRequestSnapshots() {
      return Lists.newArrayList();
    }

    @Override
    public List<Response> getResponseSnapshots() {
      return Lists.newArrayList();
    }

    @Override
    public Optional<String> getServerDeveloperErrorMessage() {
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
  private List<Combination> combinationsToValidate;

  private final EwpDocBuilder docBuilder;
  private final Internet internet;
  private final RegistryClient regClient;
  private final AnonymousRequestSigner reqSignerAnon;
  private final EwpCertificateRequestSigner reqSignerCert;
  private final EwpHttpSigRequestSigner reqSignerHttpSig;
  private final DecodingHelper resDecoderHelper;

  private EwpHttpSigResponseAuthorizer resAuthorizerHttpSig;

  private Set<CombEntry> allCombEntriesCache = null;

  EchoValidationSuite(EchoValidator echoValidator, EwpDocBuilder docBuilder, Internet internet,
      String urlStr, RegistryClient regClient) {
    this.parentEchoValidator = echoValidator;
    this.urlToBeValidated = urlStr;
    this.steps = new ArrayList<>();
    this.docBuilder = docBuilder;
    this.internet = internet;
    this.regClient = regClient;
    this.reqSignerAnon = new AnonymousRequestSigner();
    this.reqSignerCert =
        new EwpCertificateRequestSigner(this.parentEchoValidator.getTlsClientCertificateInUse(),
            this.parentEchoValidator.getTlsKeyPairInUse());
    this.reqSignerHttpSig =
        new EwpHttpSigRequestSigner(this.parentEchoValidator.getClientRsaKeyPairInUse());
    this.resDecoderHelper = new DecodingHelper();
    this.resDecoderHelper.addDecoder(new EwpRsaAesResponseDecoder(
        Lists.newArrayList(this.parentEchoValidator.getClientRsaKeyPairInUse(),
            this.parentEchoValidator.getServerRsaKeyPairInUse())));
    this.resDecoderHelper.addDecoder(new GzipResponseDecoder());
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

  private void checkEdgeCasesForAxxx(Combination combination) throws SuiteBroken {
    assert combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE);

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " (no client authentication). "
            + "Expecting a valid HTTP 401 or HTTP 403 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this, combination,
            request, Lists.newArrayList(401, 403)));
      }
    });
  }

  private void checkEdgeCasesForHxxx(Combination combination) throws SuiteBroken {
    assert combination.getCliAuth().equals(CombEntry.CLIAUTH_HTTPSIG);

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with an unknown RSA client key "
            + "(a random one, that has never been published in the Registry). "
            + "Expecting to receive a valid HTTP 401 or HTTP 403 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        KeyPair otherKeyPair =
            EchoValidationSuite.this.parentEchoValidator.getUnregisteredKeyPair();
        // Replace the previously set Authorization header with a different one.
        RequestSigner badSigner = new EwpHttpSigRequestSigner(otherKeyPair);
        badSigner.sign(request);
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this,
            combination.withChangedResEncr(CombEntry.RESENCR_TLS), request,
            Lists.newArrayList(401, 403)));
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
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        // Leave keyId as is, but use a new random key for signature generation.
        String previousKeyId = Authorization.parse(request.getHeader("Authorization")).getKeyId();
        KeyPair otherKeyPair =
            EchoValidationSuite.this.parentEchoValidator.getUnregisteredKeyPair();
        RequestSigner mySigner = new EwpHttpSigRequestSigner(otherKeyPair) {
          @Override
          public String getKeyId() {
            return previousKeyId;
          }
        };
        // Replace the previously set Authorization header with a different one.
        mySigner.sign(request);
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this, combination,
            request, Lists.newArrayList(400, 401)));
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
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        request.putHeader("missing-header-that-should-exist", "Temporarilly exists");
        EchoValidationSuite.this.reqSignerHttpSig.sign(request);
        request.removeHeader("missing-header-that-should-exist");
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this, combination,
            request, Lists.newArrayList(400, 401)));
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
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        String value = request.getHeader("Date");
        request.removeHeader("Date");
        request.putHeader("Original-Date", value);
        EchoValidationSuite.this.reqSignerHttpSig.sign(request);
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination,
            request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
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
          Request request =
              EchoValidationSuite.this.createValidRequestForCombination(this, combination);
          List<String> headersToSign = new ArrayList<>(stdHeaders);
          headersToSign.remove(headerToSkip);
          RequestSigner mySigner =
              new EwpHttpSigRequestSigner(EchoValidationSuite.this.reqSignerHttpSig.getKeyPair()) {
                @Override
                protected List<String> getHeadersToSign(Request request) {
                  return headersToSign;
                }
              };
          mySigner.sign(request);
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this, combination,
              request, Lists.newArrayList(400, 401)));
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
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        request.putHeader("Some-Custom-Header", "Value");
        EchoValidationSuite.this.reqSignerHttpSig.sign(request);
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination,
            request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
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
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        KeyPair keyPair = EchoValidationSuite.this.parentEchoValidator.getServerRsaKeyPairInUse();
        RequestSigner badSigner = new EwpHttpSigRequestSigner(keyPair);
        badSigner.sign(request);
        return Optional.of(
            EchoValidationSuite.this.makeRequestAndExpectError(this, combination, request, 403));
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
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
        dateTime = dateTime.minusMinutes(20);
        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(dateTime);
        request.putHeader("Original-Date", date);

        Authorization authz = Authorization.parse(request.getHeader("Authorization"));
        List<String> headers = new ArrayList<>(authz.getHeaders());
        headers.add("original-date");
        headers.remove("date");
        RequestSigner mySigner =
            new EwpHttpSigRequestSigner(EchoValidationSuite.this.reqSignerHttpSig.getKeyPair()) {
              @Override
              protected List<String> getHeadersToSign(Request request) {
                return headers;
              }
            };
        mySigner.sign(request);
        try {
          return Optional.of(
              EchoValidationSuite.this.makeRequestAndExpectError(this, combination, request, 400));
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
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        request.putHeader("X-Request-Id",
            request.getHeader("X-Request-Id").replaceAll("-", "").toUpperCase(Locale.US));
        EchoValidationSuite.this.reqSignerHttpSig.sign(request);
        try {
          return Optional.of(
              EchoValidationSuite.this.makeRequestAndExpectError(this, combination, request, 400));
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

    if (combination.getHttpMethod().equals("POST")) {

      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Trying " + combination + " with an invalid Digest. "
              + "Expecting to receive a valid HTTP 400 error response.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          Request request = EchoValidationSuite.this.createValidRequestForCombination(this,
              combination, "echo=a&echo=b&echo=a");
          // Change the body after digest and signature were generated. This should invalidate
          // the digest.
          request.setBody("something-else".getBytes(StandardCharsets.UTF_8));
          return Optional.of(
              EchoValidationSuite.this.makeRequestAndExpectError(this, combination, request, 400));
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
          Request request = EchoValidationSuite.this.createValidRequestForCombination(this,
              combination, "echo=b&echo=b");
          // Override the Digest and Authorization with the output of a custom signer.
          RequestSigner mySigner =
              new EwpHttpSigRequestSigner(EchoValidationSuite.this.reqSignerHttpSig.getKeyPair()) {
                @Override
                protected void includeDigestHeader(Request request) {
                  super.includeDigestHeader(request);
                  request.putHeader("Digest",
                      request.getHeader("Digest") + ", Unknown-Digest-Algorithm=SomeValue");
                }
              };
          mySigner.sign(request);
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination,
              request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
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
          Request request = EchoValidationSuite.this.createValidRequestForCombination(this,
              combination, "echo=b&echo=b");
          RequestSigner badSigner =
              new EwpHttpSigRequestSigner(EchoValidationSuite.this.reqSignerHttpSig.getKeyPair()) {
                @Override
                protected void includeDigestHeader(Request request) {
                  request.putHeader("Digest", "SHA=" + Base64.getEncoder().encodeToString(
                      DigestUtils.getSha1Digest().digest(request.getBodyOrEmpty())));
                }
              };
          badSigner.sign(request);
          try {
            return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this, combination,
                request, 400));
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
          return "Trying " + combination + " with \"shA-256\" request digest (mixed case). "
              + "Digest RFC requires clients to accept that. Expecting to receive a valid "
              + "HTTP 200 response.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          Request request = EchoValidationSuite.this.createValidRequestForCombination(this,
              combination, "echo=b&echo=b");
          RequestSigner mySigner =
              new EwpHttpSigRequestSigner(EchoValidationSuite.this.reqSignerHttpSig.getKeyPair()) {
                @Override
                protected void includeDigestHeader(Request request) {
                  super.includeDigestHeader(request);
                  String newValue = request.getHeader("Digest").replace("SHA-256=", "shA-256=");
                  request.putHeader("Digest", newValue);
                }
              };
          mySigner.sign(request);
          try {
            return Optional
                .of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination, request,
                    EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
                    Lists.newArrayList("b", "b")));
          } catch (Failure f) {
            if (f.getStatus().equals(Status.FAILURE)) {
              throw f.withChangedStatus(Status.WARNING);
            } else {
              throw f;
            }
          }
        }
      });
    }
  }

  private void checkEdgeCasesForSxxx(Combination combination) throws SuiteBroken {
    assert combination.getCliAuth().equals(CombEntry.CLIAUTH_TLSCERT_SELFSIGNED);

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with an unknown TLS client certificate "
            + "(a random one, that has never been published in the Registry). "
            + "Expecting to receive a valid HTTP 401 or HTTP 403 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);

        // Override the work done by the original (valid) request signer.
        KeyPair otherKeyPair =
            EchoValidationSuite.this.parentEchoValidator.getUnregisteredKeyPair();
        X509Certificate otherCert =
            EchoValidationSuite.this.parentEchoValidator.generateCertificate(otherKeyPair);
        EwpCertificateRequestSigner mySigner =
            new EwpCertificateRequestSigner(otherCert, otherKeyPair);
        mySigner.sign(request);

        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this, combination,
            request, Lists.newArrayList(401, 403)));
      }
    });
  }

  private void checkEdgeCasesForxHxx(Combination combination) throws SuiteBroken {

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with no valid algorithm in Accept-Signature header. "
            + "Expecting to receive unsigned response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        request.putHeader("Accept-Signature", "unknown-algorithm");
        EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
            .sign(request);
        Combination relaxedCombination = combination.withChangedSrvAuth(CombEntry.SRVAUTH_TLSCERT);
        if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this,
              relaxedCombination, request, Lists.newArrayList(401, 403)));
        } else {
          return Optional
              .of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, relaxedCombination,
                  request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
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
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        request.putHeader("Accept-Signature", "rsa-sha256, unknown-algorithm");
        EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
            .sign(request);
        if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this, combination,
              request, Lists.newArrayList(401, 403)));
        } else {
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination,
              request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              Lists.newArrayList()));
        }
      }
    });

    if (!combination.getCliAuth().equals(CombEntry.CLIAUTH_HTTPSIG)) {
      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Trying " + combination + " with X-Request-Id header. This header "
              + "is not required to be present in this request in this combination, "
              + "but we're still expecting the server to include its copy in the response.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          Request request =
              EchoValidationSuite.this.createValidRequestForCombination(this, combination);
          request.putHeader("X-Request-Id", UUID.randomUUID().toString());
          EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
              .sign(request);
          if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
            return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this, combination,
                request, Lists.newArrayList(401, 403)));
          } else {
            return Optional
                .of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination, request,
                    EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
                    Lists.newArrayList()));
          }
        }
      });
    }

  }

  private void checkEdgeCasesForxxEx(Combination combination) throws SuiteBroken {

    if (!combination.getHttpMethod().equals("POST")) {
      throw new RuntimeException();
    }

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination.withChangedHttpMethod("GET")
            + " - this is invalid, because GET requests are not supported by "
            + "ewp-rsa-aes128gcm encryption. Expecting HTTP 405 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {

        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        // Transform it to a GET request, while leaving the body etc. intact.
        request.setMethod("GET");
        EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
            .sign(request);
        List<Integer> acceptableResponses = Lists.newArrayList(405);
        if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
          acceptableResponses.add(403);
          acceptableResponses.add(401);
        }
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this, combination,
            request, acceptableResponses));
      }
    });

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with truncated encrypted body. "
            + "Expecting HTTP 400 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {

        Request request = EchoValidationSuite.this.createValidRequestForCombination(this,
            combination, "echo=a&echo=b&echo=a");
        byte[] body = request.getBody().get();
        // Truncate right after the encryptedAesKeyLength.
        body = Arrays.copyOf(body, 32 + 2);
        request.setBody(body);
        EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
            .sign(request);
        List<Integer> acceptableResponses = Lists.newArrayList(400);
        if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
          acceptableResponses.add(403);
          acceptableResponses.add(401);
        }
        return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this, combination,
            request, acceptableResponses));
      }
    });
  }


  private void checkEdgeCasesForxxxE(Combination combination) throws SuiteBroken {

    boolean encryptionRequired = !this.endpointSupports(CombEntry.RESENCR_TLS);
    boolean noAuth = combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE);

    if (!noAuth) {
      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          StringBuilder sb = new StringBuilder();
          sb.append("Trying " + combination + " with an invalid Accept-Encoding header. ");
          if (encryptionRequired) {
            sb.append("Your endpoint requires encryption, so we're expecting ");
            sb.append("unencrypted HTTP 406 error response.");
          } else {
            sb.append("Your endpoint does not require encryption, so we're expecting ");
            sb.append("a valid unencrypted HTTP 200 response.");
          }
          return sb.toString();
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {

          Request request =
              EchoValidationSuite.this.createValidRequestForCombination(this, combination);
          request.putHeader("Accept-Encoding", "invalid-coding");
          EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
              .sign(request);
          if (encryptionRequired) {
            return Optional.of(EchoValidationSuite.this.makeRequestAndExpectError(this,
                combination.withChangedResEncr(CombEntry.RESENCR_TLS), request, 406));
          } else {
            return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this,
                combination.withChangedResEncr(CombEntry.RESENCR_TLS), request,
                EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
                Collections.<String>emptyList()));
          }
        }
      });

      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Trying " + combination + " with Accept-Encoding header formatted in a "
              + "different way. This formatting is still valid (per RFC), so we are expecting "
              + "a valid encrypted HTTP 200 response.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {

          Request request =
              EchoValidationSuite.this.createValidRequestForCombination(this, combination);
          request.putHeader("Accept-Encoding", " ewp-rsa-AES128GCM ; q=1 , *;q=0");
          EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
              .sign(request);
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination,
              request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              Collections.<String>emptyList()));
        }
      });

      if (!encryptionRequired) {
        this.addAndRun(false, new InlineValidationStep() {

          @Override
          public String getName() {
            return "Trying " + combination + " with explicitly forbidden ewp-rsa-aes128gcm in "
                + "its Accept-Encoding header. Expecting unencrypted response.";
          }

          @Override
          protected Optional<Response> innerRun() throws Failure {

            Request request =
                EchoValidationSuite.this.createValidRequestForCombination(this, combination);
            request.putHeader("Accept-Encoding", " ewp-rsa-aes128gcm;q=0, identity;q=1");
            EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
                .sign(request);
            return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this,
                combination.withChangedResEncr(CombEntry.RESENCR_TLS), request,
                EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
                Collections.<String>emptyList()));
          }
        });
      }
    }

    if (combination.getCliAuth().equals(CombEntry.CLIAUTH_HTTPSIG)) {
      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Trying " + combination + " with unsigned "
              + "Accept-Response-Encryption-Key header. "
              + "Expecting the unsigned header to be ignored " + "(man-in-the-middle attack).";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {

          Request request =
              EchoValidationSuite.this.createValidRequestForCombination(this, combination);
          RSAPublicKey attackersKey =
              EchoValidationSuite.this.parentEchoValidator.getServerRsaPublicKeyInUse();
          byte[] attackersKeyFingerprint =
              DigestUtils.getSha256Digest().digest(attackersKey.getEncoded());
          if (request.getHeader("Accept-Response-Encryption-Key") == null) {
            request.putHeader("Accept-Response-Encryption-Key",
                Base64.getEncoder().encodeToString(attackersKey.getEncoded()));
          } else {
            // Should not happen. When httpsig client authentication is used, then this
            // header should not be present.
            throw new RuntimeException();
          }
          // Make sure that attacker's key is NOT used.
          Response response = EchoValidationSuite.this.makeRequest(this, request);
          try {
            byte[] actualFingerprint =
                EwpRsaAes128GcmDecoder.extractRecipientPublicKeySha256(response.getBody());
            if (Arrays.equals(actualFingerprint, attackersKeyFingerprint)) {
              throw new Failure(
                  "The response was encrypted with the attacker's key. "
                      + "Your installation is vulnerable to man-in-the-middle attacks.",
                  Status.FAILURE, response);
            }
          } catch (BadEwpRsaAesBody e) {
            // Ignore. This exception will be re-thrown in a moment (and properly cast
            // into Failure).
          }
          EchoValidationSuite.this.expectHttp200(this, combination, request, response,
              EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              Collections.<String>emptyList());
          return Optional.of(response);
        }
      });

      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Trying " + combination + " with overriden encryption key "
              + "(properly signed Accept-Response-Encryption-Key header). "
              + "Expecting the response to be encrypted for the overriden key.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {

          Request request =
              EchoValidationSuite.this.createValidRequestForCombination(this, combination);
          RSAPublicKey newKey =
              EchoValidationSuite.this.parentEchoValidator.getServerRsaPublicKeyInUse();
          byte[] newKeyFingerprint = DigestUtils.getSha256Digest().digest(newKey.getEncoded());
          if (request.getHeader("Accept-Response-Encryption-Key") == null) {
            request.putHeader("Accept-Response-Encryption-Key",
                Base64.getEncoder().encodeToString(newKey.getEncoded()));
          } else {
            // Should not happen. When httpsig client authentication is used, then this
            // header should not be present.
            throw new RuntimeException();
          }
          EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
              .sign(request);
          // Make sure that the "old" key is not used.
          Response response = EchoValidationSuite.this.makeRequest(this, request);
          KeyPair oldKeyPair =
              EchoValidationSuite.this.parentEchoValidator.getClientRsaKeyPairInUse();
          byte[] oldKeyFingerprint =
              DigestUtils.getSha256Digest().digest(oldKeyPair.getPublic().getEncoded());
          try {
            byte[] fingerprint =
                EwpRsaAes128GcmDecoder.extractRecipientPublicKeySha256(response.getBody());
            if (Arrays.equals(fingerprint, oldKeyFingerprint)) {
              throw new Failure("The response was encrypted with the signer's key. "
                  + "Your installation seems to be ignoring the (properly signed) "
                  + "Accept-Response-Encryption-Key header.", Status.FAILURE, response);
            }
            if (!Arrays.equals(fingerprint, newKeyFingerprint)) {
              throw new Failure("The response seems to be encrypted to neither the "
                  + "signer's key, nor to the overridden key.", Status.FAILURE, response);
            }
          } catch (BadEwpRsaAesBody e) {
            // Ignore. This exception will be re-thrown in a moment (and properly cast
            // into Failure).
          }
          EchoValidationSuite.this.expectHttp200(this, combination, request, response,
              EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              Collections.<String>emptyList());
          return Optional.of(response);
        }
      });
    }
  }

  /**
   * Helper method for creating simple request method validation steps (e.g. run a PUT request and
   * expect error, run a POST and expect success).
   */
  private InlineValidationStep createHttpMethodValidationStep(Combination combination,
      boolean expectSuccess) {
    return new InlineValidationStep() {

      @Override
      public String getName() {
        if (expectSuccess) {
          return "Trying " + combination + " with a " + combination.getHttpMethod() + " request, "
              + "and without any additional parameters. Expecting to "
              + "receive a valid HTTP 200 Echo API response with proper hei-ids, and "
              + "without any echo values.";
        } else {
          return "Trying " + combination + " with a " + combination.getHttpMethod() + " request. "
              + "Expecting to receive a valid HTTP 405 error response.";
        }
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request =
            EchoValidationSuite.this.createValidRequestForCombination(this, combination);
        if (expectSuccess) {
          return Optional.of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination,
              request, EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
              Collections.<String>emptyList()));
        } else {
          return Optional.of(
              EchoValidationSuite.this.makeRequestAndExpectError(this, combination, request, 405));
        }
      }
    };
  }

  private List<String> decodeAndValidateResponseCommons(InlineValidationStep step,
      Combination combination, Request request, Response response) throws Failure {

    try {
      new TlsResponseAuthorizer().authorize(request, response);
    } catch (InvalidResponseError e) {
      throw new Failure(e.getMessage(), Status.FAILURE, response);
    }
    step.addResponseSnapshot(response);

    List<String> notices = new ArrayList<>();
    if (combination.getSrvAuth().equals(CombEntry.SRVAUTH_TLSCERT)) {
      if (response.getHeader("Signature") != null) {
        notices.add("Response contains the Signature header, even though the client "
            + "didn't ask for it. In general, there's nothing wrong with that, but "
            + "you might want to tweak your implementation to save some computing time.");
      }
    } else if (combination.getSrvAuth().equals(CombEntry.SRVAUTH_HTTPSIG)) {
      this.validateResponseCommonsForxHxx(combination, request, response);
    }
    step.addResponseSnapshot(response);

    // Decode.

    if (request.getHeader("Accept-Encoding") != null) {
      this.resDecoderHelper.setAcceptEncodingHeader(request.getHeader("Accept-Encoding"));
    } else {
      this.resDecoderHelper.setAcceptEncodingHeader(null);
    }
    if (combination.getResEncr().equals(CombEntry.RESENCR_EWP)) {
      this.resDecoderHelper.setRequiredCodings(Lists.newArrayList("ewp-rsa-aes128gcm"));
    } else {
      this.resDecoderHelper.setRequiredCodings(Lists.newArrayList());
    }
    this.resDecoderHelper.decode(step, response);

    return notices;
  }

  private boolean endpointSupports(CombEntry combEntry) {
    Set<CombEntry> set = this.getAllCombEntriesSupportedByEndpoint();
    return set.contains(combEntry);
  }

  private void expectError(InlineValidationStep step, Combination combination, Request request,
      Response response, List<Integer> statuses) throws Failure {
    final List<String> notices =
        this.decodeAndValidateResponseCommons(step, combination, request, response);
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
            + "WWW-Authenticate header (it should be signed if HttpSig is used). See here: "
            + "https://tools.ietf.org/html/rfc7235#section-4.1", Status.WARNING, response);
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
  }

  private void expectHttp200(InlineValidationStep step, Combination combination, Request request,
      Response response, List<String> heiIdsExpected, List<String> echoValuesExpected)
      throws Failure {
    final List<String> notices =
        this.decodeAndValidateResponseCommons(step, combination, request, response);
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

  private Set<CombEntry> getAllCombEntriesSupportedByEndpoint() {
    if (this.allCombEntriesCache == null) {
      this.allCombEntriesCache = new HashSet<>();
      for (Combination combination : this.combinationsToValidate) {
        this.allCombEntriesCache.add(combination.getCliAuth());
        this.allCombEntriesCache.add(combination.getSrvAuth());
        this.allCombEntriesCache.add(combination.getReqEncr());
        this.allCombEntriesCache.add(combination.getResEncr());
      }
    }
    return Collections.unmodifiableSet(this.allCombEntriesCache);
  }

  private EwpHttpSigResponseAuthorizer getEwpHttpSigResponseAuthorizer() {
    if (this.resAuthorizerHttpSig == null) {
      this.resAuthorizerHttpSig =
          new EwpHttpSigResponseAuthorizer(this.regClient, this.matchedApiEntry);
    }
    return this.resAuthorizerHttpSig;
  }

  private RequestEncoder getRequestEncoderForCombination(InlineValidationStep step, Request request,
      Combination combination) {
    step.addRequestSnapshot(request);
    if (combination.getReqEncr().equals(CombEntry.REQENCR_TLS)) {
      return new NoopRequestEncoder();
    } else if (combination.getReqEncr().equals(CombEntry.REQENCR_EWP)) {
      if (combination.getHttpMethod().equals("GET")) {
        // Invalid.
        throw new RuntimeException();
      }
      RSAPublicKey key =
          this.pickRandom(this.regClient.getServerKeysCoveringApi(combination.getApiEntry()));
      return new EwpRsaAesRequestEncoder(key);
    } else {
      throw new RuntimeException("Not supported");
    }
  }

  private RequestSigner getRequestSignerForCombination(InlineValidationStep step, Request request,
      Combination combination) {
    step.addRequestSnapshot(request);
    if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
      return this.reqSignerAnon;
    } else if (combination.getCliAuth().equals(CombEntry.CLIAUTH_HTTPSIG)) {
      return this.reqSignerHttpSig;
    } else if (combination.getCliAuth().equals(CombEntry.CLIAUTH_TLSCERT_SELFSIGNED)) {
      return this.reqSignerCert;
    } else {
      throw new RuntimeException();
    }
  }

  private Response makeRequest(InlineValidationStep step, Request request) throws Failure {
    step.addRequestSnapshot(request);
    try {
      Response response = this.internet.makeRequest(request);
      step.addResponseSnapshot(response);
      return response;
    } catch (IOException e) {
      logger.debug(
          "Problems retrieving response from server: " + ExceptionUtils.getFullStackTrace(e));
      throw new Failure("Problems retrieving response from server: " + e.getMessage(), Status.ERROR,
          null);
    }
  }

  private Response makeRequestAndExpectError(InlineValidationStep step, Combination combination,
      Request request, int status) throws Failure {
    return this.makeRequestAndExpectError(step, combination, request, Lists.newArrayList(status));
  }

  /**
   * Make the request and check if the response contains a valid error of expected type.
   *
   * @param request The request to be made.
   * @param statuses Expected HTTP response statuses (any of those).
   * @throws Failure If HTTP status differs from expected, or if the response body doesn't contain a
   *         proper error response.
   */
  private Response makeRequestAndExpectError(InlineValidationStep step, Combination combination,
      Request request, List<Integer> statuses) throws Failure {
    Response response = this.makeRequest(step, request);
    this.expectError(step, combination, request, response, statuses);
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
  private Response makeRequestAndExpectHttp200(InlineValidationStep step, Combination combination,
      Request request, List<String> heiIdsExpected, List<String> echoValuesExpected)
      throws Failure {
    Response response = this.makeRequest(step, request);
    this.expectHttp200(step, combination, request, response, heiIdsExpected, echoValuesExpected);
    return response;
  }

  private RSAPublicKey pickRandom(Collection<RSAPublicKey> keys) {
    List<RSAPublicKey> lst = new ArrayList<>(keys);
    int index = new Random().nextInt(lst.size());
    return lst.get(index);
  }

  private void validateCombination(Combination combination) throws SuiteBroken {

    /* Try to "break" specific security method implementations. */

    if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
      this.checkEdgeCasesForAxxx(combination);
    } else if (combination.getCliAuth().equals(CombEntry.CLIAUTH_TLSCERT_SELFSIGNED)) {
      this.checkEdgeCasesForSxxx(combination);
    } else if (combination.getCliAuth().equals(CombEntry.CLIAUTH_HTTPSIG)) {
      this.checkEdgeCasesForHxxx(combination);
    } else {
      // Shouldn't happen.
      throw new RuntimeException("Unsupported combination");
    }
    if (combination.getSrvAuth().equals(CombEntry.SRVAUTH_TLSCERT)) {
      // Not much to validate.
    } else if (combination.getSrvAuth().equals(CombEntry.SRVAUTH_HTTPSIG)) {
      this.checkEdgeCasesForxHxx(combination);
    } else {
      // Shouldn't happen.
      throw new RuntimeException("Unsupported combination");
    }
    if (combination.getReqEncr().equals(CombEntry.REQENCR_TLS)) {
      // Not much to test against.
    } else if (combination.getReqEncr().equals(CombEntry.REQENCR_EWP)) {
      this.checkEdgeCasesForxxEx(combination);
    } else {
      // Shouldn't happen.
      throw new RuntimeException("Unsupported combination");
    }
    if (combination.getResEncr().equals(CombEntry.RESENCR_TLS)) {
      // Not much to test against.
    } else if (combination.getResEncr().equals(CombEntry.RESENCR_EWP)) {
      this.checkEdgeCasesForxxxE(combination);
    } else {
      // Shouldn't happen.
      throw new RuntimeException("Unsupported combination");
    }

    if (!combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
      if (combination.getHttpMethod().equals("POST")) {
        this.addAndRun(false,
            this.createHttpMethodValidationStep(combination.withChangedHttpMethod("PUT"), false));
        this.addAndRun(false, this
            .createHttpMethodValidationStep(combination.withChangedHttpMethod("DELETE"), false));
      }

      if (combination.getHttpMethod().equals("GET")) {
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
            Request request = EchoValidationSuite.this.createValidRequestForCombination(this,
                combination.withChangedUrl(
                    EchoValidationSuite.this.urlToBeValidated + "?echo=a&echo=b&echo=a"));
            return Optional
                .of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination, request,
                    EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
                    expectedEchoValues));
          }
        });
      }

      if (combination.getHttpMethod().equals("POST")) {
        this.addAndRun(false, new InlineValidationStep() {

          @Override
          public String getName() {
            return "Trying " + combination + " POST request with a list of echo values [a, b, a]. "
                + "Expecting to receive a valid HTTP 200 Echo API response, "
                + "with proper hei-id and matching echo values.";
          }

          @Override
          protected Optional<Response> innerRun() throws Failure {
            ArrayList<String> expectedEchoValues = new ArrayList<>();
            expectedEchoValues.add("a");
            expectedEchoValues.add("b");
            expectedEchoValues.add("a");
            Request request = EchoValidationSuite.this.createValidRequestForCombination(this,
                combination, "echo=a&echo=b&echo=a");
            return Optional
                .of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination, request,
                    EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
                    expectedEchoValues));
          }
        });

        this.addAndRun(false, new InlineValidationStep() {

          @Override
          public String getName() {
            return "Trying " + combination + " POST request with a list of echo values [a, b, a], "
                + "plus an additional GET echo=c&echo=c parameters. Expecting the GET parameters "
                + "to be ignored. (It's a POST request, so all parameters are passed via POST "
                + "body.)";
          }

          @Override
          protected Optional<Response> innerRun() throws Failure {
            Request request = EchoValidationSuite.this.createValidRequestForCombination(this,
                combination, "echo=a&echo=b&echo=a");
            // Update the URL
            String url = request.getUrl();
            url += url.contains("?") ? "&" : "?";
            url += "echo=c&echo=c";
            request.setUrl(url);
            // Expected result
            ArrayList<String> expectedEchoValues = new ArrayList<>();
            expectedEchoValues.add("a");
            expectedEchoValues.add("b");
            expectedEchoValues.add("a");
            // We have changed the URL, so we need to regenerate the digest and signature.
            EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
                .sign(request);
            return Optional
                .of(EchoValidationSuite.this.makeRequestAndExpectHttp200(this, combination, request,
                    EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
                    expectedEchoValues));
          }
        });

        this.addAndRun(false, new InlineValidationStep() {

          @Override
          public String getName() {
            return "Trying " + combination + " with additional \"gzip\" added in "
                + "Accept-Encoding. Expecting the same response, but preferably gzipped.";
          }
          // WRTODO: test gzip on unencrypted responses

          @Override
          protected Optional<Response> innerRun() throws Failure {

            Request request =
                EchoValidationSuite.this.createValidRequestForCombination(this, combination);
            // Allow the response to be gzipped.
            String prev = request.getHeader("Accept-Encoding");
            request.putHeader("Accept-Encoding", "gzip" + ((prev != null) ? ", " + prev : ""));
            EchoValidationSuite.this.getRequestSignerForCombination(this, request, combination)
                .sign(request);
            Response response = EchoValidationSuite.this.makeRequest(this, request);
            int encryptionIndex = -1;
            int gzipIndex = -1;
            List<String> codings =
                Utils.commaSeparatedTokens(response.getHeader("Content-Encoding"));
            for (int i = 0; i < codings.size(); i++) {
              if (codings.get(i).equalsIgnoreCase("gzip")) {
                gzipIndex = i;
              } else if (codings.get(i).equalsIgnoreCase("ewp-rsa-aes128gcm")) {
                encryptionIndex = i;
              }
            }
            if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
              EchoValidationSuite.this.expectError(this, combination, request, response,
                  Lists.newArrayList(401, 403));
            } else {
              EchoValidationSuite.this.expectHttp200(this, combination, request, response,
                  EchoValidationSuite.this.parentEchoValidator.getCoveredHeiIDs(),
                  Collections.<String>emptyList());
            }
            if (gzipIndex == -1) {
              throw new Failure(
                  "The client explicitly accepted gzip, but the server didn't compress "
                      + "its response. That's not an error, but it might be useful to "
                      + "support gzip encoding to save bandwidth.",
                  Status.NOTICE, response);
            }
            if ((encryptionIndex != -1) && (gzipIndex > encryptionIndex)) {
              throw new Failure(
                  "The response was valid, but the order of its encodings was \"weird\". "
                      + "Your response was first encrypted, and gzipped later. "
                      + "(Gzipping encrypted content doesn't work well, you should "
                      + "switch the order of your encoders)",
                  Status.WARNING, response);
            }
            return Optional.of(response);
          }
        });
      }
    }
  }

  private void validateResponseCommonsForxHxx(Combination combination, // NOPMD
      Request request, Response response) throws Failure {
    try {
      this.getEwpHttpSigResponseAuthorizer().authorize(request, response);
    } catch (InvalidResponseError e) {
      throw new Failure(e.getMessage(), Status.FAILURE, response);
    }
  }

  protected Request createValidRequestForCombination(InlineValidationStep step,
      Combination combination) {
    return this.createValidRequestForCombination(step, combination, (byte[]) null);
  }

  protected Request createValidRequestForCombination(InlineValidationStep step,
      Combination combination, byte[] body) {

    Request request = new Request(combination.getHttpMethod(), combination.getUrl());
    if (body != null) {
      request.setBody(body);
    }
    if (combination.getHttpMethod().equals("POST") || combination.getHttpMethod().equals("PUT")) {
      request.putHeader("Content-Type", "application/x-www-form-urlencoded");
    }
    step.addRequestSnapshot(request);

    // reqencr

    this.getRequestEncoderForCombination(step, request, combination).encode(request);
    step.addRequestSnapshot(request);

    // srvauth

    if (combination.getSrvAuth().equals(CombEntry.SRVAUTH_TLSCERT)) {
      // pass
    } else if (combination.getSrvAuth().equals(CombEntry.SRVAUTH_HTTPSIG)) {
      request.putHeader("Want-Digest", "SHA-256");
      request.putHeader("Accept-Signature", "rsa-sha256");
    } else {
      throw new RuntimeException("Not supported");
    }

    // resencr

    if (combination.getResEncr().equals(CombEntry.RESENCR_TLS)) {
      // pass
    } else if (combination.getResEncr().equals(CombEntry.RESENCR_EWP)) {
      request.putHeader("Accept-Encoding", "ewp-rsa-aes128gcm, identity;q=0.1");
      if (!combination.getCliAuth().equals(CombEntry.CLIAUTH_HTTPSIG)) {
        request.putHeader("Accept-Response-Encryption-Key", Base64.getEncoder()
            .encodeToString(this.parentEchoValidator.getClientRsaPublicKeyInUse().getEncoded()));
      }
    } else {
      throw new RuntimeException("Not supported");
    }

    // cliauth

    this.getRequestSignerForCombination(step, request, combination).sign(request);
    step.addRequestSnapshot(request);

    return request;
  }

  protected Request createValidRequestForCombination(InlineValidationStep step,
      Combination combination, String body) {
    if (body == null) {
      return this.createValidRequestForCombination(step, combination);
    } else {
      return this.createValidRequestForCombination(step, combination,
          body.getBytes(StandardCharsets.UTF_8));
    }
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

        // GATTT, PATTT, GSTTT, PSTTT
        this.validateCombination(new Combination("GET", this.urlToBeValidated, this.matchedApiEntry,
            CombEntry.CLIAUTH_NONE, CombEntry.SRVAUTH_TLSCERT, CombEntry.REQENCR_TLS,
            CombEntry.RESENCR_TLS));
        this.validateCombination(new Combination("POST", this.urlToBeValidated,
            this.matchedApiEntry, CombEntry.CLIAUTH_NONE, CombEntry.SRVAUTH_TLSCERT,
            CombEntry.REQENCR_TLS, CombEntry.RESENCR_TLS));
        this.validateCombination(new Combination("GET", this.urlToBeValidated, this.matchedApiEntry,
            CombEntry.CLIAUTH_TLSCERT_SELFSIGNED, CombEntry.SRVAUTH_TLSCERT, CombEntry.REQENCR_TLS,
            CombEntry.RESENCR_TLS));
        this.validateCombination(new Combination("POST", this.urlToBeValidated,
            this.matchedApiEntry, CombEntry.CLIAUTH_TLSCERT_SELFSIGNED, CombEntry.SRVAUTH_TLSCERT,
            CombEntry.REQENCR_TLS, CombEntry.RESENCR_TLS));

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

            List<CombEntry> cliAuthMethodsToValidate = new ArrayList<>();
            if (sec.supportsCliAuthNone()) {
              warnings.add("Anonymous Client Authentication SHOULD NOT be enabled for Echo API.");
            }
            // Even though, we will still run some tests on it.
            cliAuthMethodsToValidate.add(CombEntry.CLIAUTH_NONE);
            if (sec.supportsCliAuthTlsCert()) {
              if (sec.supportsCliAuthTlsCertSelfSigned()) {
                cliAuthMethodsToValidate.add(CombEntry.CLIAUTH_TLSCERT_SELFSIGNED);
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
              cliAuthMethodsToValidate.add(CombEntry.CLIAUTH_HTTPSIG);
            } else {
              warnings.add("It is RECOMMENDED for all EWP server endpoints to support HTTP "
                  + "Signature Client Authentication. Your endpoint doesn't.");
            }
            if (cliAuthMethodsToValidate.size() <= 1) {
              errors.add("Your Echo API does not support ANY of the client authentication "
                  + "methods recognized by the Validator.");
            }

            // srvauth

            List<CombEntry> srvAuthMethodsToValidate = new ArrayList<>();
            if (sec.supportsSrvAuthTlsCert()) {
              srvAuthMethodsToValidate.add(CombEntry.SRVAUTH_TLSCERT);
            }
            if (sec.supportsSrvAuthHttpSig()) {
              srvAuthMethodsToValidate.add(CombEntry.SRVAUTH_HTTPSIG);
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

            List<CombEntry> reqEncrMethodsToValidate = new ArrayList<>();
            if (sec.supportsReqEncrTls()) {
              reqEncrMethodsToValidate.add(CombEntry.REQENCR_TLS);
            }
            if (sec.supportsReqEncrEwp()) {
              reqEncrMethodsToValidate.add(CombEntry.REQENCR_EWP);
            }
            if (reqEncrMethodsToValidate.size() == 0) {
              errors.add("Your Echo API does not support ANY of the request encryption "
                  + "methods recognized by the Validator.");
            }

            // resencr

            List<CombEntry> resEncrMethodsToValidate = new ArrayList<>();
            if (sec.supportsResEncrTls()) {
              resEncrMethodsToValidate.add(CombEntry.RESENCR_TLS);
            }
            if (sec.supportsResEncrEwp()) {
              resEncrMethodsToValidate.add(CombEntry.RESENCR_EWP);
            }
            if (resEncrMethodsToValidate.size() == 0) {
              errors.add("Your Echo API does not support ANY of the response encryption "
                  + "methods recognized by the Validator.");
            }

            // Generate all possible combinations of validatable security methods.

            EchoValidationSuite.this.combinationsToValidate = new ArrayList<>();
            for (CombEntry cliauth : cliAuthMethodsToValidate) {
              for (CombEntry srvauth : srvAuthMethodsToValidate) {
                for (CombEntry reqencr : reqEncrMethodsToValidate) {
                  for (CombEntry resencr : resEncrMethodsToValidate) {
                    boolean supportsGetRequests = true;
                    if (reqencr.equals(CombEntry.REQENCR_EWP)) {
                      supportsGetRequests = false;
                    }
                    if (supportsGetRequests) {
                      EchoValidationSuite.this.combinationsToValidate
                          .add(new Combination("GET", EchoValidationSuite.this.urlToBeValidated,
                              EchoValidationSuite.this.matchedApiEntry, cliauth, srvauth, reqencr,
                              resencr));
                    }
                    EchoValidationSuite.this.combinationsToValidate
                        .add(new Combination("POST", EchoValidationSuite.this.urlToBeValidated,
                            EchoValidationSuite.this.matchedApiEntry, cliauth, srvauth, reqencr,
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

        for (Combination combination : this.combinationsToValidate) {
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
