package eu.erasmuswithoutpaper.registry.validators;

import static org.joox.JOOX.$;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import eu.erasmuswithoutpaper.registry.common.Utils;
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
import eu.erasmuswithoutpaper.registry.repository.CatalogueNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
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
  protected final ManifestRepository repo;

  protected final ApiValidator<S> parentValidator;
  protected final List<ValidationStepWithStatus> steps;
  protected final EwpDocBuilder docBuilder;
  protected final Internet internet;
  protected final RegistryClient regClient;
  protected final AnonymousRequestSigner reqSignerAnon;
  protected final EwpCertificateRequestSigner reqSignerCert;
  protected final EwpHttpSigRequestSigner reqSignerHttpSig;
  protected final DecodingHelper resDecoderHelper;
  protected Match catalogueMatch = null;
  protected S currentState;

  protected AbstractValidationSuite(
      ApiValidator<S> validator, S currentState,
      ValidationSuiteConfig config) {
    this.repo = config.repo;
    this.steps = new ArrayList<>();
    this.parentValidator = validator;
    this.docBuilder = config.docBuilder;
    this.internet = config.internet;
    this.regClient = config.regClient;
    this.reqSignerAnon = new AnonymousRequestSigner();
    this.reqSignerCert =
        new EwpCertificateRequestSigner(
            this.parentValidator.getTlsClientCertificateInUse(),
            this.parentValidator.getTlsKeyPairInUse()
        );
    this.reqSignerHttpSig =
        new EwpHttpSigRequestSigner(this.parentValidator.getClientRsaKeyPairInUse());
    this.resDecoderHelper = new DecodingHelper();
    this.resDecoderHelper.addDecoder(new EwpRsaAesResponseDecoder(Lists.newArrayList(
        this.parentValidator.getClientRsaKeyPairInUse(),
        this.parentValidator.getServerRsaKeyPairInUse()
    )));
    this.resDecoderHelper.addDecoder(new GzipResponseDecoder());
    this.currentState = currentState;
  }

  @SafeVarargs
  public static <T> List<T> concatArrays(List<T>... lists) {
    return Stream.of(lists).flatMap(List::stream).collect(Collectors.toList());
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
   * @param security
   *     security method to be used in tests. If security == null then all tests are run.
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
   * Add a new setup step and run it. Setup steps are required to finish without failure.
   *
   * @param step
   *     Setup step to be added and run.
   * @throws SuiteBroken
   *     If setup step fails.
   */
  protected void setup(InlineValidationStep step) throws SuiteBroken {
    addAndRun(true, step);
  }

  /**
   * Add a new step (to the public list of steps been run) and run it.
   *
   * @param requireSuccess
   *     If true, then a {@link SuiteBroken} exception will be raised, if this steps fails.
   * @param step
   *     The step to be added and run.
   * @throws SuiteBroken
   *     If the step, which was required to succeed, fails.
   */
  protected void addAndRun(boolean requireSuccess, InlineValidationStep step) throws SuiteBroken {
    this.steps.add(step);
    Status status = step.run();
    if (requireSuccess && (status.compareTo(Status.FAILURE) >= 0)) {
      // Note, that NOTICE and WARNING are still acceptable.
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
            .xpath(getApiPrefix() + ":http-security").get(0);
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
        request.putHeader(
            "Accept-Response-Encryption-Key",
            Base64.getEncoder()
                .encodeToString(this.parentValidator.getClientRsaPublicKeyInUse().getEncoded())
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
      Combination combination, String body) {
    if (body == null) {
      return this.createValidRequestForCombination(step, combination);
    } else {
      return this.createValidRequestForCombination(
          step,
          combination,
          body.getBytes(StandardCharsets.UTF_8)
      );
    }
  }

  /**
   * Helper method for creating simple request method validation steps (e.g. run a PUT request and
   * expect error, run a POST and expect success).
   *
   * @param combination
   *     combination to test with.
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
      Request request, int status) throws Failure {
    return this.makeRequestAndExpectError(step, combination, request, Lists.newArrayList(status));
  }

  protected Response makeRequestAndExpectError(InlineValidationStep step, Combination combination,
      Request request, List<Integer> statuses) throws Failure {
    Response response = this.makeRequest(step, request);
    this.expectError(step, combination, request, response, statuses);
    return response;
  }

  protected Response makeRequest(InlineValidationStep step, Request request) throws Failure {
    step.addRequestSnapshot(request);
    try {
      Response response = this.internet.makeRequest(request);
      step.addResponseSnapshot(response);
      return response;
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
   * @param statuses
   *     Expected HTTP response statuses (any of those).
   * @throws Failure
   *     If HTTP status differs from expected, or if the response body doesn't contain a proper
   *     error response.
   */
  protected void expectError(InlineValidationStep step, Combination combination, Request request,
      Response response, List<Integer> statuses) throws Failure {
    expectError(step, combination, request, response, statuses, Status.FAILURE);
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
   * @param statuses
   *     Expected HTTP response statuses (any of those).
   * @param failureStatus
   *     Type of error to report when statuses do not match.
   * @throws Failure
   *     If HTTP status differs from expected, or if the response body doesn't contain a proper
   *     error response.
   */
  protected void expectError(InlineValidationStep step, Combination combination, Request request,
      Response response, List<Integer> statuses, Status failureStatus) throws Failure {
    final List<String> notices =
        this.decodeAndValidateResponseCommons(step, combination, request, response);
    if (!statuses.contains(response.getStatus())) {
      int gotFirstDigit = response.getStatus() / 100;
      int expectedFirstDigit = statuses.get(0) / 100;
      Status status =
          (gotFirstDigit == expectedFirstDigit) ? Status.WARNING : failureStatus;
      String message = "HTTP " + String.join(
          " or HTTP ",
          statuses.stream().map(Object::toString).collect(Collectors.toList())
      );
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
    for (int i = 0; i < errors.size(); i++) {
      sb.append('\n').append(i + 1).append(". ");
      sb.append("(Line ").append(errors.get(i).getLineNumber()).append(") ");
      sb.append(errors.get(i).getMessage());
    }
    return sb.toString();
  }

  protected Element makeXmlFromBytes(byte[] bytes) {
    try {
      final InputStream stream = new ByteArrayInputStream(bytes);
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
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
    for (int i = 0; i < nodeList.getLength(); i++) {
      ret.add(nodeList.item(i).getTextContent());
    }
    return ret;
  }

  protected List<String> getApiUrlForHei(String api, String hei) {
    Match apis = getCatalogueMatcher().xpath(
        "/r:catalogue/r:host/r:institutions-covered/" + "r:hei-id[normalize-space(text())='" + hei
            + "']" + "/../../r:apis-implemented/*[local-name()='" + api
            + "']/*[local-name()='url']");
    if (apis.isEmpty()) {
      return null;
    }
    return apis.texts();
  }

  protected Element getApiEntryFromUrl(String url) {
    List<Element> apis = getCatalogueMatcher().xpath("/r:catalogue/r:host/r:apis-implemented/*/"
        + "*[local-name()='url' and normalize-space(text())='" + url + "']" + "/..").get();
    if (apis.isEmpty()) {
      return null;
    }
    return apis.get(0);
  }

  protected Match getCatalogueMatcher() {
    if (catalogueMatch == null) {
      DocumentBuilder docBuilder = Utils.newSecureDocumentBuilder();
      Document doc = null;
      try {
        doc = docBuilder.parse(new ByteArrayInputStream(this.repo.getCatalogue()
            .getBytes(StandardCharsets.UTF_8)));
      } catch (SAXException | IOException | CatalogueNotFound e) {
        throw new RuntimeException(e);
      }

      catalogueMatch = $(doc).namespaces(KnownNamespace.prefixMap());
    }
    return catalogueMatch;
  }

  protected List<String> fetchHeiIdsCoveredByApiByUrl(String url) {
    return getCatalogueMatcher().xpath("/r:catalogue/r:host/r:apis-implemented/*/"
        + "*[local-name()='url' and normalize-space(text())='" + url + "']"
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
        .xpath(getApiPrefix() + ":" + what);
  }

  protected Request createRequestWithParameters(InlineValidationStep step, Combination combination,
      List<Parameter> parameters) {
    URIBuilder builder;
    try {
      if (combination.getHttpMethod().equals("POST")) {
        builder = new URIBuilder();
      } else {
        builder = new URIBuilder(combination.getUrl());
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException("Invalid URI scheme, shouldn't happen.");
    }

    for (Parameter p : parameters) {
      builder.addParameter(p.name, p.value);
    }

    String params = builder.toString();

    if (combination.getHttpMethod().equals("POST")) {
      if (!params.isEmpty()) {
        params = params.substring(1); //remove leading ?
      }
      return this.createValidRequestForCombination(step, combination, params);
    } else if (combination.getHttpMethod().equals("GET")) {
      return this.createValidRequestForCombination(step, combination.withChangedUrl(params));
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

  protected Response verifyResponse(InlineValidationStep step, Combination combination,
      Request request, Verifier verifier) throws Failure {
    return verifyResponse(step, combination, request, verifier, Status.FAILURE);
  }

  protected Response verifyResponse(InlineValidationStep step, Combination combination,
      Request request, Verifier verifier, Status failureStatus) throws Failure {
    List<String> notices = new ArrayList<>();
    Response response = makeRequest(step, combination, request, notices);
    expect200(response, failureStatus);
    verifyContents(response, verifier);
    checkNotices(response, notices);
    return response;
  }

  protected void verifyContents(Response response, Verifier verifier) throws Failure {
    BuildParams params = new BuildParams(response.getBody());
    params.setExpectedKnownElement(getKnownElement());
    BuildResult result = this.docBuilder.build(params);
    if (!result.isValid()) {
      throw new Failure(
          "HTTP response status was okay, but the content has failed Schema validation. " + this
              .formatDocBuildErrors(result.getErrors()),
          Status.FAILURE,
          response
      );
    }
    Match root = $(result.getDocument().get()).namespaces(KnownNamespace.prefixMap());
    verifier.verify(this, root, response);
  }

  protected void testParameters200(Combination combination, String name, List<Parameter> params,
      Verifier verifier) throws SuiteBroken {
    testParameters200(combination, name, params, verifier, Status.FAILURE);
  }

  protected void testParameters200(Combination combination, String name, List<Parameter> params,
      Verifier verifier, Status failureStatus) throws SuiteBroken {
    this.addAndRun(false, new InlineValidationStep() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request = createRequestWithParameters(this, combination, params);
        return Optional.of(verifyResponse(this, combination, request, verifier, failureStatus));
      }
    });
  }

  protected void testParametersError(Combination combination, String name, List<Parameter> params,
      int error) throws SuiteBroken {
    testParametersError(combination, name, params, Arrays.asList(error), Status.FAILURE);
  }

  protected void testParametersError(Combination combination, String name, List<Parameter> params,
      int error, Status failureStatus) throws SuiteBroken {
    testParametersError(combination, name, params, Arrays.asList(error), failureStatus);
  }

  protected void testParametersError(Combination combination, String name, List<Parameter> params,
      List<Integer> errors) throws SuiteBroken {
    testParametersError(combination, name, params, errors, Status.FAILURE);
  }

  protected void testParametersError(Combination combination, String name, List<Parameter> params,
      List<Integer> errors, Status status) throws SuiteBroken {
    this.addAndRun(false, new InlineValidationStep() {
      @Override
      public String getName() {
        return name;
      }

      @Override
      protected Optional<Response> innerRun() throws Failure {
        Request request = createRequestWithParameters(this, combination, params);
        return Optional.of(makeRequestAndExpectError(this, combination, request, errors));
      }
    });
  }

  protected abstract String getApiNamespace();

  protected abstract String getApiName();

  protected abstract String getApiVersion();

  protected abstract void validateCombinationPost(Combination combination)
      throws SuiteBroken;

  protected abstract void validateCombinationGet(Combination combination)
      throws SuiteBroken;

  protected abstract Logger getLogger();

  protected abstract KnownElement getKnownElement();

  public List<ValidationStepWithStatus> getResults() {
    return this.steps;
  }

  public abstract String getApiPrefix();

  public abstract String getApiResponsePrefix();

  protected void generalTestsIdsAndCodes(Combination combination, String name, String selectedHeiId,
      String id, String code, int maxIds, int maxCodes, VerifierFactory verifier)
      throws SuiteBroken {
    if (code != null) {
      testParameters200(
          combination,
          "Request for one of known " + name + "-codes, expect 200 OK.",
          Arrays.asList(
              new Parameter("hei_id", selectedHeiId),
              new Parameter(name + "_code", code)
          ),
          verifier.create(Collections.singletonList(id))
      );
    }

    testParameters200(
        combination,
        "Request one unknown " + name + "-id, expect 200 and empty response.",
        Arrays.asList(
            new Parameter("hei_id", selectedHeiId),
            new Parameter(name + "_id", fakeId)
        ),
        verifier.create(new ArrayList<>())
    );

    if (maxIds > 1) {
      testParameters200(
          combination,
          "Request one known and one unknown " + name + "-id, expect 200 and only one " + name
              + " in response.",
          Arrays.asList(
              new Parameter("hei_id", selectedHeiId),
              new Parameter(name + "_id", id),
              new Parameter(name + "_id", fakeId)
          ),
          verifier.create(Collections.singletonList(id))
      );
    }

    if (maxCodes > 1 && code != null) {
      testParameters200(
          combination,
          "Request one known and one unknown " + name + "-code, expect 200 and only one " + name
              + " in response.",
          Arrays.asList(
              new Parameter("hei_id", selectedHeiId),
              new Parameter(name + "_code", code),
              new Parameter(name + "_code", fakeId)
          ),
          verifier.create(Collections.singletonList(id))
      );
    }

    testParametersError(
        combination,
        "Request without hei-ids and " + name + "-ids, expect 400.",
        new ArrayList<>(),
        400
    );

    testParametersError(
        combination,
        "Request without hei-ids, expect 400.",
        Arrays.asList(new Parameter(name + "_id", id)),
        400
    );

    testParametersError(
        combination,
        "Request without " + name + "-ids and " + name + "-codes, expect 400.",
        Arrays.asList(new Parameter("hei_id", selectedHeiId)),
        400
    );

    testParametersError(
        combination,
        "Request for one of known " + name + "-ids with unknown hei-id, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", fakeId),
            new Parameter(name + "_id", id)
        ),
        400
    );

    if (code != null) {
      testParametersError(
          combination,
          "Request for one of known " + name + "-codes with unknown hei-id, expect 400.",
          Arrays.asList(
              new Parameter("hei_id", fakeId),
              new Parameter(name + "_code", code)
          ),
          400
      );
    }

    testParametersError(
        combination,
        "Request more than <max-" + name + "-ids> known " + name + "-ids, expect 400.",
        concatArrays(
            Arrays.asList(new Parameter("hei_id", selectedHeiId)),
            Collections.nCopies(maxIds + 1, new Parameter(name + "_id", id))
        ),
        400
    );

    if (code != null) {
      testParametersError(
          combination,
          "Request more than <max-" + name + "-codes> known " + name + "-codes, expect 400.",
          concatArrays(
              Arrays.asList(new Parameter("hei_id", selectedHeiId)),
              Collections.nCopies(
                  maxCodes + 1,
                  new Parameter(name + "_code", code)
              )
          ),
          400
      );
    }

    testParametersError(
        combination,
        "Request more than <max-" + name + "-ids> unknown " + name + "-ids, expect 400.",
        concatArrays(
            Arrays.asList(new Parameter("hei_id", selectedHeiId)),
            Collections.nCopies(maxIds + 1, new Parameter(name + "_id", fakeId))
        ),
        400
    );

    testParametersError(
        combination,
        "Request more than <max-" + name + "-codes> unknown " + name + "-codes, expect 400.",
        concatArrays(
            Arrays.asList(new Parameter("hei_id", selectedHeiId)),
            Collections.nCopies(maxCodes + 1, new Parameter(name + "_code", fakeId))
        ),
        400
    );

    testParameters200(
        combination,
        "Request exactly <max-" + name + "-ids> known " + name + "-ids, expect 200 and <max-" + name
            + "-ids> " + name + "-ids in response.",
        concatArrays(
            Arrays.asList(new Parameter("hei_id", selectedHeiId)),
            Collections.nCopies(maxIds, new Parameter(name + "_id", id))
        ),
        verifier.create(Collections.nCopies(maxIds, id))
    );

    if (code != null) {
      testParameters200(
          combination,
          "Request exactly <max-" + name + "-codes> known " + name + "-codes, expect 200 and <max-"
              + name + "-codes> " + name + "-codes in response.",
          concatArrays(
              Arrays.asList(new Parameter("hei_id", selectedHeiId)),
              Collections.nCopies(maxCodes, new Parameter(name + "_code", code)
              )
          ),
          verifier.create(Collections.nCopies(maxCodes, id))
      );
    }

    testParametersError(
        combination,
        "Request with single incorrect parameter, expect 400.",
        Arrays.asList(new Parameter(name + "_id_param", id)),
        400
    );

    testParameters200(
        combination,
        "Request with additional parameter, expect 200 and one " + name + " in response.",
        Arrays.asList(
            new Parameter("hei_id", selectedHeiId),
            new Parameter(name + "_id", id),
            new Parameter(name + "_id_param", id)
        ),
        verifier.create(Collections.singletonList(id))
    );

    if (code != null) {
      testParametersError(
          combination,
          "Request with correct " + name + "_id and correct " + name + "_code, expect 400.",
          Arrays.asList(
              new Parameter("hei_id", selectedHeiId),
              new Parameter(name + "_id", id),
              new Parameter(name + "_code", code)
          ),
          400
      );
    }

    testParametersError(
        combination,
        "Request with correct hei_id twice, expect 400.",
        Arrays.asList(
            new Parameter("hei_id", selectedHeiId),
            new Parameter("hei_id", selectedHeiId),
            new Parameter(name + "_id", id)
        ),
        400
    );
  }


  public static class ValidationSuiteConfig {
    public final EwpDocBuilder docBuilder;
    public final Internet internet;
    public final RegistryClient regClient;
    public final ManifestRepository repo;
    public final GitHubTagsGetter gitHubTagsGetter;

    /**
     * Creates data structure with all configurations required for AbstractValidationSuite to work.
     *
     * @param docBuilder
     *     Needed for validating Echo API responses against the schemas.
     * @param internet
     *     Needed to make Echo API requests across the network.
     * @param regClient
     *     Needed to fetch (and verify) Echo APIs' security settings.
     * @param repo
     *     to store the new content of the fetched manifests.
     * @param gitHubTagsGetter
     *     to fetch API tags from GitHub.
     */
    public ValidationSuiteConfig(
        EwpDocBuilder docBuilder,
        Internet internet,
        RegistryClient regClient,
        ManifestRepository repo,
        GitHubTagsGetter gitHubTagsGetter) {
      this.docBuilder = docBuilder;
      this.internet = internet;
      this.regClient = regClient;
      this.repo = repo;
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
  public static class SuiteBroken extends Exception {}


  protected static class Parameter {
    public final String name;
    public final String value;

    public Parameter(String name, String value) {
      this.name = name;
      this.value = value;
    }
  }

}
