package eu.erasmuswithoutpaper.registry.configuration;

import java.util.List;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.notifier.NotifierFlag;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdater;
import eu.erasmuswithoutpaper.registry.updater.UptimeChecker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This component manages tasks which are scheduled to be run periodically in production
 * environment.
 */
@Profile({ "production", "development" })
@Component
public class ProductionScheduledTasks {

  private static final Logger logger = LoggerFactory.getLogger(ProductionConfiguration.class);

  private final RegistryUpdater updater;
  private final NotifierService notifier;
  private final ManifestRepository repo;
  private final UptimeChecker uptimeChecker;

  private final NotifierFlag manifestReloadingStatus;
  private final NotifierFlag notificationSendingStatus;
  private final NotifierFlag logPushingStatus;
  private final NotifierFlag uptimeCheckerStatus;

  /**
   * @param updater Needed to run periodical manifest updates.
   * @param notifier Needed to trigger sending notifications.
   * @param repo Needed to trigger pushing changes to remote repository.
   * @param adminEmails Needed for the recipients of error-notification flags.
   * @param uptimeChecker Needed to trigger fetching uptime-stats from remote server.
   */
  @Autowired
  public ProductionScheduledTasks(RegistryUpdater updater, NotifierService notifier,
      ManifestRepository repo, @Value("${app.admin-emails}") List<String> adminEmails,
      UptimeChecker uptimeChecker) {
    this.updater = updater;
    this.notifier = notifier;
    this.repo = repo;
    this.uptimeChecker = uptimeChecker;

    this.manifestReloadingStatus = new NotifierFlag(adminEmails) {
      @Override
      public String getName() {
        return "Status of background manifest reloading service.";
      }
    };
    this.notifier.addWatchedFlag(this.manifestReloadingStatus);

    this.notificationSendingStatus = new NotifierFlag(adminEmails) {
      @Override
      public String getName() {
        return "Status of background notification sending service.";
      }
    };
    this.notifier.addWatchedFlag(this.notificationSendingStatus);

    this.logPushingStatus = new NotifierFlag(adminEmails) {
      @Override
      public String getName() {
        return "Status of background \"git push\" service.";
      }
    };
    this.notifier.addWatchedFlag(this.logPushingStatus);

    this.uptimeCheckerStatus = new NotifierFlag(adminEmails) {
      @Override
      public String getName() {
        return "Status of background uptime-checker service.";
      }
    };
    this.notifier.addWatchedFlag(this.uptimeCheckerStatus);
  }

  /**
   * Push all commits to the remote repository (if there are any to be pushed).
   */
  @Scheduled(initialDelay = 0, fixedRate = 30000)
  public void pushGitCommits() {
    try {
      this.repo.push();
      this.logPushingStatus.setStatus(Severity.OK);
    } catch (GitAPIException e) {
      logger.error("GitAPIException while pushing repository changes: " + e);
      this.logPushingStatus.setStatus(Severity.WARNING);
    } catch (RuntimeException e) {
      logger.error("RuntimeException while pushing repository changes", e);
      this.logPushingStatus.setStatus(Severity.ERROR);
    }
  }

  /**
   * Fetch the current uptime-stats from remote monitoring service.
   */
  @Scheduled(initialDelay = 0, fixedRate = 30 * 60000)
  public void refreshUptimeStats() {
    try {
      this.uptimeChecker.refresh();
      this.uptimeCheckerStatus.setStatus(Severity.OK);
    } catch (RuntimeException e) {
      logger.error("RuntimeException while refreshing uptime stats", e);
      this.uptimeCheckerStatus.setStatus(Severity.ERROR);
    }
  }

  /**
   * Reload all manifest sources.
   */
  @Scheduled(initialDelay = 0, fixedRate = 300000)
  public void reloadManifestSources() {
    try {
      this.updater.reloadAllManifestSources();
      this.manifestReloadingStatus.setStatus(Severity.OK);
    } catch (RuntimeException e) {
      logger.error("RuntimeException while reloading manifests", e);
      this.manifestReloadingStatus.setStatus(Severity.ERROR);
    }
  }

  /**
   * Send notifications (if there are any to be sent).
   */
  @Scheduled(initialDelay = 0, fixedRate = 5000)
  public void sendNotifications() {
    try {
      this.notifier.sendNotifications();
      this.notificationSendingStatus.setStatus(Severity.OK);
    } catch (RuntimeException e) {
      logger.error("RuntimeException while sending notifications", e);
      this.notificationSendingStatus.setStatus(Severity.ERROR);
    }
  }
}
