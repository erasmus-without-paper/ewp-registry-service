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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.cmatrix.CoverageMatrixGenerator;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildError;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Response;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import eu.erasmuswithoutpaper.registry.repository.CatalogueDependantCache;
import eu.erasmuswithoutpaper.registry.repository.ManifestNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.ManifestUpdateStatus;
import eu.erasmuswithoutpaper.registry.updater.ManifestUpdateStatusRepository;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdater;
import eu.erasmuswithoutpaper.registry.updater.UptimeChecker;
import eu.erasmuswithoutpaper.registry.validators.ApiValidator;
import eu.erasmuswithoutpaper.registry.validators.ApiValidatorsManager;
import eu.erasmuswithoutpaper.registry.validators.Combination;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription.InvalidDescriptionString;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion.InvalidVersionString;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.web.ManifestApiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
  private final ManifestRepository manifestRepository;
  private final ManifestSourceProvider sourceProvider;
  private final RegistryUpdater updater;
  private final NotifierService notifier;
  private final UptimeChecker uptimeChecker;
  private final EwpDocBuilder docBuilder;
  private final SelfManifestProvider selfManifestProvider;
  private final ResourceLoader resLoader;
  private final CoverageMatrixGenerator matrixGenerator;
  private final RegistryClient regClient;
  private final CatalogueDependantCache catcache;
  private final ApiValidatorsManager apiValidatorsManager;
  private final ValidatorKeyStore validatorKeyStore;

  private byte[] cachedCss;
  private String cachedCssFingerprint;
  private byte[] cachedJs;
  private String cachedJsFingerprint;
  private byte[] cachedLogo;

  /**
   * @param taskExecutor
   *     needed for running background tasks.
   * @param manifestUpdateStatuses
   *     needed to display statuses of manifests.
   * @param manifestRepository
   *     needed to display list of apis implemented by hosts in manifest.
   * @param sourceProvider
   *     needed to present the list of all sources.
   * @param updater
   *     needed to perform on-demand manifest updates.
   * @param notifier
   *     needed to retrieve issues watched by particular recipients.
   * @param uptimeChecker
   *     needed to display current uptime stats.
   * @param docBuilder
   *     needed to support online document validation service.
   * @param selfManifestProvider
   *     needed in Echo Validator responses.
   * @param resLoader
   *     needed to load CSS, logos etc.
   * @param matrixGenerator
   *     needed to render "API support table".
   * @param regClient
   *     needed to feed the {@link CoverageMatrixGenerator}.
   * @param catcache
   *     needed for caching the result of {@link CoverageMatrixGenerator}.
   * @param apiValidatorsManager
   *     needed to check if there are some tests for given api and version.
   * @param validatorKeyStore
   *     KeyStore providing credentials.
   */
  @Autowired
  public UiController(TaskExecutor taskExecutor,
      ManifestUpdateStatusRepository manifestUpdateStatuses, ManifestRepository manifestRepository,
      ManifestSourceProvider sourceProvider, RegistryUpdater updater, NotifierService notifier,
      UptimeChecker uptimeChecker, EwpDocBuilder docBuilder,
      SelfManifestProvider selfManifestProvider, ResourceLoader resLoader,
      CoverageMatrixGenerator matrixGenerator, RegistryClient regClient,
      CatalogueDependantCache catcache,
      ApiValidatorsManager apiValidatorsManager,
      ValidatorKeyStore validatorKeyStore) {
    this.taskExecutor = taskExecutor;
    this.manifestStatusRepo = manifestUpdateStatuses;
    this.manifestRepository = manifestRepository;
    this.sourceProvider = sourceProvider;
    this.updater = updater;
    this.notifier = notifier;
    this.uptimeChecker = uptimeChecker;
    this.docBuilder = docBuilder;
    this.selfManifestProvider = selfManifestProvider;
    this.resLoader = resLoader;
    this.matrixGenerator = matrixGenerator;
    this.regClient = regClient;
    this.catcache = catcache;
    this.apiValidatorsManager = apiValidatorsManager;
    this.validatorKeyStore = validatorKeyStore;
  }

  /**
   * @param response
   *     Needed to add some custom headers.
   * @return The HEI/API coverage matrix page.
   */
  @RequestMapping(value = "/coverage", method = RequestMethod.GET)
  public ModelAndView coverage(HttpServletResponse response) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("coverage");
    response.addHeader("Cache-Control", "public, max-age=300");

    mav.addObject("coverageMatrixHtml", this.getCoverageMatrixHtml());
    return mav;
  }

  /**
   * @param response
   *     Needed to add some custom headers.
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
   * @param response
   *     Needed to add some custom headers.
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
   * @param response
   *     Needed to add some custom headers.
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
   * @param response
   *     Needed to add some custom headers.
   * @param adminEmails
   *     Admin emails to display on page.
   * @return A welcome page.
   */
  @RequestMapping(value = "/", method = RequestMethod.GET)
  public ModelAndView index(HttpServletResponse response,
      @Value("${app.admin-emails}") List<String> adminEmails) {

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
    mav.addObject("adminEmails", adminEmails);

    return mav;
  }

  /**
   * @param response
   *     Needed to add some custom headers.
   * @param url
   *     URL of the manifest source.
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
   * @param response
   *     Needed to add some custom headers.
   * @param url
   *     URL of the manifest source.
   * @return A page describing the status of the manifest.
   */
  @RequestMapping(value = "/manifestValidation", params = "url", method = RequestMethod.GET)
  public ModelAndView manifestValidate(HttpServletResponse response, @RequestParam String url) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("manifestValidation");
    response.addHeader("Cache-Control", "max-age=0, must-revalidate");

    mav.addObject("manifestUrl", url);

    List<ManifestApiEntry> apis;
    try {
      apis = ManifestApiEntry
          .parseManifest(manifestRepository.getManifestFiltered(url), apiValidatorsManager);
      apis = apis.stream().filter(api -> api.available).collect(Collectors.toList());
    } catch (ManifestNotFound manifestNotFound) {
      mav.setStatus(HttpStatus.BAD_REQUEST);
      return mav;
    }
    mav.addObject("apis", apis);

    List<Map<String, String>> securities = new ArrayList<>();
    for (Map.Entry<String, String> entry : HttpSecurityDescription.getLegend().entrySet()) {
      Map<String, String> mapParts = new HashMap<>();
      mapParts.put("marker", entry.getKey());
      mapParts.put("description", entry.getValue());
      securities.add(mapParts);
    }
    mav.addObject("securities", securities);

    return mav;
  }

  /**
   * Perform an on-demand reload of a single specific manifest.
   *
   * @param response
   *     Needed to add some custom headers.
   * @param url
   *     URL of the manifest source to be reloaded.
   * @return Empty response with HTTP 200 on success (queued). Empty HTTP 400 response on error
   *     (unknown URL).
   */
  @RequestMapping(value = "/reload", params = "url", method = RequestMethod.POST)
  public ResponseEntity<String> reloadManifest(HttpServletResponse response,
      @RequestParam String url) {

    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl("max-age=0, must-revalidate");
    headers.setExpires(0);

    Optional<ManifestSource> source = this.sourceProvider.getOne(url);

    if (source.isPresent()) {
      this.taskExecutor.execute(() -> UiController.this.updater.reloadManifestSource(source.get()));
      return new ResponseEntity<>("", headers, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("", headers, HttpStatus.BAD_REQUEST);
    }
  }


  /**
   * @param response
   *     Needed to add some custom headers.
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
    mav.addObject("manifestValidationUrl", Application.getRootUrl() + "/manifestValidation");
    return mav;
  }


  /**
   * Display a status page tailored for a given notification recipient.
   *
   * @param response
   *     Needed to add some custom headers.
   * @param email
   *     Email address of the recipient.
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
   * Run validation tests on the Institutions API served at the given URL.
   *
   * <p>
   * This is not part of the API and MAY be removed later on.
   * </p>
   *
   * @param url
   *     The URL at which the Institutions API is being served.
   * @return An undocumented JSON object with the results of the validation (not guaranteed to stay
   *     backward compatible).
   */
  @RequestMapping(path = "/validate-institutions", params = "url", method = RequestMethod.POST)
  public ResponseEntity<String> validateInstitutions(@RequestParam String url) {
    return validateApi(url, this.apiValidatorsManager.getApiValidator("institutions"));
  }

  /**
   * Run validation tests on the Echo API served at the given URL.
   *
   * <p>
   * This is not part of the API and MAY be removed later on.
   * </p>
   *
   * @param url
   *     The URL at which the Echo API is being served.
   * @return An undocumented JSON object with the results of the validation (not guaranteed to stay
   *     backward compatible).
   */
  @RequestMapping(path = "/validate-echo", params = "url", method = RequestMethod.POST)
  public ResponseEntity<String> validateEcho(@RequestParam String url) {
    return validateApi(url, this.apiValidatorsManager.getApiValidator("echo"));
  }

  /**
   * Run validation on one of APIs served at the given URL. TODO: remove when validators are removed
   * from developers website.
   *
   * <p>
   * This is not part of the API and MAY be removed later on.
   * </p>
   *
   * @param url
   *     The URL at which the API is being served.
   * @return An undocumented JSON object with the results of the validation (not guaranteed to stay
   *     backward compatible).
   */
  private ResponseEntity<String> validateApi(String url, ApiValidator<?> tester) {
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
    Date clientKeysRegenerated = this.validatorKeyStore.getCredentialsGenerationDate();
    info.addProperty("clientKeysRegenerated", isoDateFormat.format(clientKeysRegenerated));
    info.addProperty(
        "clientKeysAgeWhenValidationStartedInSeconds",
        (validationStarted.getTime() - clientKeysRegenerated.getTime()) / 1000
    );
    info.addProperty("registryManifestBody", this.selfManifestProvider.getManifest());
    JsonArray combinations = new JsonArray();
    info.add("Combinations", combinations); // WRCLEANIT: backward compatibility
    info.add("combinations", combinations);
    for (Entry<String, String> entry : Combination.getCombinationLegend().entrySet()) {
      JsonObject desc = new JsonObject();
      desc.addProperty("code", entry.getKey());
      desc.addProperty("name", entry.getValue());
      combinations.add(desc);
    }

    JsonArray testsArray = new JsonArray();
    Status worstStatus = Status.SUCCESS;
    List<ValidationStepWithStatus> testResults =
        tester.runTests(url, new SemanticVersion(2, 0, 0), null);
    for (ValidationStepWithStatus testResult : testResults) {
      JsonObject testObj = new JsonObject();
      testObj.addProperty("name", testResult.getName());
      testObj.addProperty("status", testResult.getStatus().toString());
      if (worstStatus.compareTo(testResult.getStatus()) < 0) {
        worstStatus = testResult.getStatus();
      }
      testObj.addProperty("message", testResult.getMessage());

      JsonArray requestSnapshots = this.formatRequestSnapshots(testResult);
      testObj.add("requestSnapshots", requestSnapshots);
      JsonArray responseSnapshots = this.formatResponseSnapshots(testResult);
      testObj.add("responseSnapshots", responseSnapshots);

      testsArray.add(testObj);
    }
    responseObj.addProperty("success", worstStatus.equals(Status.SUCCESS));
    responseObj.addProperty("status", worstStatus.toString());
    responseObj.add("tests", testsArray);
    Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();
    String json = gson.toJson(responseObj);
    return new ResponseEntity<>(json, headers, HttpStatus.OK);
  }

  /**
   * Validate a supplied XML document against all known EWP schemas.
   *
   * <p>
   * This is not part of the API and MAY be removed later on.
   * </p>
   *
   * @param xml
   *     The XML to be validated.
   * @return An undocumented JSON object with the results of the validation (not guaranteed to stay
   *     backward compatible).
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
    List<String> prettyLines1 = result1.getPrettyLines().orElseGet(Lists::newArrayList);
    JsonArray prettyLines2 = new JsonArray();
    result2.add("prettyLines", prettyLines2);
    for (String line : prettyLines1) {
      prettyLines2.add(new JsonPrimitive(line));
    }

    // Format the result.
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String json = gson.toJson(result2);
    return new ResponseEntity<>(json, headers, HttpStatus.OK);
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

  private JsonElement formatRequestSnapshot(Request request) {
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
        result.addProperty(
            "clientCertFingerprint",
            DigestUtils.sha256Hex(request.getClientCertificate().get().getEncoded())
        );
      } catch (CertificateEncodingException e) {
        throw new RuntimeException(e);
      }
    } else {
      result.add("clientCertFingerprint", JsonNull.INSTANCE);
    }
    JsonArray noticesHtml = new JsonArray();
    for (String noticeHtml : request.getProcessingNoticesHtml()) {
      noticesHtml.add(noticeHtml);
    }
    result.add("processingNoticesHtml", noticesHtml);
    return result;
  }

  private JsonArray formatRequestSnapshots(ValidationStepWithStatus testResult) {
    JsonArray results = new JsonArray();
    for (Request snapshot : testResult.getRequestSnapshots()) {
      results.add(this.formatRequestSnapshot(snapshot));
    }
    return results;
  }

  private JsonObject formatResponseSnapshot(Response response) {
    JsonObject result = new JsonObject();
    result.addProperty("status", response.getStatus());
    result.addProperty("rawBodyBase64", Base64.encode(response.getBody()));
    BuildParams params = new BuildParams(response.getBody());
    params.setMakingPretty(true);
    BuildResult buildResult = this.docBuilder.build(params);
    if (buildResult.getDocument().isPresent()) {
      Match root = $(buildResult.getDocument().get()).namespaces(KnownNamespace.prefixMap());
      result.addProperty(
          "developerMessage",
          root.xpath("/ewp:error-response/ewp:developer-message").text()
      );
      result.addProperty("prettyXml", buildResult.getPrettyXml().orElse(null));
    } else {
      result.add("developerMessage", JsonNull.INSTANCE);
      result.addProperty("prettyXml", (String) null);
    }
    JsonObject headers = new JsonObject();
    for (Entry<String, String> entry : response.getHeaders().entrySet()) {
      headers.addProperty(entry.getKey(), entry.getValue());
    }
    result.add("headers", headers);
    JsonArray noticesHtml = new JsonArray();
    for (String noticeHtml : response.getProcessingNoticesHtml()) {
      noticesHtml.add(noticeHtml);
    }
    result.add("processingNoticesHtml", noticesHtml);
    return result;
  }

  private JsonArray formatResponseSnapshots(ValidationStepWithStatus testResult) {
    JsonArray results = new JsonArray();
    for (Response snapshot : testResult.getResponseSnapshots()) {
      results.add(this.formatResponseSnapshot(snapshot));
    }
    return results;
  }

  private String getCoverageMatrixHtml() {
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

  /**
   * Run validation on one of APIs served at the given URL.
   *
   * <p>
   * This is not part of the API and MAY be removed later on.
   * </p>
   *
   * @param url
   *     The URL at which the API is being served.
   * @param name Name of the API being tested.
   * @param version Version of the API being tested.
   * @param security Security ddescription to be tested.
   * @return HTML with validation results.
   */
  @RequestMapping(path = "/validateApi", method = RequestMethod.POST)
  public ModelAndView validateApiVersion(@RequestParam String url, @RequestParam String name,
      @RequestParam String version, @RequestParam String security) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("validationResult");

    Map<String, Object> info = new HashMap<>();
    DateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    Date validationStarted = new Date();
    info.put("validationStarted", isoDateFormat.format(validationStarted));
    Date clientKeysRegenerated = this.validatorKeyStore.getCredentialsGenerationDate();
    info.put("clientKeysRegenerated", isoDateFormat.format(clientKeysRegenerated));
    info.put(
        "clientKeysAgeWhenValidationStartedInSeconds",
        (validationStarted.getTime() - clientKeysRegenerated.getTime()) / 1000
    );
    info.put("registryManifestBody", this.selfManifestProvider.getManifest());
    info.put("security", security);
    info.put("url", url);
    info.put("apiName", name);
    info.put("version", version);

    HttpSecurityDescription desc;
    SemanticVersion ver;
    try {
      desc = new HttpSecurityDescription(security);
      ver = new SemanticVersion(version);
    } catch (InvalidDescriptionString | InvalidVersionString ignored) {
      mav.setStatus(HttpStatus.BAD_REQUEST);
      return mav;
    }

    String[] explanations = desc.getExplanation().split("\n");
    List<Map<String, String>> splitExplanations = new ArrayList<>();
    for (String explanation : explanations) {
      String[] parts = explanation.split(":", 2);
      Map<String, String> mapParts = new HashMap<>();
      mapParts.put("marker", parts[0]);
      mapParts.put("description", parts[1]);
      splitExplanations.add(mapParts);
    }


    info.put("securityExplanation", splitExplanations);
    mav.addObject("info", info);

    Status worstStatus = Status.SUCCESS;
    if (!this.apiValidatorsManager.hasCompatibleTests(name, ver)) {
      mav.setStatus(HttpStatus.BAD_REQUEST);
      return mav;
    }


    List<Map<String, Object>> testsArray = new ArrayList<>();
    List<ValidationStepWithStatus> testResults =
        this.apiValidatorsManager.getApiValidator(name).runTests(url, ver, desc);
    for (ValidationStepWithStatus testResult : testResults) {
      Map<String, Object> testObj = new HashMap<>();
      testObj.put("name", testResult.getName());
      testObj.put("status", testResult.getStatus().toString());
      if (worstStatus.compareTo(testResult.getStatus()) < 0) {
        worstStatus = testResult.getStatus();
      }

      String message = testResult.getMessage();
      testObj.put("message", message);

      List<Object> requestSnapshots = this.formatRequestSnapshotsToMap(testResult);
      testObj.put("requestSnapshots", requestSnapshots);
      List<Object> responseSnapshots = this.formatResponseSnapshotsToMap(testResult);
      testObj.put("responseSnapshots", responseSnapshots);

      boolean hasMessage = !message.isEmpty() && !message.equals("OK");
      testObj.put("hasMessage", hasMessage);

      boolean hasDetails = !requestSnapshots.isEmpty() || !responseSnapshots.isEmpty();
      testObj.put("hasDetails", hasDetails);

      int height = 1;
      if (hasMessage) {
        height += 1;
      }
      if (hasDetails) {
        height += 1;
      }

      testObj.put("height", height);

      testsArray.add(testObj);
    }

    mav.addObject("success", worstStatus.equals(Status.SUCCESS));
    mav.addObject("status", worstStatus.toString());
    mav.addObject("tests", testsArray);

    return mav;
  }


  private List<Object> formatResponseSnapshotsToMap(ValidationStepWithStatus testResult) {
    List<Object> results = new ArrayList<>();
    for (Response snapshot : testResult.getResponseSnapshots()) {
      results.add(this.formatResponseSnapshotToMap(snapshot));
    }
    return results;
  }

  private Map<String, Object> formatResponseSnapshotToMap(Response response) {
    Map<String, Object> result = new HashMap<>();
    result.put("status", response.getStatus());
    result.put("rawBodyBase64", Base64.encode(response.getBody()));
    BuildParams params = new BuildParams(response.getBody());
    params.setMakingPretty(true);
    BuildResult buildResult = this.docBuilder.build(params);
    if (buildResult.getDocument().isPresent()) {
      Match root = $(buildResult.getDocument().get()).namespaces(KnownNamespace.prefixMap());
      result.put(
          "developerMessage",
          root.xpath("/ewp:error-response/ewp:developer-message").text()
      );
      result.put("prettyXml", buildResult.getPrettyXml().orElse(null));
    } else {
      result.put("developerMessage", null);
      result.put("prettyXml", null);
    }
    result.put("headers", response.getHeaders());
    result.put("processingNoticesHtml", response.getProcessingNoticesHtml());
    return result;
  }

  private List<Object> formatRequestSnapshotsToMap(ValidationStepWithStatus testResult) {
    List<Object> results = new ArrayList<>();
    for (Request snapshot : testResult.getRequestSnapshots()) {
      results.add(this.formatRequestSnapshotToMap(snapshot));
    }
    return results;
  }

  private Map<String, Object> formatRequestSnapshotToMap(Request request) {
    Map<String, Object> result = new HashMap<>();
    if (request.getBody().isPresent()) {
      byte[] body = request.getBody().get();
      result.put("rawBodyBase64", Base64.encode(body));
      CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
      try {
        CharBuffer decoded = decoder.decode(ByteBuffer.wrap(body));
        result.put("body", decoded.toString());
      } catch (CharacterCodingException e) {
        result.put("body", null);
      }
    } else {
      result.put("rawBodyBase64", null);
      result.put("body", null);
    }
    result.put("url", request.getUrl());
    result.put("method", request.getMethod());
    result.put("headers", request.getHeaders());
    if (request.getClientCertificate().isPresent()) {
      try {
        result.put(
            "clientCertFingerprint",
            DigestUtils.sha256Hex(request.getClientCertificate().get().getEncoded())
        );
      } catch (CertificateEncodingException e) {
        throw new RuntimeException(e);
      }
    } else {
      result.put("clientCertFingerprint", null);
    }
    result.put("processingNoticesHtml", request.getProcessingNoticesHtml());
    return result;
  }

}
