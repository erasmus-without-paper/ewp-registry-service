package eu.erasmuswithoutpaper.registry.web;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletResponse;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.cmatrix.CoverageMatrixGenerator;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildError;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.echovalidator.EchoValidator;
import eu.erasmuswithoutpaper.registry.echovalidator.SecMethodsCombination;
import eu.erasmuswithoutpaper.registry.echovalidator.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.echovalidator.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import eu.erasmuswithoutpaper.registry.repository.CatalogueDependantCache;
import eu.erasmuswithoutpaper.registry.repository.CatalogueNotFound;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.ManifestUpdateStatus;
import eu.erasmuswithoutpaper.registry.updater.ManifestUpdateStatusRepository;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdater;
import eu.erasmuswithoutpaper.registry.updater.UptimeChecker;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.joox.Match;

/**
 * Handles UI requests.
 */
@Controller
@SuppressFBWarnings("SIC_INNER_SHOULD_BE_STATIC_ANON")
public class UiController {

  private final TaskExecutor taskExecutor;
  private final ManifestUpdateStatusRepository manifestStatusRepo;
  private final ManifestSourceProvider sourceProvider;
  private final RegistryUpdater updater;
  private final NotifierService notifier;
  private final UptimeChecker uptimeChecker;
  private final EwpDocBuilder docBuilder;
  private final EchoValidator echoTester;
  private final SelfManifestProvider selfManifestProvider;
  private final ResourceLoader resLoader;
  private final CoverageMatrixGenerator matrixGenerator;
  private final RegistryClient regClient;
  private final CatalogueDependantCache catcache;

  private byte[] cachedCss;
  private String cachedCssFingerprint;
  private byte[] cachedJs;
  private String cachedJsFingerprint;
  private byte[] cachedLogo;

  /**
   * @param taskExecutor needed for running background tasks.
   * @param manifestUpdateStatuses needed to display statuses of manifests.
   * @param sourceProvider needed to present the list of all sources.
   * @param updater needed to perform on-demand manifest updates.
   * @param notifier needed to retrieve issues watched by particular recipients.
   * @param uptimeChecker needed to display current uptime stats.
   * @param docBuilder needed to support online document validation service.
   * @param echoTester needed to support online Echo API validation service.
   * @param selfManifestProvider needed in Echo Validator responses.
   * @param resLoader needed to load CSS, logos etc.
   * @param matrixGenerator needed to render "API support table".
   * @param regClient needed to feed the {@link CoverageMatrixGenerator}.
   * @param catcache needed for caching the result of {@link CoverageMatrixGenerator}.
   */
  @Autowired
  public UiController(TaskExecutor taskExecutor,
      ManifestUpdateStatusRepository manifestUpdateStatuses, ManifestSourceProvider sourceProvider,
      RegistryUpdater updater, NotifierService notifier, UptimeChecker uptimeChecker,
      EwpDocBuilder docBuilder, EchoValidator echoTester, SelfManifestProvider selfManifestProvider,
      ResourceLoader resLoader, CoverageMatrixGenerator matrixGenerator, RegistryClient regClient,
      CatalogueDependantCache catcache) {
    this.taskExecutor = taskExecutor;
    this.manifestStatusRepo = manifestUpdateStatuses;
    this.sourceProvider = sourceProvider;
    this.updater = updater;
    this.notifier = notifier;
    this.uptimeChecker = uptimeChecker;
    this.docBuilder = docBuilder;
    this.echoTester = echoTester;
    this.selfManifestProvider = selfManifestProvider;
    this.resLoader = resLoader;
    this.matrixGenerator = matrixGenerator;
    this.regClient = regClient;
    this.catcache = catcache;
  }

  /**
   * @param response Needed to add some custom headers.
   * @return The HEI/API coverage matrix page.
   */
  @RequestMapping(value = "/coverage", method = RequestMethod.GET)
  public ModelAndView coverage(HttpServletResponse response) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("coverage");
    response.addHeader("Cache-Control", "public, max-age=300");

    try {
      mav.addObject("coverageMatrixHtml", this.getCoverageMatrixHtml());
    } catch (CatalogueNotFound e) {
      mav.addObject("coverageMatrixHtml", "<p>No catalogue found.</p>");
    }

    return mav;
  }

  /**
   * @param response Needed to add some custom headers.
   * @return Our CSS file.
   */
  @ResponseBody
  @RequestMapping(value = "/style-{version}.css", method = RequestMethod.GET, produces = "text/css")
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getCss(HttpServletResponse response) {
    response.addHeader("Cache-Control", "public, max-age=86400, stale-while-revalidate=604800");
    if (this.cachedCss == null) {
      this.cacheCss();
    }
    return this.cachedCss;
  }

  /**
   * @param response Needed to add some custom headers.
   * @return Our scripts file.
   */
  @ResponseBody
  @RequestMapping(value = "/scripts-{version}.js", method = RequestMethod.GET,
      produces = "application/javascript")
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getJs(HttpServletResponse response) {
    response.addHeader("Cache-Control", "public, max-age=86400, stale-while-revalidate=604800");
    if (this.cachedJs == null) {
      this.cacheJs();
    }
    return this.cachedJs;
  }

  /**
   * @param response Needed to add some custom headers.
   * @return EWP logo image. Depends on the result of {@link #isUsingDevDesign()}.
   */
  @ResponseBody
  @RequestMapping(value = "/logo.png", method = RequestMethod.GET, produces = "image/png")
  @SuppressFBWarnings("EI_EXPOSE_REP")
  public byte[] getLogo(HttpServletResponse response) {
    response.addHeader("Cache-Control", "public, max-age=86400, stale-while-revalidate=604800");
    if (this.cachedLogo == null) {
      try {
        String path;
        if (this.isUsingDevDesign()) {
          path = "classpath:logo-dev.png";
        } else {
          path = "classpath:logo-prod.png";
        }
        this.cachedLogo = IOUtils.toByteArray(this.resLoader.getResource(path).getInputStream());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return this.cachedLogo;
  }

  /**
   * @param response Needed to add some custom headers.
   * @return A welcome page.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public ModelAndView index(HttpServletResponse response) {

    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("index");
    response.addHeader("Cache-Control", "public, max-age=300");
    mav.addObject("catalogueUrl", Application.getRootUrl() + "/catalogue-v1.xml");
    mav.addObject("statusUrl", Application.getRootUrl() + "/status");
    mav.addObject("coverageUrl", Application.getRootUrl() + "/coverage");
    mav.addObject("uptime24", this.uptimeChecker.getLast24HoursUptimeRatio());
    mav.addObject("uptime7", this.uptimeChecker.getLast7DaysUptimeRatio());
    mav.addObject("uptime30", this.uptimeChecker.getLast30DaysUptimeRatio());
    mav.addObject("uptime365", this.uptimeChecker.getLast365DaysUptimeRatio());
    mav.addObject("artifactVersion", this.getClass().getPackage().getImplementationVersion());

    return mav;
  }

  /**
   * @param response Needed to add some custom headers.
   * @param url URL of the manifest source.
   * @return A page describing the status of the manifest.
   */
  @RequestMapping(value = "/status", params = "url", method = RequestMethod.GET)
  public ModelAndView manifestStatus(HttpServletResponse response, @RequestParam String url) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("statusPerUrl");
    response.addHeader("Cache-Control", "max-age=0, must-revalidate");

    mav.addObject("manifestUrl", url);
    Optional<ManifestUpdateStatus> status =
        Optional.ofNullable(this.manifestStatusRepo.findOne(url));
    if (status.isPresent() && (!status.get().getLastAccessAttempt().isPresent())) {
      status = Optional.empty();
    }
    Optional<ManifestSource> source = this.sourceProvider.getOne(url);
    mav.addObject("status", status);
    mav.addObject("source", source);
    return mav;
  }

  /**
   * Perform an on-demand reload of a single specific manifest.
   *
   * @param response Needed to add some custom headers.
   * @param url URL of the manifest source to be reloaded.
   * @return Empty response with HTTP 200 on success (queued). Empty HTTP 400 response on error
   *         (unknown URL).
   */
  @RequestMapping(value = "/reload", params = "url", method = RequestMethod.POST)
  public ResponseEntity<String> reloadManifest(HttpServletResponse response,
      @RequestParam String url) {

    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl("max-age=0, must-revalidate");
    headers.setExpires(0);

    Optional<ManifestSource> source = this.sourceProvider.getOne(url);

    if (source.isPresent()) {
      this.taskExecutor.execute(new Runnable() {
        @Override
        public void run() {
          UiController.this.updater.reloadManifestSource(source.get());
        }
      });
      return new ResponseEntity<String>("", headers, HttpStatus.OK);
    } else {
      return new ResponseEntity<String>("", headers, HttpStatus.BAD_REQUEST);
    }
  }


  /**
   * @param response Needed to add some custom headers.
   * @return A page with all manifest sources and their statuses.
   */
  @RequestMapping(value = "/status", method = RequestMethod.GET)
  public ModelAndView serviceStatus(HttpServletResponse response) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("status");
    response.addHeader("Cache-Control", "max-age=0, must-revalidate");

    List<ManifestUpdateStatus> statuses = new ArrayList<>();
    for (ManifestSource source : this.sourceProvider.getAll()) {
      statuses.add(this.manifestStatusRepo.findOne(source.getUrl()));
    }
    mav.addObject("manifestStatuses", statuses);
    return mav;
  }


  /**
   * Display a status page tailored for a given notification recipient.
   *
   * @param response Needed to add some custom headers.
   * @param email Email address of the recipient.
   * @return A page with the list of issue statuses related to this recipient.
   */
  @RequestMapping(value = "/status", params = "email", method = RequestMethod.GET)
  public ModelAndView statusForRecipient(HttpServletResponse response, @RequestParam String email) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("statusPerEmail");
    response.addHeader("Cache-Control", "max-age=0, must-revalidate");

    mav.addObject("email", email);
    mav.addObject("flags", this.notifier.getFlagsWatchedBy(email));

    return mav;
  }

  /**
   * Run validation tests on the Echo API served at the given URL.
   *
   * <p>
   * This is not part of the API and MAY be removed later on.
   * </p>
   *
   * @param url The URL at which the Echo API is being served.
   * @return An undocumented JSON object with the results of the validation (not guaranteed to stay
   *         backward compatible).
   */
  @RequestMapping(path = "/validate-echo", params = "url", method = RequestMethod.POST)
  public ResponseEntity<String> validateEcho(@RequestParam String url) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
    headers.setCacheControl("max-age=0, must-revalidate");
    headers.setExpires(0);

    JsonObject responseObj = new JsonObject();
    JsonObject info = new JsonObject();
    responseObj.add("info", info);
    DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    Date validationStarted = new Date();
    info.addProperty("validationStarted", isoDateFormat.format(validationStarted));
    Date clientKeysRegenerated = this.echoTester.getCredentialsGenerationDate();
    info.addProperty("clientKeysRegenerated", isoDateFormat.format(clientKeysRegenerated));
    info.addProperty("clientKeysAgeWhenValidationStartedInSeconds",
        (validationStarted.getTime() - clientKeysRegenerated.getTime()) / 1000);
    info.addProperty("registryManifestBody", this.selfManifestProvider.getManifest());
    JsonArray combinations = new JsonArray();
    info.add("secMethodCombinations", combinations);
    for (Entry<String, String> entry : SecMethodsCombination.getCombinationLegend().entrySet()) {
      JsonObject desc = new JsonObject();
      desc.addProperty("code", entry.getKey());
      desc.addProperty("name", entry.getValue());
      combinations.add(desc);
    }

    JsonArray testsArray = new JsonArray();
    Status worstStatus = Status.SUCCESS;
    List<ValidationStepWithStatus> testResults = this.echoTester.runTests(url);
    for (ValidationStepWithStatus testResult : testResults) {
      JsonObject testObj = new JsonObject();
      testObj.addProperty("name", testResult.getName());
      testObj.addProperty("status", testResult.getStatus().toString());
      if (worstStatus.compareTo(testResult.getStatus()) < 0) {
        worstStatus = testResult.getStatus();
      }
      testObj.addProperty("message", testResult.getMessage());
      testObj.add("clientRequest", this.formatClientRequestObject(testResult));
      testObj.add("serverResponse", this.formatServerResponseObject(testResult));
      testsArray.add(testObj);
    }
    responseObj.addProperty("success", worstStatus.equals(Status.SUCCESS));
    responseObj.addProperty("status", worstStatus.toString());
    responseObj.add("tests", testsArray);
    Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    String json = gson.toJson(responseObj);
    return new ResponseEntity<String>(json, headers, HttpStatus.OK);
  }

  /**
   * Validate a supplied XML document against all known EWP schemas.
   *
   * <p>
   * This is not part of the API and MAY be removed later on.
   * </p>
   *
   * @param xml The XML to be validated.
   * @return An undocumented JSON object with the results of the validation (not guaranteed to stay
   *         backward compatible).
   */
  @RequestMapping(path = "/validate", params = "xml", method = RequestMethod.POST)
  public ResponseEntity<String> validateXml(@RequestParam String xml) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
    headers.setCacheControl("max-age=0, must-revalidate");
    headers.setExpires(0);
    headers.setAccessControlAllowOrigin("http://developers.erasmuswithoutpaper.eu");

    BuildParams params = new BuildParams(xml);
    params.setMakingPretty(true);

    // root
    BuildResult result1 = this.docBuilder.build(params);
    JsonObject result2 = new JsonObject();

    // isValid
    result2.addProperty("isValid", result1.isValid());
    JsonArray errors2 = new JsonArray();

    // rootLocalName, rootNamespaceUri
    result2.addProperty("rootLocalName", result1.getRootLocalName());
    result2.addProperty("rootNamespaceUri", result1.getRootNamespaceUri());

    // errors
    result2.add("errors", errors2);
    for (BuildError error1 : result1.getErrors()) {
      JsonObject error2 = new JsonObject();
      error2.addProperty("lineNumber", error1.getLineNumber());
      error2.addProperty("message", error1.getMessage());
      errors2.add(error2);
    }

    // prettyLines
    List<String> prettyLines1 = result1.getPrettyLines().orElseGet(new Supplier<List<String>>() {
      @Override
      public List<String> get() {
        return Lists.newArrayList();
      }
    });
    JsonArray prettyLines2 = new JsonArray();
    result2.add("prettyLines", prettyLines2);
    for (String line : prettyLines1) {
      prettyLines2.add(new JsonPrimitive(line));
    }

    // Format the result.
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String json = gson.toJson(result2);
    return new ResponseEntity<String>(json, headers, HttpStatus.OK);
  }

  private void cacheCss() {
    try {
      this.cachedCss =
          IOUtils.toByteArray(this.resLoader.getResource("classpath:style.css").getInputStream());
      this.cachedCssFingerprint = DigestUtils.sha1Hex(this.cachedCss);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void cacheJs() {
    try {
      this.cachedJs =
          IOUtils.toByteArray(this.resLoader.getResource("classpath:scripts.js").getInputStream());
      this.cachedJsFingerprint = DigestUtils.sha1Hex(this.cachedJs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private JsonElement formatClientRequestObject(ValidationStepWithStatus testResult) {
    if (!testResult.getClientRequest().isPresent()) {
      return null;
    }
    Request request = testResult.getClientRequest().get();
    JsonObject result = new JsonObject();
    if (request.getBody().isPresent()) {
      byte[] body = request.getBody().get();
      result.addProperty("rawBodyBase64", Base64.encode(body));
      CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
      try {
        CharBuffer decoded = decoder.decode(ByteBuffer.wrap(body));
        result.addProperty("body", decoded.toString());
      } catch (CharacterCodingException e) {
        result.add("body", JsonNull.INSTANCE);
      }
    } else {
      result.add("rawBodyBase64", JsonNull.INSTANCE);
      result.add("body", JsonNull.INSTANCE);
    }
    result.addProperty("url", request.getUrl());
    result.addProperty("method", request.getMethod());
    JsonObject headers = new JsonObject();
    for (Entry<String, String> entry : request.getHeaders().entrySet()) {
      headers.addProperty(entry.getKey(), entry.getValue());
    }
    result.add("headers", headers);
    if (request.getClientCertificate().isPresent()) {
      try {
        result.addProperty("clientCertFingerprint",
            DigestUtils.sha256Hex(request.getClientCertificate().get().getEncoded()));
      } catch (CertificateEncodingException e) {
        throw new RuntimeException(e);
      }
    } else {
      result.add("clientCertFingerprint", JsonNull.INSTANCE);
    }
    return result;
  }

  private JsonObject formatServerResponseObject(ValidationStepWithStatus testResult) {
    if (!testResult.getServerResponse().isPresent()) {
      return null;
    }
    Response response = testResult.getServerResponse().get();
    JsonObject result = new JsonObject();
    result.addProperty("status", response.getStatus());
    result.addProperty("rawBodyBase64", Base64.encode(response.getBody()));
    BuildParams params = new BuildParams(response.getBody());
    params.setMakingPretty(true);
    BuildResult buildResult = this.docBuilder.build(params);
    result.addProperty("prettyXml", buildResult.getPrettyXml().orElse(null));
    if (buildResult.getDocument().isPresent()) {
      Match root = $(buildResult.getDocument().get()).namespaces(KnownNamespace.prefixMap());
      result.addProperty("developerMessage",
          root.xpath("/ewp:error-response/ewp:developer-message").text());
    } else {
      result.add("developerMessage", JsonNull.INSTANCE);
    }
    JsonObject headers = new JsonObject();
    for (Entry<String, String> entry : response.getHeaders().entrySet()) {
      headers.addProperty(entry.getKey(), entry.getValue());
    }
    result.add("headers", headers);
    return result;
  }

  private String getCoverageMatrixHtml() throws CatalogueNotFound {
    String result = this.catcache.getCoverageMatrixHtml();
    if (result == null) {
      result = this.matrixGenerator.generateToHtmlTable(this.regClient);
      this.catcache.putCoverageMatrixHtml(result);
    }
    return result;
  }

  private String getCssFingerprint() {
    if (this.cachedCssFingerprint == null) {
      this.cacheCss();
    }
    return this.cachedCssFingerprint;
  }

  private String getJsFingerprint() {
    if (this.cachedJsFingerprint == null) {
      this.cacheJs();
    }
    return this.cachedJsFingerprint;
  }

  private void initializeMavCommons(ModelAndView mav) {
    mav.addObject("isUsingDevDesign", this.isUsingDevDesign());
    mav.addObject("cssFingerprint", this.getCssFingerprint());
    mav.addObject("jsFingerprint", this.getJsFingerprint());
  }

  private boolean isUsingDevDesign() {
    return !Application.isProductionSite();
  }
}
