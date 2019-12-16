package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import eu.erasmuswithoutpaper.registry.repository.ManifestNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManifestOverviewManager {
  private final ManifestSourceProvider sourceProvider;
  private final ManifestRepository manifestRepository;
  private final Map<String, ManifestOverviewInfo> overviews;
  private ImplementedApisCount implementedApisCount;
  private ApiForHeiImplementationMapping apiForHeiImplementationMappingDuplicates;
  private CoveredInstitutionsCounters heiDuplicates;

  private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.WriteLock writeLock = readWriteLock.writeLock();
  private final ReentrantReadWriteLock.ReadLock readLock = readWriteLock.readLock();

  /**
   * Creates Manifest Overview Manager, which stores {@link ManifestOverviewInfo} for each of
   * covered manifests.
   *
   * @param sourceProvider
   *     Provides list of covered manifests.
   * @param manifestRepository
   *     Provides contents of covered manifests.
   */
  @Autowired
  public ManifestOverviewManager(
      ManifestSourceProvider sourceProvider,
      ManifestRepository manifestRepository) {
    this.sourceProvider = sourceProvider;
    this.manifestRepository = manifestRepository;
    overviews = new HashMap<>();
    this.implementedApisCount = new ImplementedApisCount();
    this.apiForHeiImplementationMappingDuplicates = new ApiForHeiImplementationMapping();
    this.heiDuplicates = new CoveredInstitutionsCounters();
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
    this.apiForHeiImplementationMappingDuplicates =
        apiForHeiImplementationMapping.getMappingWithDuplicates();

    CoveredInstitutionsCounters coveredInstitutionsCounters =
        CoveredInstitutionsCounters.fromManifestOverviewInfos(infos);
    this.heiDuplicates = coveredInstitutionsCounters.getOnlyDuplicates();
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
}
