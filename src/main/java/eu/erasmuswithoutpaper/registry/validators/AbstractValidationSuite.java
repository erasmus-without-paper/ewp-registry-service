package eu.erasmuswithoutpaper.registry.validators;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import eu.erasmuswithoutpaper.registry.documentbuilder.BuildError;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
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
import eu.erasmuswithoutpaper.registry.validators.InlineValidationStep.Failure;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.echovalidator.HttpSecuritySettings;
import eu.erasmuswithoutpaper.registry.validators.githubtags.GitHubTagsGetter;
import eu.erasmuswithoutpaper.registry.validators.verifiers.Verifier;
import eu.erasmuswithoutpaper.registry.validators.verifiers.VerifierFactory;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import com.google.common.collect.Lists;
import net.adamcin.httpsig.api.Algorithm;
import net.adamcin.httpsig.api.Challenge;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.http.client.utils.URIBuilder;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public abstract class AbstractValidationSuite<S extends SuiteState> {
  protected static final String fakeId = "this-is-some-unknown-and-unexpected-id";
  protected final List<ValidationStepWithStatus> steps;
  protected final EwpDocBuilder docBuilder;
  protected final Internet internet;
  protected final RegistryClient regClient;
  protected final Integer timeoutMillis = 10000;
  private final CatalogueMatcherProvider catalogueMatcherProvider;
  protected ValidatorKeyStore validatorKeyStore;
  protected ValidatorKeyStoreSet validatorKeyStoreSet;
  protected AnonymousRequestSigner reqSignerAnon;
  protected EwpCertificateRequestSigner reqSignerCert;
  protected EwpHttpSigRequestSigner reqSignerHttpSig;
  protected DecodingHelper resDecoderHelper;
  protected S currentState;
  private Match catalogueMatcher;

  protected AbstractValidationSuite(
      ApiValidator<S> validator,
      S currentState,
      ValidationSuiteConfig config) {
    this.catalogueMatcherProvider = config.catalogueMatcherProvider;
    this.steps = new ArrayList<>();
    this.docBuilder = config.docBuilder;
    this.internet = config.internet;
    this.regClient = config.regClient;
    this.currentState = currentState;
    this.validatorKeyStoreSet = validator.getValidatorKeyStoreSet();

    this.setValidatorKeyStore(validator.getValidatorKeyStoreSet().getMainKeyStore());
  }

  @SafeVarargs
  public static <T> List<T> concatArrays(List<T>... lists) {
    return Stream.of(lists).flatMap(List::stream).collect(Collectors.toList());
  }

  protected void setValidatorKeyStore(ValidatorKeyStore validatorKeyStore) {
    this.validatorKeyStore = validatorKeyStore;
    this.reqSignerAnon = new AnonymousRequestSigner();
    this.reqSignerCert =
        new EwpCertificateRequestSigner(
            this.validatorKeyStore.getTlsClientCertificateInUse(),
            this.validatorKeyStore.getTlsKeyPairInUse()
        );
    this.reqSignerHttpSig =
        new EwpHttpSigRequestSigner(this.validatorKeyStore.getClientRsaKeyPairInUse());
    this.resDecoderHelper = new DecodingHelper();
    this.resDecoderHelper.addDecoder(new EwpRsaAesResponseDecoder(Lists.newArrayList(
        this.validatorKeyStore.getClientRsaKeyPairInUse(),
        this.validatorKeyStore.getServerRsaKeyPairInUse()
    )));
    this.resDecoderHelper.addDecoder(new GzipResponseDecoder());

  }

  protected void runTests(HttpSecurityDescription security) throws SuiteBroken {
    boolean hasCompatibleTest = false;
    for (Combination combination : this.currentState.combinations) {
      if (security == null || combination.getSecurityDescription().equals(security)) {
        this.validateCombination(combination);
        hasCompatibleTest = true;
      }
    }
    if (!hasCompatibleTest) {
      throw new RuntimeException(
          "Security " + security.toString() + " is not supported by this endpoint");
    }
  }

  /**
   * Runs all tests.
   *
   * @param security security method to be used in tests. If security == null then all tests are
   *                 run.
   */
  public void run(HttpSecurityDescription security) {
    try {
      runTests(security);
    } catch (SuiteBroken e) {
      this.currentState.broken = true;
    } catch (RuntimeException e) {
      this.steps.add(new GenericErrorFakeStep(e));
    }
  }

  /**
   * Add a new setup step and run it. Setup steps are required to finish without even a notice.
   *
   * @param step Setup step to be added and run.
   * @throws SuiteBroken If setup step fails.
   */
  protected void setup(InlineValidationStep step) throws SuiteBroken {
    // Allow only success.
    addAndRun(Status.NOTICE, step);
  }

  /**
   * Add a new step (to the public list of steps been run) and run it.
   *
   * @param requireSuccess If true, then a {@link SuiteBroken} exception will be raised, if this
   *                       steps fails.
   * @param step           The step to be added and run.
   * @throws SuiteBroken If the step, which was required to succeed, fails.
   */
  protected void addAndRun(boolean requireSuccess, InlineValidationStep step) throws SuiteBroken {
    Status failureStatus = null;
    if (requireSuccess) {
      // Note, that NOTICE and WARNING are still acceptable.
      failureStatus = Status.FAILURE;
    }
    addAndRun(failureStatus, step);
  }

  /**
   * Add a new step (to the public list of steps been run) and run it.
   *
   * @param failedStatus If validation returns `failedStatus` or more severe status,
   *                     then a {@link SuiteBroken} exception will be raised.
   * @param step         The step to be added and run.
   * @throws SuiteBroken If the step, which was required to succeed, fails.
   */
  protected void addAndRun(Status failedStatus, InlineValidationStep step) throws SuiteBroken {
    this.steps.add(step);
    Status status = null;
    try {
      status = step.run();
    } catch (InlineValidationStep.FatalFailure e) {
      throw new SuiteBroken();
    }
    if (failedStatus != null && status.compareTo(failedStatus) >= 0) {
      throw new SuiteBroken();
    }
  }

  protected void checkAuthErrors(List<String> errors, List<String> warnings, List<String> notices)
      throws Failure {
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
  }

  protected HttpSecuritySettings getSecuritySettings() {
    Element httpSecurityElem =
        $(this.currentState.matchedApiEntry).namespaces(KnownNamespace.prefixMap())
            .xpath(getApiInfo().getApiPrefix() + ":http-security").get(0);
    return new HttpSecuritySettings(httpSecurityElem);
  }

  protected void validateCombination(Combination combination) throws SuiteBroken {
    if (combination.getHttpMethod().equals("POST")) {
      validateCombinationPost(combination);
    } else if (combination.getHttpMethod().equals("GET")) {
      validateCombinationGet(combination);
    }
  }

  protected Request createValidRequestForCombination(InlineValidationStep step,
      Combination combination) {
    return this.createValidRequestForCombination(step, combination,
        ContentType.ApplicationWwwFormUrlencoded);
  }

  protected Request createValidRequestForCombination(InlineValidationStep step,
      Combination combination, ContentType postContentType) {
    return this.createValidRequestForCombination(step, combination, (byte[]) null, postContentType);
  }

  protected Request createValidRequestForCombination(InlineValidationStep step,
      Combination combination, byte[] body, ContentType postContentType) {

    Request request = new Request(combination.getHttpMethod(), combination.getUrl());
    if (body != null) {
      request.setBodyAndContentLength(body);
    }
    if (combination.getHttpMethod().equals("POST") || combination.getHttpMethod().equals("PUT")) {
      request.putHeader("Content-Type", postContentType.getContentTypeString());
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
        request.putHeader(
            "Accept-Response-Encryption-Key",
            Base64.getEncoder()
                .encodeToString(this.validatorKeyStore.getClientRsaPublicKeyInUse().getEncoded())
        );
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
      Combination combination, String body, ContentType postContentType) {
    if (body == null) {
      return this.createValidRequestForCombination(step, combination, postContentType);
    } else {
      return this.createValidRequestForCombination(
          step,
          combination,
          body.getBytes(StandardCharsets.UTF_8),
          postContentType
      );
    }
  }

  protected Request createValidRequestForCombination(InlineValidationStep step,
      Combination combination, String body) {
    return createValidRequestForCombination(step, combination, body,
        ContentType.ApplicationWwwFormUrlencoded);
  }

  /**
   * Helper method for creating simple request method validation steps (e.g. run a PUT request and
   * expect error, run a POST and expect success).
   *
   * @param combination combination to test with.
   * @return ValidationStep performing simple request with selected method.
   */
  protected InlineValidationStep createHttpMethodValidationStep(Combination combination) {
    return new InlineValidationStep() {

      @Override
      public String getName() {
        return "Trying " + combination + " with a " + combination.getHttpMethod() + " request. "
            + "Expecting to receive a valid HTTP 405 error response.";
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request =
            AbstractValidationSuite.this.createValidRequestForCombination(this, combination);
        return Optional.of(AbstractValidationSuite.this
            .makeRequestAndExpectError(this, combination, request, 405));
      }
    };
  }

  protected RequestEncoder getRequestEncoderForCombination(InlineValidationStep step,
      Request request, Combination combination) {
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

  private RSAPublicKey pickRandom(Collection<RSAPublicKey> keys) {
    List<RSAPublicKey> lst = new ArrayList<>(keys);
    int index = new Random().nextInt(lst.size());
    return lst.get(index);
  }

  protected RequestSigner getRequestSignerForCombination(InlineValidationStep step, Request request,
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

  protected EwpHttpSigResponseAuthorizer getEwpHttpSigResponseAuthorizer() {
    if (this.currentState.resAuthorizerHttpSig == null) {
      this.currentState.resAuthorizerHttpSig =
          new EwpHttpSigResponseAuthorizer(this.regClient, this.currentState.matchedApiEntry);
    }
    return this.currentState.resAuthorizerHttpSig;
  }

  protected void validateResponseCommonsForxHxx(Combination combination, Request request,
      Response response) throws Failure {
    try {
      this.getEwpHttpSigResponseAuthorizer().authorize(request, response);
    } catch (InvalidResponseError e) {
      throw new Failure(e.getMessage(), Status.FAILURE, response);
    }
  }

  protected List<String> decodeAndValidateResponseCommons(InlineValidationStep step,
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

  protected Response makeRequestAndExpectError(InlineValidationStep step, Combination combination,
      Request request, int expectedErrorCode) throws Failure {
    return this.makeRequestAndExpectError(step, combination, request,
        Lists.newArrayList(expectedErrorCode));
  }

  protected Response makeRequestAndExpectError(InlineValidationStep step, Combination combination,
      Request request, List<Integer> expectedErrorCodes) throws Failure {
    return this.makeRequestAndExpectError(step, combination, request, expectedErrorCodes,
        Status.FAILURE);
  }

  protected Response makeRequestAndExpectError(InlineValidationStep step, Combination combination,
      Request request, List<Integer> expectedErrorCodes,
      Status failureStatus) throws Failure {
    Response response = this.makeRequest(step, request);
    this.expectError(step, combination, request, response, expectedErrorCodes, failureStatus);
    return response;
  }

  protected Response makeRequest(InlineValidationStep step, Request request) throws Failure {
    step.addRequestSnapshot(request);
    try {
      Response response = this.internet.makeRequest(request, timeoutMillis);
      step.addResponseSnapshot(response);
      return response;
    } catch (SocketTimeoutException e) {
      getLogger().debug(
          "Timeout when retrieving response from server: " + ExceptionUtils.getFullStackTrace(e));
      throw new Failure(
          String.format("Timeout when retrieving %s response from url %s.",
              request.getMethod(), request.getUrl()),
          Status.ERROR,
          true
      );
    } catch (IOException e) {
      getLogger().debug(
          "Problems retrieving response from server: " + ExceptionUtils.getFullStackTrace(e));
      throw new Failure(
          "Problems retrieving response from server: " + e.getMessage(),
          Status.ERROR,
          null
      );
    }
  }

  protected Response makeRequest(InlineValidationStep step, Combination combination,
      Request request, List<String> notices) throws Failure {
    Response response = this.makeRequest(step, request);
    notices.addAll(this.decodeAndValidateResponseCommons(step, combination, request, response));
    return response;
  }

  /**
   * Check if the response contains a valid error of expected type.
   *
   * @param step
   *     validation step associated with this response.
   * @param combination
   *     combination with which request was made.
   * @param request
   *     The request that triggered the response.
   * @param response
   *     Response to be tested.
   * @param expectedErrorCodes
   *     Expected HTTP response statuses (any of those).
   * @throws Failure
   *     If HTTP status differs from expected, or if the response body doesn't contain a proper
   *     error response.
   */
  protected void expectError(InlineValidationStep step, Combination combination, Request request,
      Response response, List<Integer> expectedErrorCodes) throws Failure {
    expectError(step, combination, request, response, expectedErrorCodes, Status.FAILURE);
  }

  /**
   * Check if the response contains a valid error of expected type.
   *
   * @param step
   *     validation step associated with this response.
   * @param combination
   *     combination with which request was made.
   * @param request
   *     The request that triggered the response.
   * @param response
   *     Response to be tested.
   * @param expectedErrorCodes
   *     Expected HTTP response statuses (any of those).
   * @param failureStatus
   *     Type of error to report when statuses do not match.
   * @throws Failure
   *     If HTTP status differs from expected, or if the response body doesn't contain a proper
   *     error response.
   */
  protected void expectError(InlineValidationStep step, Combination combination, Request request,
      Response response, List<Integer> expectedErrorCodes, Status failureStatus) throws Failure {
    final List<String> notices =
        this.decodeAndValidateResponseCommons(step, combination, request, response);
    if (!expectedErrorCodes.contains(response.getStatus())) {
      int gotFirstDigit = response.getStatus() / 100;
      int expectedFirstDigit = expectedErrorCodes.get(0) / 100;
      Status status =
          (gotFirstDigit == expectedFirstDigit) ? Status.WARNING : failureStatus;
      String message = "HTTP " + expectedErrorCodes.stream()
          .map(Object::toString)
          .collect(Collectors.joining(" or HTTP "));
      message += " expected, but HTTP " + response.getStatus() + " received.";
      throw new Failure(message, status, response);
    }
    BuildParams params = new BuildParams(response.getBody());
    params.setExpectedKnownElement(KnownElement.COMMON_ERROR_RESPONSE);
    BuildResult result = this.docBuilder.build(params);
    if (!result.isValid()) {
      throw new Failure(
          "HTTP response status was okay, but the content has failed Schema validation. "
              + "It is recommended to return a proper <error-response> in case of errors. " + this
              .formatDocBuildErrors(result.getErrors()),
          Status.WARNING,
          response
      );
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
        if (!parsed.getAlgorithms().isEmpty() && (!parsed.getAlgorithms()
            .contains(Algorithm.RSA_SHA256))) {
          throw new Failure(
              "Your WWW-Authenticate describes required Signature algorithms, "
                  + "but the list doesn't contain the required rsa-sha256 algorithm.",
              Status.WARNING,
              response
          );
        }
        if (!parsed.getHeaders().isEmpty() && (!parsed.getHeaders().containsAll(Lists
            .newArrayList("(request-target)", "host", "digest", "x-request-id", "date")))) {
          throw new Failure(
              "If you want to include the \"headers\" "
                  + "property in your WWW-Authenticate header, then it should contain at least "
                  + "all required values: (request-target), host, digest, x-request-id and date",
              Status.WARNING,
              response
          );
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

  protected String formatDocBuildErrors(List<BuildError> errors) {
    StringBuilder sb = new StringBuilder();
    sb.append("Our document parser has reported the following errors:");
    for (int i = 0;
         i < errors.size();
         i++) {
      sb.append('\n').append(i + 1).append(". ");
      sb.append("(Line ").append(errors.get(i).getLineNumber()).append(") ");
      sb.append(errors.get(i).getMessage());
    }
    return sb.toString();
  }

  protected static Element makeXmlFromBytes(byte[] bytes) {
    return makeXmlFromBytes(bytes, false);
  }

  protected static Element makeXmlFromBytes(byte[] bytes, boolean namespaceAware) {
    try {
      final InputStream stream = new ByteArrayInputStream(bytes);
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      documentBuilderFactory.setNamespaceAware(namespaceAware);
      DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
      Document document = builder.parse(stream);
      return document.getDocumentElement();
    } catch (ParserConfigurationException | SAXException | IOException e) {
      return null;
    }
  }

  protected List<String> selectFromDocument(Element rootElement, String selector) {
    if (rootElement == null) {
      return new ArrayList<>();
    }
    try {
      XPathFactory xpathFactory = XPathFactory.newInstance();
      XPath xpath = xpathFactory.newXPath();
      NodeList nodeList = (NodeList) xpath.evaluate(selector, rootElement, XPathConstants.NODESET);
      return getTextFromNodeList(nodeList);
    } catch (XPathExpressionException e) {
      return new ArrayList<>();
    }
  }

  private List<String> getTextFromNodeList(NodeList nodeList) {
    ArrayList<String> ret = new ArrayList<>();
    for (int i = 0;
         i < nodeList.getLength();
         i++) {
      ret.add(nodeList.item(i).getTextContent());
    }
    return ret;
  }

  protected List<String> selectApiUrlForHeiFromCatalogue(String api, ApiEndpoint endpoint,
      String hei) {
    Match apis = getCatalogueMatcher().xpath(
        "/r:catalogue/r:host/r:institutions-covered/" + "r:hei-id[normalize-space(text())='" + hei
            + "']" + "/../../r:apis-implemented/*[local-name()='" + api
            + "']/*[local-name()='" + getUrlElementName(endpoint) + "']");
    if (apis.isEmpty()) {
      return null;
    }
    return apis.texts();
  }

  protected Element getApiEntryFromUrlFormCatalogue(String url, ApiEndpoint endpoint) {
    String endpointUrlElementName = getUrlElementName(endpoint);
    String selector = "/r:catalogue/r:host/r:apis-implemented/*/*["
        + "local-name()='" + endpointUrlElementName + "'"
        + " and normalize-space(text())='" + url + "'"
        + "]/..";
    List<Element> apis = getCatalogueMatcher().xpath(selector).get();
    if (apis.isEmpty()) {
      return null;
    }
    return apis.get(0);
  }

  protected Match getCatalogueMatcher() {
    if (this.catalogueMatcher == null) {
      this.catalogueMatcher = catalogueMatcherProvider.getMatcher();
    }
    return this.catalogueMatcher;
  }

  protected List<String> fetchHeiIdsCoveredByApiByUrl(String url) {
    return getCatalogueMatcher().xpath("/r:catalogue/r:host/r:apis-implemented/*/"
        + "*[local-name()='" + getUrlElementName() + "' and normalize-space(text())='" + url + "']"
        + "/../../../r:institutions-covered/r:hei-id").texts();
  }

  protected int getMaxIds(String what) {
    Match maxIdsMatch = getManifestParameter("max-" + what);
    if (maxIdsMatch.isEmpty()) {
      return 1;
    }
    return Integer.parseInt(maxIdsMatch.get(0).getTextContent());
  }

  protected Match getManifestParameter(String what) {
    return $(this.currentState.matchedApiEntry).namespaces(KnownNamespace.prefixMap())
        .xpath(getApiInfo().getApiPrefix() + ":" + what);
  }

  protected Request createRequestWithParameters(InlineValidationStep step, Combination combination,
      Parameters parameters) {
    if (combination.getHttpMethod().equals("POST")) {
      return this.createValidRequestForCombination(
          step, combination, parameters.getPostBody(),
          parameters.getPostContentType()
        );
    } else if (combination.getHttpMethod().equals("GET")) {
      return this.createValidRequestForCombination(
          step, combination.withChangedUrl(parameters.getGetUrl(combination.getUrl()))
      );
    }
    throw new RuntimeException("Should never reach here");
  }

  protected void checkNotices(Response response, List<String> notices) throws Failure {
    if (notices.size() == 0) {
      return;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("Notices:\n");
    for (String message : notices) {
      sb.append("- ").append(message).append('\n');
    }
    throw new Failure(sb.toString(), Status.NOTICE, response);
  }

  protected void expect200(Response response) throws Failure {
    expect200(response, Status.FAILURE);
  }

  protected void expect200(Response response, Status failureStatus) throws Failure {
    if (response.getStatus() != 200) {
      StringBuilder sb = new StringBuilder();
      sb.append("HTTP 200 expected, but HTTP ").append(response.getStatus()).append(" received.");
      if (response.getStatus() == 403) {
        sb.append(" Make sure you validate clients' credentials against a fresh "
            + "Registry catalogue version.");
      }
      throw new Failure(sb.toString(), failureStatus, response);
    }
  }


  protected void expectValidResponse(Response response, Verifier verifier, Status failureStatus,
      List<String> notices) throws Failure {
    expect200(response, failureStatus);
    verifyContents(response, verifier, failureStatus);
    checkNotices(response, notices);
  }

  protected Response makeRequestAndVerifyResponse(InlineValidationStep step,
      Combination combination, Request request, Verifier verifier) throws Failure {
    return makeRequestAndVerifyResponse(step, combination, request, verifier, Status.FAILURE);
  }

  protected Response makeRequestAndVerifyResponse(InlineValidationStep step,
      Combination combination, Request request, Verifier verifier,
      Status failureStatus) throws Failure {
    List<String> notices = new ArrayList<>();
    Response response = makeRequest(step, combination, request, notices);
    this.expectValidResponse(response, verifier, failureStatus, notices);
    return response;
  }

  protected void verifyContents(Response response, Verifier verifier,
      Status failureStatus) throws Failure {
    BuildParams params = new BuildParams(response.getBody());
    params.setExpectedKnownElement(getApiInfo().getResponseKnownElement());
    BuildResult result = this.docBuilder.build(params);
    if (!result.isValid()) {
      throw new Failure(
          "HTTP response status was okay, but the content has failed Schema validation. "
              + this.formatDocBuildErrors(result.getErrors()),
          Status.FAILURE,
          response
      );
    }
    Match root = $(result.getDocument().get()).namespaces(KnownNamespace.prefixMap());
    verifier.performVerificaion(this, root, response, failureStatus);
  }

  protected void testParameters200(Combination combination, String name, Parameters params,
      Verifier verifier) throws SuiteBroken {
    testParameters200(combination, name, params, verifier, Status.FAILURE, false, null);
  }

  protected void testParameters200(Combination combination, String name, Parameters params,
      Verifier verifier, boolean shouldSkip, String skipReason) throws SuiteBroken {
    testParameters200(combination, name, params, verifier, Status.FAILURE, shouldSkip, skipReason);
  }

  protected void testParameters200(Combination combination, String name, Parameters params,
      Verifier verifier, Status failureStatus) throws SuiteBroken {
    testParameters200(combination, name, params, verifier, failureStatus, false, null);
  }

  protected void testParameters200(Combination combination, String name, Parameters params,
      Verifier verifier, Status failureStatus,
      boolean shouldSkip, String skipReason) throws SuiteBroken {
    this.addAndRun(false, new InlineValidationStep() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      protected boolean shouldSkip() {
        return shouldSkip;
      }

      @Override
      protected String getSkipReason() {
        return skipReason;
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request = createRequestWithParameters(this, combination, params);
        return Optional.of(
            makeRequestAndVerifyResponse(this, combination, request, verifier, failureStatus));
      }
    });
  }

  protected void testSkipped(String name, String reason) throws SuiteBroken {
    this.addAndRun(false, new InlineValidationStep() {
      @Override
      protected boolean shouldSkip() {
        return true;
      }

      @Override
      protected String getSkipReason() {
        return reason;
      }

      @Override
      public String getName() {
        return name;
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        return Optional.empty();
      }
    });
  }

  protected void testParametersError(Combination combination, String name, Parameters params,
      int error) throws SuiteBroken {
    testParametersError(combination, name, params, Arrays.asList(error));
  }

  protected void testParametersError(Combination combination, String name, Parameters params,
      int error, boolean shouldSkip, String skipReason) throws SuiteBroken {
    testParametersError(combination, name, params, Arrays.asList(error), shouldSkip, skipReason);
  }

  protected void testParametersError(Combination combination, String name, Parameters params,
      int error, Status failureStatus) throws SuiteBroken {
    testParametersError(combination, name, params, Arrays.asList(error), failureStatus);
  }

  protected void testParametersError(Combination combination, String name, Parameters params,
      List<Integer> errors, boolean shouldSkip, String skipReason) throws SuiteBroken {
    testParametersError(combination, name, params, errors, Status.FAILURE, shouldSkip, skipReason);
  }

  protected void testParametersError(Combination combination, String name, Parameters params,
      List<Integer> errors) throws SuiteBroken {
    testParametersError(combination, name, params, errors, Status.FAILURE);
  }

  protected void testParametersError(Combination combination, String name, Parameters params,
      List<Integer> errors, Status failureStatus) throws SuiteBroken {
    testParametersError(combination, name, params, errors, failureStatus, false, null);
  }

  protected void testParametersError(Combination combination, String name, Parameters params,
      List<Integer> errors, Status failureStatus,
      boolean shouldSkip, String skipReason) throws SuiteBroken {
    this.addAndRun(false, new InlineValidationStep() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      protected boolean shouldSkip() {
        return shouldSkip;
      }

      @Override
      protected String getSkipReason() {
        return skipReason;
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request = createRequestWithParameters(this, combination, params);
        return Optional.of(
            makeRequestAndExpectError(this, combination, request, errors, failureStatus));
      }
    });
  }

  protected void testParameters200AsOtherEwpParticipant(Combination combination, String name,
      Parameters parameters, Verifier verifier,
      ValidationStepWithStatus.Status failureStatus) throws SuiteBroken {
    testParameters200AsOtherEwpParticipant(
        combination, name, parameters, verifier, failureStatus, false, null
    );
  }

  protected void testParameters200AsOtherEwpParticipant(Combination combination, String name,
      Parameters parameters, Verifier verifier,
      ValidationStepWithStatus.Status failureStatus,
      boolean shouldSkip, String skipReason) throws SuiteBroken {
    final ValidatorKeyStore currentValidatorKeyStore = this.validatorKeyStore;
    final ValidatorKeyStore otherValidationKeyStore =
        this.validatorKeyStoreSet.getSecondaryKeyStore();

    String realSkipReason =
        "Keys for another EWP participant not provided. Run the Validator locally and provide "
            + "secondary keys.";
    boolean realShouldSkip = otherValidationKeyStore == null;
    if (shouldSkip) {
      realShouldSkip = shouldSkip;
      realSkipReason = skipReason;
    }

    try {
      this.setValidatorKeyStore(otherValidationKeyStore);
      testParameters200(
          combination,
          name,
          parameters,
          verifier,
          failureStatus,
          realShouldSkip,
          realSkipReason
      );
    } finally {
      this.setValidatorKeyStore(currentValidatorKeyStore);
    }
  }

  protected void testParametersErrorAsOtherEwpParticipant(Combination combination, String name,
      Parameters parameters, int error,
      ValidationStepWithStatus.Status failureStatus,
      boolean shouldSkip, String skipReason) throws SuiteBroken {
    final ValidatorKeyStore currentValidatorKeyStore = this.validatorKeyStore;
    final ValidatorKeyStore otherValidationKeyStore =
        this.validatorKeyStoreSet.getSecondaryKeyStore();

    String realSkipReason =
        "Keys for another EWP participant not provided. Run the Validator locally and provide "
            + "secondary keys.";
    boolean realShouldSkip = otherValidationKeyStore == null;
    if (shouldSkip) {
      realShouldSkip = shouldSkip;
      realSkipReason = skipReason;
    }

    try {
      this.setValidatorKeyStore(otherValidationKeyStore);
      testParametersError(
          combination,
          name,
          parameters,
          Collections.singletonList(error),
          failureStatus,
          realShouldSkip,
          realSkipReason
      );
    } finally {
      this.setValidatorKeyStore(currentValidatorKeyStore);
    }
  }

  public abstract ValidatedApiInfo getApiInfo();

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

  protected void validateCombinationGet(Combination combination)
      throws SuiteBroken {
    validateCombinationAny(combination);
  }

  protected abstract void validateCombinationAny(Combination combination) throws SuiteBroken;

  protected abstract Logger getLogger();

  public List<ValidationStepWithStatus> getResults() {
    return this.steps;
  }

  protected void generalTestsIds(Combination combination,
      String heiParameterName, String heiId,
      String secondParameterNamePrefix,
      String id, int maxIds,
      boolean shouldUnknownHeiIdsFail,
      VerifierFactory verifierFactory) throws SuiteBroken {
    generalTestsIdsAndCodes(combination,
        heiParameterName, heiId,
        secondParameterNamePrefix,
        id, maxIds,
        null, 0,
        shouldUnknownHeiIdsFail,
        true,
        verifierFactory);
  }

  protected void generalTestsIdsAndCodes(Combination combination, String heiId,
      String secondParameterNamePrefix,
      String id, int maxIds, String code, int maxCodes,
      VerifierFactory verifierFactory) throws SuiteBroken {
    generalTestsIdsAndCodes(combination,
        "hei_id", heiId,
        secondParameterNamePrefix,
        id, maxIds,
        code, maxCodes,
        true,
        false,
        verifierFactory);
  }

  protected void generalTestsIdsAndCodes(Combination combination,
      String heiParameterName, String heiId,
      String secondParameterNamePrefix,
      String id, int maxIds,
      String code, int maxCodes,
      boolean shouldUnknownHeiIdsFail,
      boolean skipCodeTests,
      VerifierFactory verifierFactory) throws SuiteBroken {
    if (!skipCodeTests) {
      testParameters200(
          combination,
          "Request for one of known " + secondParameterNamePrefix + "_codes, expect 200 OK.",
          new ParameterList(
              new Parameter(heiParameterName, heiId),
              new Parameter(secondParameterNamePrefix + "_code", code)
          ),
          verifierFactory.expectResponseToContainExactly(Collections.singletonList(id)),
          code == null,
          secondParameterNamePrefix + "_code not found."
      );
    }

    testParameters200(
        combination,
        "Request one unknown " + secondParameterNamePrefix + "_id, expect 200 and empty response.",
        new ParameterList(
            new Parameter(heiParameterName, heiId),
            new Parameter(secondParameterNamePrefix + "_id", fakeId)
        ),
        verifierFactory.expectResponseToBeEmpty()
    );

    testParameters200(
        combination,
        "Request one known and one unknown " + secondParameterNamePrefix
            + "_id, expect 200 and only one " + secondParameterNamePrefix + " in response.",
        new ParameterList(
            new Parameter(heiParameterName, heiId),
            new Parameter(secondParameterNamePrefix + "_id", id),
            new Parameter(secondParameterNamePrefix + "_id", fakeId)
        ),
        verifierFactory.expectResponseToContainExactly(Collections.singletonList(id)),
        maxIds == 1,
        "max-ids is equal to 1."

    );

    if (!skipCodeTests) {
      testParameters200(
          combination,
          "Request one known and one unknown " + secondParameterNamePrefix
              + "_code, expect 200 and only one " + secondParameterNamePrefix + " in response.",
          new ParameterList(
              new Parameter(heiParameterName, heiId),
              new Parameter(secondParameterNamePrefix + "_code", code),
              new Parameter(secondParameterNamePrefix + "_code", fakeId)
          ),
          verifierFactory.expectResponseToContainExactly(Collections.singletonList(id)),
          maxCodes == 1 || code == null,
          "max-codes equal to 1 or " + secondParameterNamePrefix + "_code not found."
      );
    }

    testParametersError(
        combination,
        "Request without " + heiParameterName + " and "
            + secondParameterNamePrefix + "_ids, expect 400.",
        new ParameterList(),
        400
    );

    testParametersError(
        combination,
        "Request without " + heiParameterName + ", expect 400.",
        new ParameterList(new Parameter(secondParameterNamePrefix + "_id", id)),
        400
    );

    testParametersError(
        combination,
        String.format("Request without %s%s, expect 400.",
            secondParameterNamePrefix + "_ids",
            skipCodeTests ? "" : (" and " + secondParameterNamePrefix + "_codes")
        ),
        new ParameterList(new Parameter(heiParameterName, heiId)),
        400
    );

    if (shouldUnknownHeiIdsFail) {
      testParametersError(
          combination,
          "Request for one of known " + secondParameterNamePrefix + "_ids with unknown "
              + heiParameterName + ", expect 400.",
          new ParameterList(
              new Parameter(heiParameterName, fakeId),
              new Parameter(secondParameterNamePrefix + "_id", id)
          ),
          400
      );
    } else {
      testParameters200(
          combination,
          "Request for one of known " + secondParameterNamePrefix + "_ids with unknown "
              + heiParameterName + ", expect 200 and empty response.",
          new ParameterList(
              new Parameter(heiParameterName, fakeId),
              new Parameter(secondParameterNamePrefix + "_id", id)
          ),
          verifierFactory.expectResponseToBeEmpty()
      );
    }

    if (!skipCodeTests) {
      testParametersError(
          combination,
          "Request for one of known " + secondParameterNamePrefix + "_codes with unknown "
              + heiParameterName + ", expect 400.",
          new ParameterList(
              new Parameter(heiParameterName, fakeId),
              new Parameter(secondParameterNamePrefix + "_code", code)
          ),
          400,
          code == null,
          secondParameterNamePrefix + "_code not found."
      );
    }

    testParametersError(
        combination,
        "Request more than <max-" + secondParameterNamePrefix + "-ids> known "
            + secondParameterNamePrefix + "_ids, expect 400.",
        new ParameterList(
            concatArrays(
              Arrays.asList(new Parameter(heiParameterName, heiId)),
              Collections.nCopies(
                  maxIds + 1, new Parameter(secondParameterNamePrefix + "_id", id)
              )
            )
        ),
        400
    );

    if (!skipCodeTests) {
      testParametersError(
          combination,
          "Request more than <max-" + secondParameterNamePrefix + "-codes> known "
              + secondParameterNamePrefix + "_codes, expect 400.",
          new ParameterList(
            concatArrays(
                Arrays.asList(new Parameter(heiParameterName, heiId)),
                Collections.nCopies(
                    maxCodes + 1,
                    new Parameter(secondParameterNamePrefix + "_code", code)
                )
            )
          ),
          400,
          code == null,
          secondParameterNamePrefix + "_code not found."
      );
    }

    testParametersError(
        combination,
        "Request more than <max-" + secondParameterNamePrefix + "-ids> unknown "
            + secondParameterNamePrefix + "_ids, expect 400.",
        new ParameterList(
            concatArrays(
              Arrays.asList(new Parameter(heiParameterName, heiId)),
              Collections.nCopies(
                  maxIds + 1, new Parameter(secondParameterNamePrefix + "_id", fakeId)
              )
            )
        ),
        400
    );

    testParametersError(
        combination,
        "Request more than <max-" + secondParameterNamePrefix + "-codes> unknown "
            + secondParameterNamePrefix + "_codes, expect 400.",
        new ParameterList(
            concatArrays(
                Arrays.asList(new Parameter(heiParameterName, heiId)),
                Collections.nCopies(
                    maxCodes + 1, new Parameter(secondParameterNamePrefix + "_code", fakeId)
                )
            )
        ),
        400
    );

    testParameters200(
        combination,
        "Request exactly "
            + "<max-" + secondParameterNamePrefix + "-ids> "
            + "known " + secondParameterNamePrefix + "_ids, "
            + "expect 200 and non-empty response.",

        new ParameterList(
            concatArrays(
                Arrays.asList(new Parameter(heiParameterName, heiId)),
                Collections.nCopies(maxIds, new Parameter(secondParameterNamePrefix + "_id", id))
            )
        ),
        verifierFactory.expectResponseToContain(Collections.singletonList(id))
    );

    if (!skipCodeTests) {
      testParameters200(
          combination,
          "Request exactly "
              + "<max-" + secondParameterNamePrefix + "-codes> "
              + "known " + secondParameterNamePrefix + "_codes, "
              + "expect 200 and non-empty response.",
          new ParameterList(
            concatArrays(
                Arrays.asList(new Parameter(heiParameterName, heiId)),
                Collections.nCopies(
                    maxCodes, new Parameter(secondParameterNamePrefix + "_code", code)
                )
            )
          ),
          verifierFactory.expectResponseToContain(Collections.singletonList(id)),
          code == null,
          secondParameterNamePrefix + "_code not found."
      );
    }

    testParametersError(
        combination,
        "Request with single incorrect parameter, expect 400.",
        new ParameterList(new Parameter(secondParameterNamePrefix + "_id_param", id)),
        400
    );

    if (!skipCodeTests) {
      testParametersError(
          combination,
          "Request with correct " + secondParameterNamePrefix + "_id and correct "
              + secondParameterNamePrefix + "_code, expect 400.",
          new ParameterList(
              new Parameter(heiParameterName, heiId),
              new Parameter(secondParameterNamePrefix + "_id", id),
              new Parameter(secondParameterNamePrefix + "_code", code)
          ),
          400,
          code == null,
          secondParameterNamePrefix + "_code not found."
      );
    }

    testParametersError(
        combination,
        "Request with correct " + heiParameterName + " twice, expect 400.",
        new ParameterList(
            new Parameter(heiParameterName, heiId),
            new Parameter(heiParameterName, heiId),
            new Parameter(secondParameterNamePrefix + "_id", id)
        ),
        400
    );

    testParametersError(
        combination,
        String.format(
            "Request with correct %s and incorrect %s, expect 400.",
            heiParameterName, heiParameterName
        ),
        new ParameterList(
            new Parameter(heiParameterName, heiId),
            new Parameter(heiParameterName, fakeId),
            new Parameter(secondParameterNamePrefix + "_id", id)
        ),
        400
    );
  }

  private static class ModifiedSinceTestsSkipInfo {
    final boolean skipErrorTests;
    final String skipErrorTestsReason;
    final boolean skipNoErrorTests;
    final String skipNoErrorTestsReason;
    static final String modifiedSinceNotSupportedSkipReason =
        "modified_since parameter not supported.";

    public ModifiedSinceTestsSkipInfo(
        boolean modifiedSinceSupported,
        boolean shouldSkip, String skipReason
    ) {
      if (!modifiedSinceSupported) {
        skipErrorTests = true;
        skipErrorTestsReason = modifiedSinceNotSupportedSkipReason;
        skipNoErrorTests = true;
        skipNoErrorTestsReason = modifiedSinceNotSupportedSkipReason;
      } else {
        skipErrorTests = false;
        skipErrorTestsReason = null;
        skipNoErrorTests = shouldSkip;
        skipNoErrorTestsReason = skipReason;
      }
    }

  }

  // TODO move other modified_since tests into this method and use it everywhere.
  protected void modifiedSinceTests(Combination combination,
      String heiIdParameterName,
      String knownHeiId,
      boolean modifiedSinceSupported,
      boolean shouldSkip, String skipReason,
      VerifierFactory verifierFactory) throws SuiteBroken {

    ModifiedSinceTestsSkipInfo skipInfo = new ModifiedSinceTestsSkipInfo(
        modifiedSinceSupported, shouldSkip, skipReason
    );

    testParametersError(
        combination,
        "Request with multiple modified_since parameters, expect 400.",
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("modified_since", "2019-02-12T15:19:21+01:00"),
            new Parameter("modified_since", "2019-02-12T15:19:21+01:00")
        ),
        400,
        skipInfo.skipErrorTests,
        skipInfo.skipErrorTestsReason
    );

    int yearInFuture = Calendar.getInstance().get(Calendar.YEAR) + 20;

    testParameters200(
        combination,
        String.format(
            "Request with known %s and modified_since in the future, expect 200 OK and empty "
                + "response",
            heiIdParameterName
        ),
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("modified_since", yearInFuture + "-02-12T15:19:21+01:00")
        ),
        verifierFactory.expectResponseToBeEmpty(),
        Status.WARNING,
        skipInfo.skipNoErrorTests,
        skipInfo.skipNoErrorTestsReason
    );

    testParameters200(
        combination,
        String.format(
            "Request with known %s and modified_since far in the past, expect 200 OK and "
                + "non-empty response.",
            heiIdParameterName
        ),
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("modified_since", "2000-02-12T15:19:21+01:00")
        ),
        verifierFactory.expectResponseToBeNotEmpty(),
        skipInfo.skipNoErrorTests,
        skipInfo.skipNoErrorTestsReason
    );

    testParameters200(
        combination,
        String.format(
            "Request with known %s and correct date, expect 200.",
            heiIdParameterName
        ),
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("modified_since", "2004-02-12T15:19:21+01:00")
        ),
        verifierFactory.expectCorrectResponse()
    );

    testParametersError(
        combination,
        "Request with invalid value of modified_since, expect 400.",
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("modified_since", fakeId)
        ),
        400
    );

    testParametersError(
        combination,
        "Request with modified_since being only a date, expect 400.",
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("modified_since", "2004-02-12")
        ),
        400
    );

    testParametersError(
        combination,
        "Request with modified_since being a dateTime in wrong format, expect 400.",
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("modified_since", "05/29/2015 05:50")
        ),
        400
    );
  }

  // TODO use it in IIAs complex test
  protected void testModifiedSinceReturnsSpecifiedId(Combination combination,
      String heiIdParameterName,
      String knownHeiId,
      boolean modifiedSinceSupported,
      boolean shouldSkip, String skipReason,
      VerifierFactory verifierFactory,
      String expectedId) throws SuiteBroken {
    // TODO is shouldSkip parameters necessary? Check after using this method in IIAs.
    ModifiedSinceTestsSkipInfo skipInfo = new ModifiedSinceTestsSkipInfo(
        modifiedSinceSupported, shouldSkip, skipReason
    );

    testParameters200(
        combination,
        String.format(
            "Request with known %s and modified_since far in the past, "
                + "expect 200 OK and non-empty response.",
            heiIdParameterName
        ),
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("modified_since", "2000-02-12T15:19:21+01:00")
        ),
        verifierFactory.expectResponseToContain(Arrays.asList(expectedId)),
        skipInfo.skipNoErrorTests,
        skipInfo.skipNoErrorTestsReason
    );
  }

  // TODO use it in IIAs basic tests
  protected void testReceivingAcademicYears(Combination combination,
      String heiIdParameterName,
      String knownHeiId,
      VerifierFactory verifierFactory
  ) throws SuiteBroken {
    /*
    testParameters200(
        combination,
        "Request with known hei_id and receiving_academic_year_id in southern hemisphere "
            + "format, expect 200 OK.",
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("receiving_academic_year_id", "2010/2010")
        ),
        iiaIdVerifierFactory.expectCorrectResponse()
    );
     */

    testParameters200(
        combination,
        String.format(
            "Request with known %s and receiving_academic_year_id in northern hemisphere "
                + "format, expect 200 OK.",
            heiIdParameterName
        ),
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("receiving_academic_year_id", "2010/2011")
        ),
        verifierFactory.expectCorrectResponse()
    );

    testParametersError(
        combination,
        "Request with receiving_academic_year_id in incorrect format, expect 400.",
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("receiving_academic_year_id", "test/test")
        ),
        400
    );

    int unknownAcademicYear = 1653; //Arbitrary, but most probably unknown.

    String unknownAcademicYearString =
        String.format("%04d/%04d", unknownAcademicYear, unknownAcademicYear + 1);
    testParameters200(
        combination,
        String.format(
            "Request with known %s and unknown receiving_academic_year_id parameter, "
                + "expect 200 OK and empty response.",
            heiIdParameterName
        ),
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("receiving_academic_year_id", unknownAcademicYearString)
        ),
        verifierFactory.expectResponseToBeEmpty()
    );
  }

  // TODO use it in IIAs complex test
  protected void testReceivingAcademicYearsReturnsExpectedId(Combination combination,
      String heiIdParameterName,
      String knownHeiId,
      VerifierFactory verifierFactory,
      String knownAcademicYear,
      String expectedId) throws SuiteBroken {
    testParameters200(
        combination,
        String.format(
            "Request with known %s and known receiving_academic_year_id parameter, "
                + "expect 200 OK and non-empty response.",
            heiIdParameterName
        ),
        new ParameterList(
            new Parameter(heiIdParameterName, knownHeiId),
            new Parameter("receiving_academic_year_id", knownAcademicYear)
        ),
        verifierFactory.expectResponseToContain(Arrays.asList(expectedId)),
        knownAcademicYear == null,
        "No known receiving_academic_year_id, try to pass additional parameters."
    );
  }

  protected void testsRequestingReceivingHeiIds(Combination combination,
      String requestingHeiIdParameterName,
      String knownRequestingHeiIdParameter,
      String respondingHeiIdParameterName,
      String knownRespondingHeiIdParameter,
      boolean shouldSkip,
      String skipReason,
      VerifierFactory verifierFactory,
      boolean shouldUnknownRespondingHeiIdsFail
  ) throws SuiteBroken {
    testParametersError(
        combination,
        String.format(
            "Request with known %s twice, expect 400.",
            respondingHeiIdParameterName
        ),
        new ParameterList(
            new Parameter(respondingHeiIdParameterName, knownRespondingHeiIdParameter),
            new Parameter(respondingHeiIdParameterName, knownRespondingHeiIdParameter)
        ),
        400
    );

    testParametersError(
        combination,
        String.format(
            "Request without %s and known %s, expect 400.",
            respondingHeiIdParameterName, requestingHeiIdParameterName
        ),
        new ParameterList(
            new Parameter(requestingHeiIdParameterName, knownRequestingHeiIdParameter)
        ),
        400
    );

    testParametersError(
        combination,
        String.format(
            "Request with known %s and unknown %s, expect 400.",
            respondingHeiIdParameterName, respondingHeiIdParameterName
        ),
        new ParameterList(
            new Parameter(respondingHeiIdParameterName, knownRespondingHeiIdParameter),
            new Parameter(respondingHeiIdParameterName, fakeId)
        ),
        400
    );

    testParametersError(
        combination,
        "Request without parameters, expect 400.",
        new ParameterList(),
        400
    );

    testParameters200(
        combination,
        String.format(
            "Request with known %s and unknown %s, expect 200 and empty response.",
            respondingHeiIdParameterName, requestingHeiIdParameterName
        ),
        new ParameterList(
            new Parameter(respondingHeiIdParameterName, knownRespondingHeiIdParameter),
            new Parameter(requestingHeiIdParameterName, fakeId)
        ),
        verifierFactory.expectResponseToBeEmpty(),
        shouldSkip,
        skipReason
    );

    testParameters200(
        combination,
        String.format(
            "Request with known %s and without %s, expect 200 and non-empty response.",
            respondingHeiIdParameterName, requestingHeiIdParameterName
        ),
        new ParameterList(
            new Parameter(respondingHeiIdParameterName, knownRespondingHeiIdParameter)
        ),
        verifierFactory.expectResponseToBeNotEmpty(),
        shouldSkip,
        skipReason
    );

    if (shouldUnknownRespondingHeiIdsFail) {
      testParametersError(
          combination,
          String.format(
              "Request with unknown %s, expect 400.",
              respondingHeiIdParameterName
          ),
          new ParameterList(
              new Parameter(respondingHeiIdParameterName, fakeId)
          ),
          400,
          shouldSkip,
          skipReason
      );
    } else {
      testParameters200(
          combination,
          String.format(
              "Request with unknown %s, expect 200 and empty response.",
              respondingHeiIdParameterName
          ),
          new ParameterList(
              new Parameter(respondingHeiIdParameterName, fakeId)
          ),
          verifierFactory.expectResponseToBeEmpty(),
          shouldSkip,
          skipReason
      );
    }

    testParameters200(
        combination,
        String.format(
            "Request with known %s and known and unknown %s, expect 200 and non-empty response.",
            respondingHeiIdParameterName, requestingHeiIdParameterName
        ),
        new ParameterList(
            new Parameter(respondingHeiIdParameterName, knownRespondingHeiIdParameter),
            new Parameter(requestingHeiIdParameterName, knownRequestingHeiIdParameter),
            new Parameter(requestingHeiIdParameterName, fakeId)
        ),
        verifierFactory.expectResponseToBeNotEmpty(),
        shouldSkip,
        skipReason
    );

    testParameters200(
        combination,
        String.format(
            "Request with known %s and two unknown %s, expect 200 OK and empty response.",
            respondingHeiIdParameterName, requestingHeiIdParameterName
        ),
        new ParameterList(
            new Parameter(respondingHeiIdParameterName, knownRespondingHeiIdParameter),
            new Parameter(requestingHeiIdParameterName, fakeId),
            new Parameter(requestingHeiIdParameterName, fakeId)
        ),
        verifierFactory.expectResponseToBeEmpty(),
        shouldSkip,
        skipReason
    );
  }

  String getUrlElementName(ApiEndpoint endpoint) {
    if (endpoint == ApiEndpoint.NoEndpoint) {
      return "url";
    }
    return endpoint.getName() + "-url";
  }

  String getUrlElementName() {
    return getUrlElementName(this.getApiInfo().getEndpoint());
  }


  public static class ValidationSuiteConfig {
    public final EwpDocBuilder docBuilder;
    public final Internet internet;
    public final RegistryClient regClient;
    public final GitHubTagsGetter gitHubTagsGetter;
    private final CatalogueMatcherProvider catalogueMatcherProvider;

    /**
     * Creates data structure with all configurations required for AbstractValidationSuite to work.
     *
     * @param docBuilder               Needed for validating API responses against the schemas.
     * @param internet                 Needed to make API requests across the network.
     * @param regClient                Needed to fetch (and verify) APIs' security settings.
     * @param catalogueMatcherProvider to get {@link Match} for catalogue.
     * @param gitHubTagsGetter         to fetch API tags from GitHub.
     */
    public ValidationSuiteConfig(
        EwpDocBuilder docBuilder,
        Internet internet,
        RegistryClient regClient,
        CatalogueMatcherProvider catalogueMatcherProvider,
        GitHubTagsGetter gitHubTagsGetter) {
      this.docBuilder = docBuilder;
      this.internet = internet;
      this.regClient = regClient;
      this.catalogueMatcherProvider = catalogueMatcherProvider;
      this.gitHubTagsGetter = gitHubTagsGetter;
    }
  }


  /**
   * This is a "fake" {@link ValidationStepWithStatus} which is dynamically added to the list of
   * steps whenever some unexpected runtime exception occurs.
   */
  protected static final class GenericErrorFakeStep implements ValidationStepWithStatus {

    private static final Logger logger = LoggerFactory.getLogger(GenericErrorFakeStep.class);
    private final RuntimeException cause;

    public GenericErrorFakeStep(RuntimeException cause) {
      logger.error(cause.getMessage(), cause);
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
  public static class SuiteBroken extends Exception {
  }


  protected static class Parameter {
    public final String name;
    public final String value;

    public Parameter(String name, String value) {
      this.name = name;
      this.value = value;
    }
  }

  protected interface Parameters {
    String getPostBody();

    String getGetUrl(String url);

    ContentType getPostContentType();
  }

  protected static class ParameterList implements Parameters {
    private final List<Parameter> parameters;

    public ParameterList(List<Parameter> parameters) {
      this.parameters = parameters;
    }

    public ParameterList(Parameter... parameters) {
      this.parameters = Arrays.asList(parameters);
    }

    @Override
    public String getPostBody() {
      URIBuilder builder = new URIBuilder();
      for (Parameter parameter : this.parameters) {
        builder.addParameter(parameter.name, parameter.value);
      }
      String parameters = builder.toString();
      if (parameters.isEmpty()) {
        return parameters;
      }
      return parameters.substring(1); // Remove leading '?'
    }

    @Override
    public String getGetUrl(String url) {
      URIBuilder builder = null;
      try {
        builder = new URIBuilder(url);
      } catch (URISyntaxException e) {
        throw new RuntimeException("Invalid URI syntax, shouldn't happen.");
      }
      for (Parameter parameter : this.parameters) {
        builder.addParameter(parameter.name, parameter.value);
      }
      return builder.toString();
    }

    @Override
    public ContentType getPostContentType() {
      return ContentType.ApplicationWwwFormUrlencoded;
    }
  }

  protected static class XmlParameters implements Parameters {
    private final Document xmlBody;

    public XmlParameters(Document xmlBody) {
      this.xmlBody = xmlBody;
    }

    public Document getXmlBody() {
      return xmlBody;
    }

    @Override
    public String getPostBody() {
      try {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(this.xmlBody), new StreamResult(sw));
        return sw.toString();
      } catch (TransformerException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public String getGetUrl(String url) {
      throw new RuntimeException("XMLParameters cannot be used as GET query string.");
    }

    @Override
    public ContentType getPostContentType() {
      return ContentType.TextXml;
    }
  }
}
