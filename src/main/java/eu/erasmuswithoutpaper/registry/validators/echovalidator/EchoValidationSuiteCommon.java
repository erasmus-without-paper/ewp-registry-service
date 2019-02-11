package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import static org.joox.JOOX.$;

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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpCertificateRequestSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.EwpHttpSigRequestSigner;
import eu.erasmuswithoutpaper.registry.internet.sec.RequestSigner;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.validators.AbstractValidationSuite;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.CombEntry;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep;
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep.Failure;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import eu.erasmuswithoutpaper.rsaaes.BadEwpRsaAesBody;
import eu.erasmuswithoutpaper.rsaaes.EwpRsaAes128GcmDecoder;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.adamcin.httpsig.api.Authorization;
import org.apache.commons.codec.digest.DigestUtils;
import org.joox.Match;

/**
 * Describes the set of test/steps to be run on an Echo API implementation in order to properly
 * validate it.
 */
abstract class EchoValidationSuiteCommon extends AbstractValidationSuite<EchoSuiteState> {
  private Set<CombEntry> allCombEntriesCache = null;

  EchoValidationSuiteCommon(ApiValidator<EchoSuiteState> echoValidator, EwpDocBuilder docBuilder,
      Internet internet, RegistryClient regClient, ManifestRepository repo,
      EchoSuiteState state) {
    super(echoValidator, docBuilder, internet, regClient, repo, state);
  }

  private void checkEdgeCasesForAxxx(Combination combination)
      throws SuiteBroken {
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectError(this, combination, request, Lists.newArrayList(401, 403)));
      }
    });
  }

  private void checkEdgeCasesForHxxx(Combination combination)
      throws SuiteBroken {
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        KeyPair otherKeyPair =
            EchoValidationSuiteCommon.this.parentValidator.getUnregisteredKeyPair();
        // Replace the previously set Authorization header with a different one.
        RequestSigner badSigner = new EwpHttpSigRequestSigner(otherKeyPair);
        badSigner.sign(request);
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectError(this, combination.withChangedResEncr(CombEntry.RESENCR_TLS),
                request, Lists.newArrayList(401, 403)
            ));
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        // Leave keyId as is, but use a new random key for signature generation.
        String previousKeyId = Authorization.parse(request.getHeader("Authorization")).getKeyId();
        KeyPair otherKeyPair =
            EchoValidationSuiteCommon.this.parentValidator.getUnregisteredKeyPair();
        RequestSigner mySigner = new EwpHttpSigRequestSigner(otherKeyPair) {
          @Override
          public String getKeyId() {
            return previousKeyId;
          }
        };
        // Replace the previously set Authorization header with a different one.
        mySigner.sign(request);
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectError(this, combination, request, Lists.newArrayList(400, 401)));
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        request.putHeader("missing-header-that-should-exist", "Temporarilly exists");
        EchoValidationSuiteCommon.this.reqSignerHttpSig.sign(request);
        request.removeHeader("missing-header-that-should-exist");
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectError(this, combination, request, Lists.newArrayList(400, 401)));
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        String value = request.getHeader("Date");
        request.removeHeader("Date");
        request.putHeader("Original-Date", value);
        EchoValidationSuiteCommon.this.reqSignerHttpSig.sign(request);
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectHttp200(this, combination, request,
                EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                Lists.newArrayList()
            ));
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
              EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
          List<String> headersToSign = new ArrayList<>(stdHeaders);
          headersToSign.remove(headerToSkip);
          RequestSigner mySigner = new EwpHttpSigRequestSigner(
              EchoValidationSuiteCommon.this.reqSignerHttpSig.getKeyPair()) {
            @Override
            protected List<String> getHeadersToSign(Request request) {
              return headersToSign;
            }
          };
          mySigner.sign(request);
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectError(this, combination, request, Lists.newArrayList(400, 401)));
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        request.putHeader("Some-Custom-Header", "Value");
        EchoValidationSuiteCommon.this.reqSignerHttpSig.sign(request);
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectHttp200(this, combination, request,
                EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                Lists.newArrayList()
            ));
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        KeyPair keyPair = EchoValidationSuiteCommon.this.parentValidator.getServerRsaKeyPairInUse();
        RequestSigner badSigner = new EwpHttpSigRequestSigner(keyPair);
        badSigner.sign(request);
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectError(this, combination, request, 403));
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        ZonedDateTime dateTime = ZonedDateTime.now(ZoneId.of("UTC"));
        dateTime = dateTime.minusMinutes(20);
        String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(dateTime);
        request.putHeader("Original-Date", date);

        Authorization authz = Authorization.parse(request.getHeader("Authorization"));
        List<String> headers = new ArrayList<>(authz.getHeaders());
        headers.add("original-date");
        headers.remove("date");
        RequestSigner mySigner = new EwpHttpSigRequestSigner(
            EchoValidationSuiteCommon.this.reqSignerHttpSig.getKeyPair()) {
          @Override
          protected List<String> getHeadersToSign(Request request) {
            return headers;
          }
        };
        mySigner.sign(request);
        try {
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectError(this, combination, request, 400));
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        request.putHeader(
            "X-Request-Id",
            request.getHeader("X-Request-Id").replaceAll("-", "").toUpperCase(Locale.US)
        );
        EchoValidationSuiteCommon.this.reqSignerHttpSig.sign(request);
        try {
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectError(this, combination, request, 400));
        } catch (Failure f) {
          // We don't want this to be a FAILURE. WARNING is enough.
          if (f.getStatus().equals(Status.FAILURE)) {
            throw new Failure(f.getMessage(), Status.WARNING,
                f.getAttachedServerResponse().orElse(null)
            );
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
          Request request = EchoValidationSuiteCommon.this
              .createValidRequestForCombination(this, combination, "echo=a&echo=b&echo=a");
          // Change the body after digest and signature were generated. This should invalidate
          // the digest.
          request.setBody("something-else".getBytes(StandardCharsets.UTF_8));
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectError(this, combination, request, 400));
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
          Request request = EchoValidationSuiteCommon.this
              .createValidRequestForCombination(this, combination, "echo=b&echo=b");
          // Override the Digest and Authorization with the output of a custom signer.
          RequestSigner mySigner = new EwpHttpSigRequestSigner(
              EchoValidationSuiteCommon.this.reqSignerHttpSig.getKeyPair()) {
            @Override
            protected void includeDigestHeader(Request request) {
              super.includeDigestHeader(request);
              request.putHeader(
                  "Digest",
                  request.getHeader("Digest") + ", Unknown-Digest-Algorithm=SomeValue"
              );
            }
          };
          mySigner.sign(request);
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectHttp200(this, combination, request,
                  EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                  Lists.newArrayList("b", "b")
              ));
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
          Request request = EchoValidationSuiteCommon.this
              .createValidRequestForCombination(this, combination, "echo=b&echo=b");
          RequestSigner badSigner = new EwpHttpSigRequestSigner(
              EchoValidationSuiteCommon.this.reqSignerHttpSig.getKeyPair()) {
            @Override
            protected void includeDigestHeader(Request request) {
              request.putHeader("Digest", "SHA=" + Base64.getEncoder()
                  .encodeToString(DigestUtils.getSha1Digest().digest(request.getBodyOrEmpty())));
            }
          };
          badSigner.sign(request);
          try {
            return Optional.of(EchoValidationSuiteCommon.this
                .makeRequestAndExpectError(this, combination, request, 400));
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
          Request request = EchoValidationSuiteCommon.this
              .createValidRequestForCombination(this, combination, "echo=b&echo=b");
          RequestSigner mySigner = new EwpHttpSigRequestSigner(
              EchoValidationSuiteCommon.this.reqSignerHttpSig.getKeyPair()) {
            @Override
            protected void includeDigestHeader(Request request) {
              super.includeDigestHeader(request);
              String newValue = request.getHeader("Digest").replace("SHA-256=", "shA-256=");
              request.putHeader("Digest", newValue);
            }
          };
          mySigner.sign(request);
          try {
            return Optional.of(EchoValidationSuiteCommon.this
                .makeRequestAndExpectHttp200(this, combination, request,
                    EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                    Lists.newArrayList("b", "b")
                ));
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

  private void checkEdgeCasesForSxxx(Combination combination)
      throws SuiteBroken {
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);

        // Override the work done by the original (valid) request signer.
        KeyPair otherKeyPair =
            EchoValidationSuiteCommon.this.parentValidator.getUnregisteredKeyPair();
        X509Certificate otherCert =
            EchoValidationSuiteCommon.this.parentValidator.generateCertificate(otherKeyPair);
        EwpCertificateRequestSigner mySigner =
            new EwpCertificateRequestSigner(otherCert, otherKeyPair);
        mySigner.sign(request);

        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectError(this, combination, request, Lists.newArrayList(401, 403)));
      }
    });
  }

  private void checkEdgeCasesForxHxx(Combination combination)
      throws SuiteBroken {

    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with no valid algorithm in Accept-Signature header. "
            + "Expecting to receive unsigned response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request =
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        request.putHeader("Accept-Signature", "unknown-algorithm");
        EchoValidationSuiteCommon.this.getRequestSignerForCombination(this, request, combination)
            .sign(request);
        Combination relaxedCombination = combination.withChangedSrvAuth(CombEntry.SRVAUTH_TLSCERT);
        if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectError(this, relaxedCombination, request,
                  Lists.newArrayList(401, 403)
              ));
        } else {
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectHttp200(this, relaxedCombination, request,
                  EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                  Lists.newArrayList()
              ));
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        request.putHeader("Accept-Signature", "rsa-sha256, unknown-algorithm");
        EchoValidationSuiteCommon.this.getRequestSignerForCombination(this, request, combination)
            .sign(request);
        if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectError(this, combination, request, Lists.newArrayList(401, 403)));
        } else {
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectHttp200(this, combination, request,
                  EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                  Lists.newArrayList()
              ));
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
              EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
          request.putHeader("X-Request-Id", UUID.randomUUID().toString());
          EchoValidationSuiteCommon.this.getRequestSignerForCombination(this, request, combination)
              .sign(request);
          if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
            return Optional.of(EchoValidationSuiteCommon.this
                .makeRequestAndExpectError(this, combination, request,
                    Lists.newArrayList(401, 403)
                ));
          } else {
            return Optional.of(EchoValidationSuiteCommon.this
                .makeRequestAndExpectHttp200(this, combination, request,
                    EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                    Lists.newArrayList()
                ));
          }
        }
      });
    }

  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is EchoSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void checkEdgeCasesForxxEx(Combination combination)
      throws SuiteBroken {

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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        // Transform it to a GET request, while leaving the body etc. intact.
        request.setMethod("GET");
        EchoValidationSuiteCommon.this.getRequestSignerForCombination(this, request, combination)
            .sign(request);
        List<Integer> acceptableResponses = Lists.newArrayList(405);
        if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
          acceptableResponses.add(403);
          acceptableResponses.add(401);
        }
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectError(this, combination, request, acceptableResponses));
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

        Request request = EchoValidationSuiteCommon.this
            .createValidRequestForCombination(this, combination, "echo=a&echo=b&echo=a");
        byte[] body = request.getBody().get();
        // Truncate right after the encryptedAesKeyLength.
        body = Arrays.copyOf(body, 32 + 2);
        request.setBody(body);
        EchoValidationSuiteCommon.this.getRequestSignerForCombination(this, request, combination)
            .sign(request);
        List<Integer> acceptableResponses = Lists.newArrayList(400);
        if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
          acceptableResponses.add(403);
          acceptableResponses.add(401);
        }
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectError(this, combination, request, acceptableResponses));
      }
    });

    if (!this.endpointSupports(CombEntry.REQENCR_TLS, this.currentState)) {

      // This endpoint explicitly requires all requests to be encrypted.

      this.addAndRun(false, new InlineValidationStep() {

        @Override
        public String getName() {
          return "Trying " + combination + " without encryption. Your endpoint "
              + "explicitly requires all requests to be encrypted, so we're expecting "
              + "HTTP 415 error response.";
        }

        @Override
        protected Optional<Response> innerRun() throws Failure {
          Request request = EchoValidationSuiteCommon.this.createValidRequestForCombination(
              this,
              combination.withChangedReqEncr(CombEntry.REQENCR_TLS)
          );
          List<Integer> acceptableResponses = Lists.newArrayList(415);
          if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
            acceptableResponses.add(403);
            acceptableResponses.add(401);
          }
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectError(this, combination, request, acceptableResponses));
        }
      });
    }
  }

  //FindBugs is not smart enough to infer that actual type of this.currentState
  //is EchoSuiteState not just SuiteState
  @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
  private void checkEdgeCasesForxxxE(Combination combination)
      throws SuiteBroken {

    boolean encryptionRequired = !this.endpointSupports(CombEntry.RESENCR_TLS, this.currentState);
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
              EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
          request.putHeader("Accept-Encoding", "invalid-coding");
          EchoValidationSuiteCommon.this.getRequestSignerForCombination(this, request, combination)
              .sign(request);
          if (encryptionRequired) {
            return Optional.of(EchoValidationSuiteCommon.this.makeRequestAndExpectError(this,
                combination.withChangedResEncr(CombEntry.RESENCR_TLS), request, 406
            ));
          } else {
            return Optional.of(EchoValidationSuiteCommon.this.makeRequestAndExpectHttp200(this,
                combination.withChangedResEncr(CombEntry.RESENCR_TLS), request,
                EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                Collections.emptyList()
            ));
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
              EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
          request.putHeader("Accept-Encoding", " ewp-rsa-AES128GCM ; q=1 , *;q=0");
          EchoValidationSuiteCommon.this.getRequestSignerForCombination(this, request, combination)
              .sign(request);
          return Optional.of(EchoValidationSuiteCommon.this
              .makeRequestAndExpectHttp200(this, combination, request,
                  EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                  Collections.emptyList()
              ));
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
                EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
            request.putHeader("Accept-Encoding", " ewp-rsa-aes128gcm;q=0, identity;q=1");
            EchoValidationSuiteCommon.this
                .getRequestSignerForCombination(this, request, combination)
                .sign(request);
            return Optional.of(EchoValidationSuiteCommon.this.makeRequestAndExpectHttp200(this,
                combination.withChangedResEncr(CombEntry.RESENCR_TLS), request,
                EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                Collections.emptyList()
            ));
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
              EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
          RSAPublicKey attackersKey =
              EchoValidationSuiteCommon.this.parentValidator.getServerRsaPublicKeyInUse();
          byte[] attackersKeyFingerprint =
              DigestUtils.getSha256Digest().digest(attackersKey.getEncoded());
          if (request.getHeader("Accept-Response-Encryption-Key") == null) {
            request.putHeader(
                "Accept-Response-Encryption-Key",
                Base64.getEncoder().encodeToString(attackersKey.getEncoded())
            );
          } else {
            // Should not happen. When httpsig client authentication is used, then this
            // header should not be present.
            throw new RuntimeException();
          }
          // Make sure that attacker's key is NOT used.
          Response response = EchoValidationSuiteCommon.this.makeRequest(this, request);
          try {
            byte[] actualFingerprint =
                EwpRsaAes128GcmDecoder.extractRecipientPublicKeySha256(response.getBody());
            if (Arrays.equals(actualFingerprint, attackersKeyFingerprint)) {
              throw new Failure("The response was encrypted with the attacker's key. "
                  + "Your installation is vulnerable to man-in-the-middle attacks.", Status.FAILURE,
                  response
              );
            }
          } catch (BadEwpRsaAesBody e) {
            // Ignore. This exception will be re-thrown in a moment (and properly cast
            // into Failure).
          }
          EchoValidationSuiteCommon.this.expectHttp200(this, combination, request, response,
              EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
              Collections.emptyList()
          );
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
              EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
          RSAPublicKey newKey =
              EchoValidationSuiteCommon.this.parentValidator.getServerRsaPublicKeyInUse();
          byte[] newKeyFingerprint = DigestUtils.getSha256Digest().digest(newKey.getEncoded());
          if (request.getHeader("Accept-Response-Encryption-Key") == null) {
            request.putHeader(
                "Accept-Response-Encryption-Key",
                Base64.getEncoder().encodeToString(newKey.getEncoded())
            );
          } else {
            // Should not happen. When httpsig client authentication is used, then this
            // header should not be present.
            throw new RuntimeException();
          }
          EchoValidationSuiteCommon.this.getRequestSignerForCombination(this, request, combination)
              .sign(request);
          // Make sure that the "old" key is not used.
          Response response = EchoValidationSuiteCommon.this.makeRequest(this, request);
          KeyPair oldKeyPair =
              EchoValidationSuiteCommon.this.parentValidator.getClientRsaKeyPairInUse();
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
          EchoValidationSuiteCommon.this.expectHttp200(this, combination, request, response,
              EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
              Collections.emptyList()
          );
          return Optional.of(response);
        }
      });
    }
  }

  private boolean endpointSupports(CombEntry combEntry, EchoSuiteState state) {
    Set<CombEntry> set = this.getAllCombEntriesSupportedByEndpoint(state);
    return set.contains(combEntry);
  }

  private Set<CombEntry> getAllCombEntriesSupportedByEndpoint(EchoSuiteState state) {
    if (this.allCombEntriesCache == null) {
      this.allCombEntriesCache = new HashSet<>();
      for (Combination combination : state.combinations) {
        this.allCombEntriesCache.add(combination.getCliAuth());
        this.allCombEntriesCache.add(combination.getSrvAuth());
        this.allCombEntriesCache.add(combination.getReqEncr());
        this.allCombEntriesCache.add(combination.getResEncr());
      }
    }
    return Collections.unmodifiableSet(this.allCombEntriesCache);
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
        Request request = EchoValidationSuiteCommon.this
            .createValidRequestForCombination(this, combination, "echo=a&echo=b&echo=a");
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectHttp200(this, combination, request,
                EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                expectedEchoValues
            ));
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
        Request request = EchoValidationSuiteCommon.this
            .createValidRequestForCombination(this, combination, "echo=a&echo=b&echo=a");
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
        EchoValidationSuiteCommon.this.getRequestSignerForCombination(this, request, combination)
            .sign(request);
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectHttp200(this, combination, request,
                EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                expectedEchoValues
            ));
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
            EchoValidationSuiteCommon.this.createValidRequestForCombination(this, combination);
        // Allow the response to be gzipped.
        String prev = request.getHeader("Accept-Encoding");
        request.putHeader("Accept-Encoding", "gzip" + ((prev != null) ? ", " + prev : ""));
        EchoValidationSuiteCommon.this.getRequestSignerForCombination(this, request, combination)
            .sign(request);
        Response response = EchoValidationSuiteCommon.this.makeRequest(this, request);
        int encryptionIndex = -1;
        int gzipIndex = -1;
        List<String> codings = Utils.commaSeparatedTokens(response.getHeader("Content-Encoding"));
        for (int i = 0; i < codings.size(); i++) {
          if (codings.get(i).equalsIgnoreCase("gzip")) {
            gzipIndex = i;
          } else if (codings.get(i).equalsIgnoreCase("ewp-rsa-aes128gcm")) {
            encryptionIndex = i;
          }
        }
        if (combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
          EchoValidationSuiteCommon.this
              .expectError(this, combination, request, response, Lists.newArrayList(401, 403));
        } else {
          EchoValidationSuiteCommon.this.expectHttp200(this, combination, request, response,
              EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
              Collections.emptyList()
          );
        }
        if (gzipIndex == -1) {
          throw new Failure("The client explicitly accepted gzip, but the server didn't compress "
              + "its response. That's not an error, but it might be useful to "
              + "support gzip encoding to save bandwidth.", Status.NOTICE, response);
        }
        if ((encryptionIndex != -1) && (gzipIndex > encryptionIndex)) {
          throw new Failure("The response was valid, but the order of its encodings was \"weird\". "
              + "Your response was first encrypted, and gzipped later. "
              + "(Gzipping encrypted content doesn't work well, you should "
              + "switch the order of your encoders)", Status.WARNING, response);
        }
        return Optional.of(response);
      }
    });
  }

  @Override
  protected void validateCombinationGet(Combination combination)
      throws SuiteBroken {
    this.addAndRun(false, new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " GET request with a list of echo values [a, b, a]. "
            + "Expecting to receive a valid HTTP 200 Echo API response, "
            + "with proper hei-id and matching echo values.";
      }

      @Override
      @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
      protected Optional<Response> innerRun() throws Failure {
        ArrayList<String> expectedEchoValues = new ArrayList<>();
        expectedEchoValues.add("a");
        expectedEchoValues.add("b");
        expectedEchoValues.add("a");
        Request request = EchoValidationSuiteCommon.this.createValidRequestForCombination(
            this,
            combination.withChangedUrl(
                EchoValidationSuiteCommon.this.currentState.url + "?echo=a&echo=b&echo=a")
        );
        return Optional.of(EchoValidationSuiteCommon.this
            .makeRequestAndExpectHttp200(this, combination, request,
                EchoValidationSuiteCommon.this.parentValidator.getCoveredHeiIDs(),
                expectedEchoValues
            ));
      }
    });
  }

  private void validateCombinationEdgeCases(Combination combination)
      throws SuiteBroken {
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

  }

  @Override
  protected void validateCombination(Combination combination)
      throws SuiteBroken {
    if (!combination.getCliAuth().equals(CombEntry.CLIAUTH_NONE)) {
      super.validateCombination(combination);
    }
    validateCombinationEdgeCases(combination);
  }

  private void verifyResponseStatus(Response response) throws Failure {
    if (response.getStatus() != 200) {
      StringBuilder sb = new StringBuilder();
      sb.append("HTTP 200 expected, but HTTP " + response.getStatus() + " received.");
      if (response.getStatus() == 403) {
        sb.append(" Make sure you validate clients' credentials against a fresh "
            + "Registry catalogue version.");
      }
      throw new Failure(sb.toString(), Status.FAILURE, response);
    }
  }

  private void expectHttp200(InlineValidationStep step, Combination combination, Request request,
      Response response, List<String> heiIdsExpected, List<String> echoValuesExpected)
      throws Failure {
    final List<String> notices =
        this.decodeAndValidateResponseCommons(step, combination, request, response);
    verifyResponseStatus(response);
    BuildParams params = new BuildParams(response.getBody());
    params.setExpectedKnownElement(getKnownElement());
    BuildResult result = this.docBuilder.build(params);
    if (!result.isValid()) {
      throw new Failure(
          "HTTP response status was okay, but the content has failed Schema validation. " + this
              .formatDocBuildErrors(result.getErrors()), Status.FAILURE, response);
    }
    Match root = $(result.getDocument().get()).namespaces(KnownNamespace.prefixMap());
    List<String> heiIdsGot = new ArrayList<>();
    String nsPrefix = getApiResponsePrefix() + ":";
    for (Match entry : root.xpath(nsPrefix + "hei-id").each()) {
      heiIdsGot.add(entry.text());
    }
    for (String heiIdGot : heiIdsGot) {
      if (!heiIdsExpected.contains(heiIdGot)) {
        throw new Failure(
            "The response has proper HTTP status and it passed the schema validation. However, "
                + "the set of returned hei-ids doesn't match what we expect. It contains <hei-id>"
                + heiIdGot + "</hei-id>, but it shouldn't. It should contain the following: "
                + heiIdsExpected, Status.FAILURE, response);
      }
    }
    for (String heiIdExpected : heiIdsExpected) {
      if (!heiIdsGot.contains(heiIdExpected)) {
        throw new Failure(
            "The response has proper HTTP status and it passed the schema validation. However, "
                + "the set of returned hei-ids doesn't match what we expect. "
                + "It should contain the following: " + heiIdsExpected, Status.FAILURE, response);
      }
    }
    List<String> echoValuesGot = root.xpath(nsPrefix + "echo").texts();
    if (!echoValuesGot.equals(echoValuesExpected)) {
      throw new Failure("The response has proper HTTP status and it passed the schema validation. "
          + "However, there's something wrong with the echo values produced. "
          + "We expected the response to contain the following echo values: " + echoValuesExpected
          + ", but the following values were found instead: " + echoValuesGot, Status.FAILURE,
          response
      );
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

  /**
   * Make the request and make sure that the response contains a valid HTTP 200 Echo API response.
   *
   * @param request
   *     The request to be made.
   * @param heiIdsExpected
   *     The expected contents of the hei-id list.
   * @param echoValuesExpected
   *     The expected contents of the echo list.
   * @throws Failure
   *     If some expectations are not met.
   */
  private Response makeRequestAndExpectHttp200(InlineValidationStep step, Combination combination,
      Request request, List<String> heiIdsExpected, List<String> echoValuesExpected)
      throws Failure {
    Response response = this.makeRequest(step, request);
    this.expectHttp200(step, combination, request, response, heiIdsExpected, echoValuesExpected);
    return response;
  }

  @Override
  protected String getApiName() {
    return "echo";
  }

}
