package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.Application;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.repository.ManifestNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@ConditionalOnWebApplication
public class ManifestOverviewManager {
  private static final Logger logger = LoggerFactory.getLogger(ManifestOverviewManager.class);

  private final ManifestSourceProvider sourceProvider;
  private final ManifestRepository manifestRepository;
  private final ManifestsNotifiedAboutDuplicatesRepository
      manifestsNotifiedAboutDuplicatesRepository;

  private final Map<String, ManifestOverviewInfo> overviews;
  private final HashSet<String> manifestAlreadyNotified;
  private final List<String> adminEmails;
  private ImplementedApisCount implementedApisCount;
  private ApiForHeiImplementationMapping apiForHeiImplementationMappingDuplicates;
  private CoveredInstitutionsCounters heiDuplicates;

  private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
  private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();

  private final Lock notificationsLock = new ReentrantLock();
  private final Internet internet;
  private final Map<String, Set<String>> manifestUrlToAdminEmails = new HashMap<>();

  /**
   * Creates Manifest Overview Manager, which stores {@link ManifestOverviewInfo} for each of
   * covered manifests.
   *
   * @param sourceProvider
   *     Provides list of covered manifests.
   * @param manifestRepository
   *     Provides contents of covered manifests.
   * @param manifestsNotifiedAboutDuplicatesRepository
   *     Repository storing whether a notification about duplicate in a manifest was sent.
   * @param internet
   *     Internet implementation used to communicate with the outer world.
   * @param adminEmails
   *     List of emails that will receive notification about all detected duplicates.
   */
  @Autowired
  public ManifestOverviewManager(
      ManifestSourceProvider sourceProvider,
      ManifestRepository manifestRepository,
      ManifestsNotifiedAboutDuplicatesRepository manifestsNotifiedAboutDuplicatesRepository,
      Internet internet,
      @Value("${app.admin-emails}") List<String> adminEmails) {
    this.sourceProvider = sourceProvider;
    this.manifestRepository = manifestRepository;
    this.manifestsNotifiedAboutDuplicatesRepository = manifestsNotifiedAboutDuplicatesRepository;
    this.internet = internet;
    overviews = new HashMap<>();
    this.implementedApisCount = new ImplementedApisCount();
    this.apiForHeiImplementationMappingDuplicates = new ApiForHeiImplementationMapping();
    this.heiDuplicates = new CoveredInstitutionsCounters();
    this.adminEmails = adminEmails;

    Iterable<ManifestAlreadyNotifiedAboutDuplicates> manifestsAlreadyNotifiedIterable =
        this.manifestsNotifiedAboutDuplicatesRepository.findAll();

    this.manifestAlreadyNotified = new HashSet<>();
    for (ManifestAlreadyNotifiedAboutDuplicates manifestAlreadyNotifiedAboutDuplicates
        : manifestsAlreadyNotifiedIterable) {
      manifestAlreadyNotified.add(manifestAlreadyNotifiedAboutDuplicates.getManifestUrl());
    }
  }

  /**
   * Updates {@link ManifestOverviewInfo} for every known manifest.
   */
  public void updateAllManifests() {
    this.writeLock.lock();
    try {
      for (ManifestSource source : this.sourceProvider.getAll()) {
        this.updateManifestWithoutRecalculatingAggregates(source.getUrl());
      }
      this.recalculateAggregates();
    } finally {
      this.writeLock.unlock();
    }
    this.notifyAboutDuplicates();
  }

  /**
   * Updates {@link ManifestOverviewInfo} single manifest.
   *
   * @param manifestUrl
   *     URL of manifest to update.
   */
  public void updateManifest(String manifestUrl) {
    this.writeLock.lock();
    try {
      this.updateManifestWithoutRecalculatingAggregates(manifestUrl);
      this.recalculateAggregates();
    } finally {
      this.writeLock.unlock();
    }
    this.notifyAboutDuplicates();
  }

  private void updateManifestWithoutRecalculatingAggregates(String manifestUrl) {
    try {
      String manifest = manifestRepository.getManifestFiltered(manifestUrl);
      ManifestOverviewInfo manifestOverviewInfo =
          ManifestOverviewInfo.generateFromManifest(manifestUrl, manifest);
      if (manifestOverviewInfo != null) {
        this.overviews.put(manifestUrl, manifestOverviewInfo);
      }
    } catch (ManifestNotFound manifestNotFound) {
      // ignore not loaded manifests
    }
  }

  private void recalculateAggregates() {
    Collection<ManifestOverviewInfo> infos = this.overviews.values();

    this.implementedApisCount =
        ImplementedApisCount.fromManifestOverviewInfos(infos);

    ApiForHeiImplementationMapping apiForHeiImplementationMapping =
        ApiForHeiImplementationMapping.fromManifestOverviewInfos(infos);
    this.apiForHeiImplementationMappingDuplicates = apiForHeiImplementationMapping
            .getMappingWithDuplicates()
            .excludeExternalDuplicates(Arrays.asList("echo", "discovery"));

    CoveredInstitutionsCounters coveredInstitutionsCounters =
        CoveredInstitutionsCounters.fromManifestOverviewInfos(infos);
    this.heiDuplicates = coveredInstitutionsCounters.getOnlyDuplicates();
  }

  public void setManifestUrlAdminEmails(String manifestUrl, List<String> adminEmails) {
    this.manifestUrlToAdminEmails.put(manifestUrl, new HashSet<>(adminEmails));
  }

  public static class ManifestOverviewState {
    public Collection<ManifestOverviewInfo> overviewInfos;
    public ImplementedApisCount implementedApisCount;
    public ApiForHeiImplementationMapping apiForHeiImplementationMappingDuplicates;
    public CoveredInstitutionsCounters getHeiDuplicates;

    ManifestOverviewState(
        Collection<ManifestOverviewInfo> overviewInfos,
        ImplementedApisCount implementedApisCount,
        ApiForHeiImplementationMapping apiForHeiImplementationMappingDuplicates,
        CoveredInstitutionsCounters getHeiDuplicates) {
      this.overviewInfos = overviewInfos;
      this.implementedApisCount = implementedApisCount;
      this.apiForHeiImplementationMappingDuplicates = apiForHeiImplementationMappingDuplicates;
      this.getHeiDuplicates = getHeiDuplicates;
    }
  }

  /**
   * Getter for current internal state of this Manifest Overview Manager.
   * Single getter is provided instead of four separate to guarantee consistency in multithreaded
   * environment. Returned references are read-only.
   *
   * @return {@link ManifestOverviewState} with current state of this Manager.
   */
  public ManifestOverviewState getManifestOverviewState() {
    this.readLock.lock();
    try {
      return new ManifestOverviewState(
          Collections.unmodifiableCollection(new ArrayList<>(overviews.values())),
          implementedApisCount,
          apiForHeiImplementationMappingDuplicates,
          heiDuplicates
      );
    } finally {
      this.readLock.unlock();
    }
  }

  private void notifyAboutDuplicates() {
    this.notificationsLock.lock();
    try {
      this.notifyAboutDuplicatesLocked();
    } finally {
      this.notificationsLock.unlock();
    }
  }

  private void notifyAboutDuplicatesLocked() {
    Set<String> manifestsWithDuplicates = this.getManifestsWithDuplicates();
    Set<String> newDuplicates =
        new HashSet<>(Sets.difference(manifestsWithDuplicates, this.manifestAlreadyNotified));
    Set<String> notDuplicatedAnymore =
        new HashSet<>(Sets.difference(this.manifestAlreadyNotified, manifestsWithDuplicates));

    if (newDuplicates.isEmpty() && notDuplicatedAnymore.isEmpty()) {
      return;
    }

    notifyEwpAdministrators(newDuplicates, notDuplicatedAnymore);

    for (String manifestWithDuplicateUrl : newDuplicates) {
      notifyAboutNewDuplicate(manifestWithDuplicateUrl);
    }

    for (String manifestWithoutDuplicateUrl : notDuplicatedAnymore) {
      notifyAboutRemovedDuplicate(manifestWithoutDuplicateUrl);
    }
  }

  private void notifyAboutRemovedDuplicate(String manifestWithoutDuplicateUrl) {
    final String subject = "EWP Duplicate API instances was removed.";
    final String contentTemplate = String.join("",
        "Hello %s,\n",
        "duplicate API from manifest at %s was removed.\n\n",
        "We will inform you about any other duplicates we will find. ",
        "You can consult %s at any time to find if there are any duplicates in the EWP Network."
    );

    if (this.sendNotificationEmail(manifestWithoutDuplicateUrl, subject, contentTemplate)) {
      this.manifestsNotifiedAboutDuplicatesRepository.delete(manifestWithoutDuplicateUrl);
      this.manifestAlreadyNotified.remove(manifestWithoutDuplicateUrl);
    }
  }

  private void notifyAboutNewDuplicate(String manifestWithDuplicateUrl) {
    final String subject = "EWP Duplicate API instances detected.";
    final String contentTemplate = String.join("",
        "Hello %s,\n",
        "we have detected duplicate API in your manifest located at %s.\n\n",
        "Please consult %s to find the duplicate. If this duplicate is your error, then please ",
        "correct it. If you think that this is someone else error then please contact them. ",
        "If you won't be able to communicate with the partner that creates the duplicate, ",
        "then contact EWP Administration and describe your problem."
    );

    if (this.sendNotificationEmail(manifestWithDuplicateUrl, subject, contentTemplate)) {
      this.manifestsNotifiedAboutDuplicatesRepository.save(
          new ManifestAlreadyNotifiedAboutDuplicates(manifestWithDuplicateUrl)
      );
      this.manifestAlreadyNotified.add(manifestWithDuplicateUrl);
    }
  }

  private boolean sendNotificationEmail(String manifestUrl, String subject,
      String contentTemplate) {
    final String manifestsOverviewUrl = Application.getRootUrl() + "/manifestsOverview";
    Optional<ManifestSource> source = this.sourceProvider.getOne(manifestUrl);
    if (!source.isPresent()) {
      logger.error("Unexpected manifest URL encountered, no source is defined for it. URL: {}",
          manifestUrl);
      return false;
    }

    Set<String> emails = this.manifestUrlToAdminEmails.getOrDefault(manifestUrl, new HashSet<>());

    for (String email : emails) {
      String contents = String.format(contentTemplate, email, manifestUrl, manifestsOverviewUrl);
      this.internet.queueEmail(Collections.singletonList(email), subject, contents);
    }
    return true;
  }

  private void notifyEwpAdministrators(Set<String> newDuplicates,
      Set<String> notDuplicatedAnymore) {
    StringBuilder sb = new StringBuilder();
    sb.append("Duplicated API status has changed.\n");
    if (!newDuplicates.isEmpty()) {
      sb.append("New duplicates were found in these manifests:\n");
      sb.append(String.join(", ", newDuplicates));
      sb.append(".\n");
    }
    if (!notDuplicatedAnymore.isEmpty()) {
      sb.append("Duplicates were removed from these manifests:\n");
      sb.append(String.join(", ", notDuplicatedAnymore));
      sb.append(".\n");
    }

    final String contents = sb.toString();
    final String subject = "EWP Duplicate API status has changed.";

    this.internet.queueEmail(this.adminEmails, subject, contents);
  }

  private Set<String> getManifestsWithDuplicates() {
    ManifestOverviewState manifestOverviewState = this.getManifestOverviewState();
    ApiForHeiImplementationMapping duplicatedApis =
        manifestOverviewState.apiForHeiImplementationMappingDuplicates;

    Set<String> manifestsWithDuplicates = new HashSet<>();
    for (Map.Entry<ApiHeiAndMajorVersionTuple, Map<ManifestAndHostIndex, List<String>>> entry :
        duplicatedApis.getMap().entrySet()) {
      Collection<ManifestAndHostIndex> manifestsAndHeisWithDuplicates = entry.getValue().keySet();
      manifestsWithDuplicates.addAll(
          manifestsAndHeisWithDuplicates.stream()
              .map(ManifestAndHostIndex::getManifestUrl)
              .collect(Collectors.toSet())
      );
    }
    return manifestsWithDuplicates;
  }
}
