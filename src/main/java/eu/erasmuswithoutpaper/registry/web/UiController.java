package eu.erasmuswithoutpaper.registry.web;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.cmatrix.CoverageMatrixGenerator;
import eu.erasmuswithoutpaper.registry.cmatrix.CoverageMatrixGenerator.HeiComparator;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildError;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.iia.ElementHashException;
import eu.erasmuswithoutpaper.registry.iia.HashComparisonResult;
import eu.erasmuswithoutpaper.registry.iia.IiaHashService;
import eu.erasmuswithoutpaper.registry.manifestoverview.ApiForHeiImplementationMapping;
import eu.erasmuswithoutpaper.registry.manifestoverview.CoveredInstitutionsCounters;
import eu.erasmuswithoutpaper.registry.manifestoverview.ImplementedApisCount;
import eu.erasmuswithoutpaper.registry.manifestoverview.ManifestOverviewInfo;
import eu.erasmuswithoutpaper.registry.manifestoverview.ManifestOverviewManager;
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
import eu.erasmuswithoutpaper.registry.validators.ApiEndpoint;
import eu.erasmuswithoutpaper.registry.validators.ApiValidatorsManager;
import eu.erasmuswithoutpaper.registry.validators.HtmlValidationReportFormatter;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription.InvalidDescriptionString;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion.InvalidVersionString;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameter;
import eu.erasmuswithoutpaper.registry.validators.ValidationParameters;
import eu.erasmuswithoutpaper.registry.validators.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStore;
import eu.erasmuswithoutpaper.registry.validators.ValidatorKeyStoreSet;
import eu.erasmuswithoutpaper.registry.validators.web.ManifestApiEntry;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Handles UI requests.
 */
@Controller
@ConditionalOnWebApplication
@PropertySource("classpath:git.properties")
public class UiController {

  private final TaskExecutor taskExecutor;
  private final ManifestUpdateStatusRepository manifestStatusRepo;
  private final ManifestRepository manifestRepository;
  private final ManifestSourceProvider sourceProvider;
  private final RegistryUpdater updater;
  private final NotifierService notifier;
  private final UptimeChecker uptimeChecker;
  private final EwpDocBuilder docBuilder;
  private final ResourceLoader resLoader;
  private final CoverageMatrixGenerator matrixGenerator;
  private final RegistryClient regClient;
  private final CatalogueDependantCache catcache;
  private final ApiValidatorsManager apiValidatorsManager;
  private final ValidatorKeyStore validatorKeyStore;
  private final RegistryErrorController errorController;
  private final ManifestOverviewManager manifestOverviewManager;
  private final IiaHashService iiaHashService;
  private final String documentationUrl;

  @Value("${git.build.version}")
  private String gitBuildVersion;
  @Value("(${git.commit.id.abbrev}, ${git.build.time})")
  private String gitBuildInfo;

  private byte[] cachedCss;
  private String cachedCssFingerprint;
  private byte[] cachedJs;
  private String cachedJsFingerprint;
  private byte[] cachedUiJs;
  private String cachedUiJsFingerprint;
  private byte[] cachedLogo;

  /**
   * @param taskExecutor needed for running background tasks.
   * @param manifestUpdateStatuses needed to display statuses of manifests.
   * @param manifestRepository needed to display list of apis implemented by hosts in manifest.
   * @param sourceProvider needed to present the list of all sources.
   * @param updater needed to perform on-demand manifest updates.
   * @param notifier needed to retrieve issues watched by particular recipients.
   * @param uptimeChecker needed to display current uptime stats.
   * @param docBuilder needed to support online document validation service.
   * @param resLoader needed to load CSS, logos etc.
   * @param matrixGenerator needed to render "API support table".
   * @param regClient needed to feed the {@link CoverageMatrixGenerator}.
   * @param catcache needed to cache the result of {@link CoverageMatrixGenerator}.
   * @param apiValidatorsManager needed to check if there are tests for given api and version.
   * @param validatorKeyStoreSet set of KeyStores providing credentials.
   * @param errorController used to generate 404 pages when the validator is not available.
   * @param manifestOverviewManager used to retrieve current data about duplicates in the network.
   * @param iiaHashService used to validate IIA cooperation conditions hash
   */
  @Autowired
  public UiController(@Qualifier("customTaskExecutor") TaskExecutor taskExecutor,
      ManifestUpdateStatusRepository manifestUpdateStatuses, ManifestRepository manifestRepository,
      ManifestSourceProvider sourceProvider, RegistryUpdater updater, NotifierService notifier,
      UptimeChecker uptimeChecker, EwpDocBuilder docBuilder, ResourceLoader resLoader,
      CoverageMatrixGenerator matrixGenerator, RegistryClient regClient,
      CatalogueDependantCache catcache, ApiValidatorsManager apiValidatorsManager,
      ValidatorKeyStoreSet validatorKeyStoreSet, RegistryErrorController errorController,
      ManifestOverviewManager manifestOverviewManager, IiaHashService iiaHashService,
      @Value("${app.ewp-documentation-url}") String documentationUrl) {
    this.taskExecutor = taskExecutor;
    this.manifestStatusRepo = manifestUpdateStatuses;
    this.manifestRepository = manifestRepository;
    this.sourceProvider = sourceProvider;
    this.updater = updater;
    this.notifier = notifier;
    this.uptimeChecker = uptimeChecker;
    this.docBuilder = docBuilder;
    this.resLoader = resLoader;
    this.matrixGenerator = matrixGenerator;
    this.regClient = regClient;
    this.catcache = catcache;
    this.apiValidatorsManager = apiValidatorsManager;
    this.validatorKeyStore = validatorKeyStoreSet.getMainKeyStore();
    this.errorController = errorController;
    this.manifestOverviewManager = manifestOverviewManager;
    this.iiaHashService = iiaHashService;
    this.documentationUrl = documentationUrl;
  }

  /**
   * @param response Needed to add some custom headers.
   * @return The HEI/API coverage matrix page.
   */
  @RequestMapping(value = "/coverage", method = RequestMethod.GET)
  public ModelAndView coverage(HttpServletResponse response,
      @RequestParam(required = false) String pattern) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("coverage");
    response.addHeader("Cache-Control", "public, max-age=300");

    mav.addObject("pattern", pattern);
    mav.addObject("coverageMatrixHtml", this.getCoverageMatrixHtml(pattern));
    return mav;
  }

  /**
   * @param response Needed to add some custom headers.
   * @return Our CSS file.
   */
  @ResponseBody
  @RequestMapping(value = "/style-{version}.css", method = RequestMethod.GET, produces = "text/css")
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
  public byte[] getJs(HttpServletResponse response) {
    response.addHeader("Cache-Control", "public, max-age=86400, stale-while-revalidate=604800");
    if (this.cachedJs == null) {
      this.cacheJs();
    }
    return this.cachedJs;
  }

  /**
   * @param response Needed to add some custom headers.
   * @return Our scripts file.
   */
  @ResponseBody
  @RequestMapping(value = "/ui-{version}.js", method = RequestMethod.GET,
      produces = "application/javascript")
  public byte[] getUiJs(HttpServletResponse response) {
    response.addHeader("Cache-Control", "public, max-age=86400, stale-while-revalidate=604800");
    if (this.cachedUiJs == null) {
      this.cacheUiJs();
    }
    return this.cachedUiJs;
  }

  /**
   * @param response Needed to add some custom headers.
   * @return EWP logo image. Depends on the result of {@link #isServingDevelopmentVersion()}.
   */
  @ResponseBody
  @RequestMapping(value = "/logo.png", method = RequestMethod.GET, produces = "image/png")
  public byte[] getLogo(HttpServletResponse response) {
    response.addHeader("Cache-Control", "public, max-age=86400, stale-while-revalidate=604800");
    if (this.cachedLogo == null) {
      try {
        String path;
        if (this.isServingDevelopmentVersion()) {
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
   * @param adminEmails Admin emails to display on page.
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
    mav.addObject("schemaValidatorUrl", Application.getRootUrl() + "/schemaValidator");
    mav.addObject("manifestOverviewUrl", Application.getRootUrl() + "/manifestsOverview");
    mav.addObject("heiSearchUrl", Application.getRootUrl() + "/heiSearch?pattern=");
    mav.addObject("iiaHashValidatorUrl", Application.getRootUrl() + "/iiaHashValidator");
    mav.addObject("uptime24", this.uptimeChecker.getLast24HoursUptimeRatio());
    mav.addObject("uptime7", this.uptimeChecker.getLast7DaysUptimeRatio());
    mav.addObject("uptime30", this.uptimeChecker.getLast30DaysUptimeRatio());
    mav.addObject("uptime365", this.uptimeChecker.getLast365DaysUptimeRatio());
    mav.addObject("artifactVersion", gitBuildVersion);
    mav.addObject("gitVersion", gitBuildInfo);
    mav.addObject("adminEmails", adminEmails);

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
    Optional<ManifestUpdateStatus> status = this.manifestStatusRepo.findById(url);
    if (status.isPresent() && (!status.get().getLastAccessAttempt().isPresent())) {
      status = Optional.empty();
    }
    Optional<ManifestSource> source = this.sourceProvider.getOne(url);
    mav.addObject("status", status);
    mav.addObject("source", source);
    return mav;
  }

  /**
   * @param response Needed to add some custom headers.
   * @param url URL of the manifest source.
   * @return A page describing the status of the manifest.
   */
  @RequestMapping(value = "/manifestValidation", params = "url", method = RequestMethod.GET)
  public Object manifestValidate(HttpServletResponse response, @RequestParam String url) {
    if (!Application.isValidationEnabled()) {
      return this.errorController.get404();
    }

    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("manifestValidation");
    response.addHeader("Cache-Control", "max-age=0, must-revalidate");

    mav.addObject("manifestUrl", url);

    List<ManifestApiEntry> apis;
    try {
      apis = ManifestApiEntry.parseManifest(manifestRepository.getManifestFiltered(url),
          apiValidatorsManager);
      apis = apis.stream().filter(api -> api.available).collect(Collectors.toList());
    } catch (ManifestNotFound manifestNotFound) {
      return this.errorController.get404();
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

    String securityDisclaimer;
    if (isServingDevelopmentVersion()) {
      securityDisclaimer = "<b>Make sure</b> that the system you connect to the DEMO EWP Network "
          + "contains only scrambled data or artificial data. This network is used for testing, "
          + "so - according to Murphy's law - whatever can go wrong, will go wrong.";
    } else {
      securityDisclaimer = "Do not connect your production system do the EWP Network unless you "
          + "are 100% sure that your system is secure and only authorised users can get "
          + "access to the data.";
    }
    mav.addObject("securityDisclaimer", securityDisclaimer);

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
      this.taskExecutor.execute(() -> UiController.this.updater.reloadManifestSource(source.get()));
      return new ResponseEntity<>("", headers, HttpStatus.OK);
    } else {
      return new ResponseEntity<>("", headers, HttpStatus.BAD_REQUEST);
    }
  }


  /**
   * @param response Needed to add some custom headers.
   * @return A page with all manifest sources and their statuses.
   */
  @RequestMapping(value = "/status", method = RequestMethod.GET)
  public ModelAndView serviceStatus(HttpServletResponse response,
      @Value("${app.admin-emails}") List<String> adminEmails) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("status");
    response.addHeader("Cache-Control", "max-age=0, must-revalidate");

    List<ManifestUpdateStatus> statuses = new ArrayList<>();
    for (ManifestSource source : this.sourceProvider.getAll()) {
      ManifestUpdateStatus status = this.manifestStatusRepo.findById(source.getUrl()).orElse(null);
      // Manifests that weren't loaded yet won't be displayed.
      if (status != null) {
        statuses.add(status);
      }
    }
    mav.addObject("manifestStatuses", statuses);
    mav.addObject("manifestValidationUrl", Application.getRootUrl() + "/manifestValidation");
    mav.addObject("isValidatorEnabled", Application.isValidationEnabled());
    mav.addObject("adminEmails", adminEmails);

    return mav;
  }


  /**
   * XML Schema Validator view.
   *
   * @param response Needed to add some custom headers.
   * @return A page with a form to validate given schema.
   */
  @RequestMapping(value = "/schemaValidator", method = RequestMethod.GET)
  public ModelAndView schemaValidator(HttpServletResponse response) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("schemaValidator");
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
    headers.setAccessControlAllowOrigin(documentationUrl);

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

  private void cacheUiJs() {
    try {
      this.cachedUiJs = IOUtils
          .toByteArray(this.resLoader.getResource("classpath:vue-app/ui.js").getInputStream());
      this.cachedUiJsFingerprint = DigestUtils.sha1Hex(this.cachedUiJs);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getCoverageMatrixHtml(String heiFilter) {
    if (heiFilter != null) {
      return this.matrixGenerator.generateToHtmlTable(this.regClient, heiFilter);
    }

    String result = this.catcache.getCoverageMatrixHtml();
    if (result == null) {
      result = this.matrixGenerator.generateToHtmlTable(this.regClient, null);
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

  private String getUiJsFingerprint() {
    if (this.cachedUiJsFingerprint == null) {
      this.cacheUiJs();
    }
    return this.cachedUiJsFingerprint;
  }

  private void initializeMavCommons(ModelAndView mav) {
    mav.addObject("isUsingDevDesign", this.isServingDevelopmentVersion());
    mav.addObject("cssFingerprint", this.getCssFingerprint());
    mav.addObject("jsFingerprint", this.getJsFingerprint());
    mav.addObject("uiJsFingerprint", this.getUiJsFingerprint());
  }

  private boolean isServingDevelopmentVersion() {
    return !Application.isProductionSite();
  }

  /**
   * Run validation on one of APIs served at the given URL.
   *
   * <p>
   * This is not part of the API and MAY be removed later on.
   * </p>
   *
   * @param requestBody request body.
   * @return HTML with validation results.
   */
  @RequestMapping(path = "/validateApi", method = RequestMethod.POST)
  public Object validateApiVersion(@RequestBody ValidationRequestBody requestBody) {
    if (!Application.isValidationEnabled()) {
      return this.errorController.get404();
    }

    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("validationResult");

    HttpSecurityDescription httpSecurityDescription;
    SemanticVersion ver;
    ApiEndpoint endpoint;
    try {
      httpSecurityDescription = new HttpSecurityDescription(requestBody.getSecurity());
      ver = new SemanticVersion(requestBody.getVersion());
      endpoint = ApiEndpoint.fromEndpointName(requestBody.getEndpoint());
    } catch (InvalidDescriptionString | IllegalArgumentException | InvalidVersionString ignored) {
      return this.errorController.get404();
    }

    if (!this.apiValidatorsManager.hasCompatibleTests(requestBody.getName(), endpoint, ver)) {
      return this.errorController.get404();
    }

    ValidationParameters requestParameters = new ValidationParameters(requestBody.getParameters());
    List<ValidationParameter> availableParameters =
        this.apiValidatorsManager.getParameters(requestBody.getName(), endpoint, ver);

    if (!requestParameters.checkDependencies(availableParameters)) {
      return this.errorController.get404();
    }

    Date validationStartedDate = new Date();

    List<ValidationStepWithStatus> testResults =
        this.apiValidatorsManager.getApiValidator(requestBody.getName(), endpoint)
            .runTests(requestBody.getUrl(), ver, httpSecurityDescription, requestParameters);

    HtmlValidationReportFormatter htmlValidationReportFormatter =
        new HtmlValidationReportFormatter(docBuilder);

    HtmlValidationReportFormatter.ValidationInfoParameters validationInfoParameters =
        new HtmlValidationReportFormatter.ValidationInfoParameters(requestBody.getName(),
            requestBody.getUrl(), requestBody.getVersion(), httpSecurityDescription,
            validationStartedDate, this.validatorKeyStore.getCredentialsGenerationDate());

    Map<String, Object> pebbleContext =
        htmlValidationReportFormatter.getPebbleContext(testResults, validationInfoParameters);

    mav.addAllObjects(pebbleContext);

    return mav;
  }

  /**
   * Presents some information from manifests.
   *
   * @param response Needed to add some custom headers.
   * @return Manifests Overview page.
   */
  @RequestMapping(path = "/manifestsOverview", method = RequestMethod.GET)
  public Object manifestOverview(HttpServletResponse response) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("manifestsOverview");
    response.addHeader("Cache-Control", "public, max-age=300");

    ManifestOverviewManager.ManifestOverviewState manifestOverviewState =
        this.manifestOverviewManager.getManifestOverviewState();

    Collection<ManifestOverviewInfo> infos = manifestOverviewState.overviewInfos;
    mav.addObject("manifestInfos", infos);

    ImplementedApisCount implementedApisCount = manifestOverviewState.implementedApisCount;
    mav.addObject("implementedApisCount", implementedApisCount);

    ApiForHeiImplementationMapping duplicates =
        manifestOverviewState.apiForHeiImplementationMappingDuplicates;
    mav.addObject("duplicatesInfo", duplicates);

    CoveredInstitutionsCounters heiDuplicates = manifestOverviewState.getHeiDuplicates;
    mav.addObject("heiDuplicates", heiDuplicates);

    return mav;
  }

  /**
   * HEI Search Page view.
   *
   * @return A page for searching HEIs.
   */
  @RequestMapping(value = "/heiSearch", method = RequestMethod.GET)
  public ModelAndView heiSearch(@RequestParam String pattern) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("heiSearch");

    mav.addObject("pattern", pattern);
    mav.addObject("heis", getHeisMatching(pattern));

    return mav;
  }

  private List<HeiEntry> getHeisMatching(String pattern) {
    if (pattern.isEmpty()) {
      return Collections.emptyList();
    }
    Collection<HeiEntry> allHeis = regClient.getAllHeis();

    return allHeis.stream().filter(Utils.getHeiFilterPredicate(pattern)).sorted(new HeiComparator())
        .collect(Collectors.toList());
  }

  /**
   * IIA Hash Validator form view.
   *
   * @return A page with a form to validate hash in a given IIA.
   */
  @RequestMapping(value = "/iiaHashValidator", method = RequestMethod.GET)
  public ModelAndView iiaHashValidatorForm() {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("iiaHashValidator");
    mav.addObject("xml", "");
    mav.addObject("hashComparisonResults", null);
    mav.addObject("errorMessage", null);

    return mav;
  }

  /**
   * IIA Hash Validator result view.
   *
   * @return A page with the result of IIA hash validation.
   */
  @RequestMapping(value = "/iiaHashValidator", method = RequestMethod.POST)
  public ModelAndView iiaHashValidatorResult(@RequestParam String xml) {
    ModelAndView mav = new ModelAndView();
    this.initializeMavCommons(mav);
    mav.setViewName("iiaHashValidator");
    mav.addObject("xml", xml);

    try (Reader reader = new StringReader(xml)) {
      List<HashComparisonResult> hashComparisonResults =
          iiaHashService.checkCooperationConditionsHash(new InputSource(reader));
      mav.addObject("hashComparisonResults", hashComparisonResults);
      mav.addObject("errorMessage", null);
      mav.addObject("allResultsCorrect", !hashComparisonResults.isEmpty()
          && hashComparisonResults.stream().allMatch(HashComparisonResult::isCorrect));
    } catch (ElementHashException | ParserConfigurationException | IOException | SAXException
        | XPathExpressionException exception) {
      mav.addObject("hashComparisonResults", null);
      mav.addObject("errorMessage", exception.getMessage());
    }

    return mav;
  }
}
