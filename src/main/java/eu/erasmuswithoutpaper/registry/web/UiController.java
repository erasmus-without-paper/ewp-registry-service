package eu.erasmuswithoutpaper.registry.web;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.common.Utils;
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
import org.springframework.web.bind.annotation.RequestParam;

import org.ocpsoft.prettytime.PrettyTime;

/**
 * Handles UI requests.
 */
@Controller
public class UiController {

  private final TaskExecutor taskExecutor;
  private final ManifestUpdateStatusRepository manifestStatusRepo;
  private final ManifestSourceProvider sourceProvider;
  private final RegistryUpdater updater;
  private final NotifierService notifier;
  private final UptimeChecker uptimeChecker;

  /**
   * @param taskExecutor needed for running background tasks.
   * @param manifestUpdateStatuses needed to display statuses of manifests.
   * @param sourceProvider needed to present the list of all sources.
   * @param updater needed to perform on-demand manifest updates.
   * @param notifier needed to retrieve issues watched by particular recipients.
   * @param uptimeChecker needed to display current uptime stats.
   */
  @Autowired
  public UiController(TaskExecutor taskExecutor,
      ManifestUpdateStatusRepository manifestUpdateStatuses, ManifestSourceProvider sourceProvider,
      RegistryUpdater updater, NotifierService notifier, UptimeChecker uptimeChecker) {
    this.taskExecutor = taskExecutor;
    this.manifestStatusRepo = manifestUpdateStatuses;
    this.sourceProvider = sourceProvider;
    this.updater = updater;
    this.notifier = notifier;
    this.uptimeChecker = uptimeChecker;
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
    sb.append("\n");
    sb.append("Limited time deal - test our gorgeous \"text/plain skin\" for free!\n");
    sb.append("\n");
    String artifactVersion = this.getClass().getPackage().getImplementationVersion();
    sb.append("Version " + artifactVersion + ".\n");
    sb.append("Uptime ratios:\n");
    sb.append("- Last 24 hours: ");
    sb.append(this.uptimeChecker.getLast24HoursUptimeRatio());
    sb.append("\n");
    sb.append("- Last 7 days: ");
    sb.append(this.uptimeChecker.getLast7DaysUptimeRatio());
    sb.append("\n");
    sb.append("- Last 30 days: ");
    sb.append(this.uptimeChecker.getLast30DaysUptimeRatio());
    sb.append("\n");
    sb.append("- Last 365 days: ");
    sb.append(this.uptimeChecker.getLast365DaysUptimeRatio());
    sb.append("\n");
    sb.append("\n\n");
    sb.append("PRODUCTION and DEVELOPMENT Registry Service installations\n");
    sb.append("---------------------------------------------------------\n");
    sb.append("\n");
    sb.append("The EWP Network is still being designed. During this time, we will use two\n");
    sb.append("separate Registry Service installations:\n");
    sb.append("\n");
    sb.append("https://registry.erasmuswithoutpaper.eu/\n");
    sb.append("    - the official production-ready prototype; it will be mostly empty\n");
    sb.append("      until mid-2017,\n");
    sb.append("\n");
    sb.append("https://dev-registry.erasmuswithoutpaper.eu/\n");
    sb.append("    - for active development; it will contain URLs to individual developers'\n");
    sb.append("      workstations and it may contain alpha implementations of draft APIs.\n");
    sb.append("\n\n");
    sb.append("API URLs\n");
    sb.append("--------\n");
    sb.append("\n");
    String root = Application.getRootUrl();
    sb.append(root + "/catalogue-v1.xml\n");
    sb.append("    - the catalogue itself, as documented in the Registry API specs.\n");
    sb.append("\n");
    sb.append(root + "/manifest.xml\n");
    sb.append("    - a \"self-manifest\" of the Registry's EWP Host.\n");
    sb.append("\n");
    sb.append("\n");
    sb.append("Other notable URLs\n");
    sb.append("------------------\n");
    sb.append("\n");
    sb.append("Until we roll out a proper web interface, you can use these URLs for navigating\n");
    sb.append("the registry:\n");
    sb.append("\n");
    sb.append(root + "/\n");
    sb.append("    - this page, just an introduction.\n");
    sb.append("\n");
    sb.append(root + "/status\n");
    sb.append("    - a status summary of all manifest sources.\n");
    sb.append("\n");
    sb.append(root + "/status?url=<manifest-source-url>\n");
    sb.append("    - details on particular manifest status (not part of the API).\n");
    sb.append("\n");
    sb.append(root + "/status?email=<admin-email-address>\n");
    sb.append("    - details on issues assigned to a particular person.\n");
    sb.append("\n");
    sb.append(root + "/refresh\n");
    sb.append("    - reload all manifest sources (not part of the API).\n");
    sb.append("\n");
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
    sb.append("\n");
    sb.append("Requested for manifest URL:\n" + Utils.escapeHtml(url) + "\n");
    sb.append("\n");
    sb.append("Status\n");
    sb.append("------\n");
    sb.append("\n");
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
      sb.append("\n");
      sb.append("Last access attempt: " + this.formatTime(status.get().getLastAccessAttempt().get())
          + "\n");
    } else if (source.isPresent() == true && status.isPresent() == false) {
      sb.append("New URL: This URL is listed among the current Registry Service manifest\n");
      sb.append("sources, but it hasn't been accessed yet. Please refresh this page.\n");
    } else { // both are present
      sb.append("Last access attempt: " + this.formatTime(status.get().getLastAccessAttempt().get())
          + "\n");
      sb.append("Last access status: " + status.get().getLastAccessFlagStatus().toString() + "\n");
      if (status.get().getLastAccessNotices().size() > 0) {
        sb.append("\n");
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
    sb.append("\n");
    for (ManifestSource source : this.sourceProvider.getAll()) {
      ManifestUpdateStatus status = this.manifestStatusRepo.findOne(source.getUrl());
      sb.append("[" + status.getLastAccessFlagStatus().toString() + "] ");
      sb.append(source.getUrl() + "\n");
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
    sb.append("\n");
    sb.append("Requested for recipient email address:\n" + Utils.escapeHtml(email) + "\n");
    sb.append("\n");
    sb.append("Issues being watched\n");
    sb.append("--------------------\n");
    sb.append("\n");

    List<NotifierFlag> flags = this.notifier.getFlagsWatchedBy(email);
    for (NotifierFlag flag : flags) {
      sb.append("[" + flag.getStatus() + "] ");
      sb.append(flag.getName());
      if (flag.getDetailsUrl().isPresent()) {
        sb.append(" - <a href='");
        sb.append(Utils.escapeHtml(flag.getDetailsUrl().get()));
        sb.append("'>details</a>");
      }
      sb.append("\n");
    }
    sb.append("</pre>");
    return new ResponseEntity<String>(sb.toString(), headers, HttpStatus.OK);
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
