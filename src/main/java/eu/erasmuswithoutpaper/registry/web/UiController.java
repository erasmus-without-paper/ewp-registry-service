package eu.erasmuswithoutpaper.registry.web;

import static org.joox.JOOX.$;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Supplier;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildError;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildResult;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.echovalidator.EchoValidator;
import eu.erasmuswithoutpaper.registry.echovalidator.ValidationStepWithStatus;
import eu.erasmuswithoutpaper.registry.echovalidator.ValidationStepWithStatus.Status;
import eu.erasmuswithoutpaper.registry.internet.Internet.Request;
import eu.erasmuswithoutpaper.registry.internet.Internet.Response;
import eu.erasmuswithoutpaper.registry.notifier.NotifierFlag;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.ManifestUpdateStatus;
import eu.erasmuswithoutpaper.registry.updater.ManifestUpdateStatusRepository;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdater;
import eu.erasmuswithoutpaper.registry.updater.UpdateNotice;
import eu.erasmuswithoutpaper.registry.updater.UptimeChecker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

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
import org.apache.xerces.impl.dv.util.Base64;
import org.joox.Match;
import org.ocpsoft.prettytime.PrettyTime;

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
   */
  @Autowired
  public UiController(TaskExecutor taskExecutor,
      ManifestUpdateStatusRepository manifestUpdateStatuses, ManifestSourceProvider sourceProvider,
      RegistryUpdater updater, NotifierService notifier, UptimeChecker uptimeChecker,
      EwpDocBuilder docBuilder, EchoValidator echoTester,
      SelfManifestProvider selfManifestProvider) {
    this.taskExecutor = taskExecutor;
    this.manifestStatusRepo = manifestUpdateStatuses;
    this.sourceProvider = sourceProvider;
    this.updater = updater;
    this.notifier = notifier;
    this.uptimeChecker = uptimeChecker;
    this.docBuilder = docBuilder;
    this.echoTester = echoTester;
    this.selfManifestProvider = selfManifestProvider;
  }

  /**
   * @return A welcome page.
   */
  @RequestMapping("/")
  public ResponseEntity<String> index() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.setCacheControl("max-age=0, must-revalidate");
    headers.setExpires(0);

    StringBuilder sb = new StringBuilder();
    sb.append("EWP Registry Service\n");
    sb.append("====================\n");
    sb.append('\n');
    sb.append("Limited time deal - test our gorgeous \"text/plain skin\" for free!\n");
    sb.append('\n');
    String artifactVersion = this.getClass().getPackage().getImplementationVersion();
    sb.append("Version " + artifactVersion + ".\n");
    sb.append("Uptime ratios:\n");
    sb.append("- Last 24 hours: ");
    sb.append(this.uptimeChecker.getLast24HoursUptimeRatio());
    sb.append('\n');
    sb.append("- Last 7 days: ");
    sb.append(this.uptimeChecker.getLast7DaysUptimeRatio());
    sb.append('\n');
    sb.append("- Last 30 days: ");
    sb.append(this.uptimeChecker.getLast30DaysUptimeRatio());
    sb.append('\n');
    sb.append("- Last 365 days: ");
    sb.append(this.uptimeChecker.getLast365DaysUptimeRatio());
    sb.append('\n');
    sb.append("\n\n");
    sb.append("PRODUCTION and DEVELOPMENT Registry Service installations\n");
    sb.append("---------------------------------------------------------\n");
    sb.append('\n');
    sb.append("The EWP Network is still being designed. During this time, we will use two\n");
    sb.append("separate Registry Service installations:\n");
    sb.append('\n');
    sb.append("https://registry.erasmuswithoutpaper.eu/\n");
    sb.append("    - the official production-ready prototype; it will be mostly empty\n");
    sb.append("      until mid-2017,\n");
    sb.append('\n');
    sb.append("https://dev-registry.erasmuswithoutpaper.eu/\n");
    sb.append("    - for active development; it will contain URLs to individual developers'\n");
    sb.append("      workstations and it may contain alpha implementations of draft APIs.\n");
    sb.append("\n\n");
    sb.append("API URLs\n");
    sb.append("--------\n");
    sb.append('\n');
    String root = Application.getRootUrl();
    sb.append(root + "/catalogue-v1.xml\n");
    sb.append("    - the catalogue itself, as documented in the Registry API specs.\n");
    sb.append('\n');
    sb.append(root + "/manifest.xml\n");
    sb.append("    - a \"self-manifest\" of the Registry's EWP Host.\n");
    sb.append('\n');
    sb.append('\n');
    sb.append("Other notable URLs\n");
    sb.append("------------------\n");
    sb.append('\n');
    sb.append("Until we roll out a proper web interface, you can use these URLs for navigating\n");
    sb.append("the registry:\n");
    sb.append('\n');
    sb.append(root + "/\n");
    sb.append("    - this page, just an introduction.\n");
    sb.append('\n');
    sb.append(root + "/status\n");
    sb.append("    - a status summary of all manifest sources.\n");
    sb.append('\n');
    sb.append(root + "/status?url=<manifest-source-url>\n");
    sb.append("    - details on particular manifest status (not part of the API).\n");
    sb.append('\n');
    sb.append(root + "/status?email=<admin-email-address>\n");
    sb.append("    - details on issues assigned to a particular person.\n");
    sb.append('\n');
    sb.append(root + "/refresh\n");
    sb.append("    - reload all manifest sources (not part of the API).\n");
    sb.append('\n');
    return new ResponseEntity<String>(sb.toString(), headers, HttpStatus.OK);
  }

  /**
   * Display a manifest status page.
   *
   * @param url URL of the manifest source.
   * @return A page describing the status of the manifest.
   */
  @RequestMapping(path = "/status", params = "url")
  public ResponseEntity<String> manifestStatus(@RequestParam String url) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_HTML);
    headers.setCacheControl("max-age=0, must-revalidate");
    headers.setExpires(0);

    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html>");
    sb.append("<pre style='word-wrap: break-word; white-space: pre-wrap; max-width: 700px'>");
    sb.append("Manifest status report\n");
    sb.append("======================\n");
    sb.append('\n');
    sb.append("Requested for manifest URL:\n" + Utils.escapeHtml(url) + '\n');
    sb.append('\n');
    sb.append("Status\n");
    sb.append("------\n");
    sb.append('\n');
    Optional<ManifestUpdateStatus> status =
        Optional.ofNullable(this.manifestStatusRepo.findOne(url));
    if (status.isPresent() && (!status.get().getLastAccessAttempt().isPresent())) {
      status = Optional.empty();
    }
    Optional<ManifestSource> source = this.sourceProvider.getOne(url);
    if (source.isPresent() == false && status.isPresent() == false) {
      sb.append("Unknown URL: This URL is not listed among the current Registry Service\n");
      sb.append("manifest sources, nor any trace of it can be found in our logs.\n");
    } else if (source.isPresent() == false && status.isPresent() == true) {
      sb.append("Stale URL: This URL was once listed among Registry Service sources, but\n");
      sb.append("it is not anymore.\n");
      sb.append('\n');
      sb.append("Last access attempt: " + this.formatTime(status.get().getLastAccessAttempt().get())
          + '\n');
    } else if (source.isPresent() == true && status.isPresent() == false) {
      sb.append("New URL: This URL is listed among the current Registry Service manifest\n");
      sb.append("sources, but it hasn't been accessed yet. Please refresh this page.\n");
    } else { // both are present
      sb.append("Last access attempt: " + this.formatTime(status.get().getLastAccessAttempt().get())
          + '\n');
      sb.append("Last access status: " + status.get().getLastAccessFlagStatus().toString() + '\n');
      if (status.get().getLastAccessNotices().size() > 0) {
        sb.append('\n');
        sb.append("Last access notices\n");
        sb.append("-------------------\n");
        sb.append("\n<ul>");
        for (UpdateNotice notice : status.get().getLastAccessNotices()) {
          sb.append("<li style='margin: 1em 0'>");
          sb.append("<p>[" + notice.getSeverity().toString() + "]</p>");
          sb.append(notice.getMessageHtml());
          sb.append("</li>");
        }
        sb.append("</ul>");
      }
    }
    sb.append("</pre>");
    return new ResponseEntity<String>(sb.toString(), headers, HttpStatus.OK);
  }

  /**
   * Perform an on-demand refresh of all manifests.
   *
   * @return A page with the confirmation.
   */
  @RequestMapping("/refresh")
  public ResponseEntity<String> refresh() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.setCacheControl("max-age=0, must-revalidate");
    headers.setExpires(0);

    this.taskExecutor.execute(new Runnable() {
      @Override
      public void run() {
        UiController.this.updater.reloadAllManifestSources();
      }
    });

    StringBuilder sb = new StringBuilder();
    sb.append("Your refresh request has been successfully queued.\n\n");
    sb.append("The catalogue should be updated in a moment:\n");
    sb.append(Application.getRootUrl());
    sb.append("/catalogue-v1.xml" + "\n\n");
    sb.append("(Note, that this page is not part of the API and can be removed at any moment!)");
    return new ResponseEntity<String>(sb.toString(), headers, HttpStatus.OK);
  }

  /**
   * @return A page with all manifest sources and their statuses.
   */
  @RequestMapping(path = "/status")
  public ResponseEntity<String> serviceStatus() {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    headers.setCacheControl("max-age=0, must-revalidate");
    headers.setExpires(0);

    StringBuilder sb = new StringBuilder();
    sb.append("Currently defined manifest sources and their statuses\n");
    sb.append("=====================================================\n");
    sb.append('\n');
    for (ManifestSource source : this.sourceProvider.getAll()) {
      ManifestUpdateStatus status = this.manifestStatusRepo.findOne(source.getUrl());
      sb.append("[" + status.getLastAccessFlagStatus().toString() + "] ");
      sb.append(source.getUrl() + '\n');
    }
    sb.append("\n(write us to add yours)\n");

    return new ResponseEntity<String>(sb.toString(), headers, HttpStatus.OK);
  }

  /**
   * Display a status page tailored for a given notification recipient.
   *
   * @param email Email address of the recipient.
   * @return A page with the list of issue statuses related to this recipient.
   */
  @RequestMapping(path = "/status", params = "email")
  public ResponseEntity<String> statusForRecipient(@RequestParam String email) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_HTML);
    headers.setCacheControl("max-age=0, must-revalidate");
    headers.setExpires(0);

    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html>");
    sb.append("<pre style='word-wrap: break-word; white-space: pre-wrap'>");
    sb.append("Recipient status report\n");
    sb.append("=======================\n");
    sb.append('\n');
    sb.append("Requested for recipient email address:\n" + Utils.escapeHtml(email) + '\n');
    sb.append('\n');
    sb.append("Issues being watched\n");
    sb.append("--------------------\n");
    sb.append('\n');

    List<NotifierFlag> flags = this.notifier.getFlagsWatchedBy(email);
    for (NotifierFlag flag : flags) {
      sb.append("[" + flag.getStatus() + "] ");
      sb.append(flag.getName());
      if (flag.getDetailsUrl().isPresent()) {
        sb.append(" - <a href='");
        sb.append(Utils.escapeHtml(flag.getDetailsUrl().get()));
        sb.append("'>details</a>");
      }
      sb.append('\n');
    }
    sb.append("</pre>");
    return new ResponseEntity<String>(sb.toString(), headers, HttpStatus.OK);
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
    for (Entry<String, String> entry : response.getHeadersMap().entrySet()) {
      headers.addProperty(entry.getKey(), entry.getValue());
    }
    result.add("headers", headers);
    return result;
  }

  /**
   * Format a date for humans.
   *
   * @param date the date to be formatted.
   * @return A regular {@link Date#toString()} with a suffix appended (e.g. "(3 days ago)").
   */
  private String formatTime(Date date) {
    if (date == null) {
      return "(never)";
    }
    StringBuilder sb = new StringBuilder();
    sb.append(date);
    sb.append(" (");
    sb.append(new PrettyTime().format(date));
    sb.append(')');
    return sb.toString();
  }
}
