package eu.erasmuswithoutpaper.registry.updater;

import static org.joox.JOOX.$;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.common.Severity.OneOfTheValuesIsUndetermined;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.constraints.FailedConstraintNotice;
import eu.erasmuswithoutpaper.registry.constraints.ManifestConstraint;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildError;
import eu.erasmuswithoutpaper.registry.documentbuilder.BuildParams;
import eu.erasmuswithoutpaper.registry.documentbuilder.EwpDocBuilder;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.internet.Internet;
import eu.erasmuswithoutpaper.registry.manifestoverview.ManifestOverviewManager;
import eu.erasmuswithoutpaper.registry.notifier.NotifierFlag;
import eu.erasmuswithoutpaper.registry.notifier.NotifierService;
import eu.erasmuswithoutpaper.registry.repository.CatalogueNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestNotFound;
import eu.erasmuswithoutpaper.registry.repository.ManifestRepository;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.ManifestParser.NotValidManifest;
import eu.erasmuswithoutpaper.registry.xmlformatter.XmlFormatter;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;
import org.joox.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Our implementation of {@link RegistryUpdater} interface.
 *
 * <p>
 * {@link RegistryUpdaterImpl} should be referenced directly <b>only in unit tests</b>. Otherwise,
 * use the {@link RegistryUpdater} interface!
 * </p>
 */
@Service
@ConditionalOnWebApplication
public class RegistryUpdaterImpl implements RegistryUpdater {

  private static final Logger logger = LoggerFactory.getLogger(RegistryUpdaterImpl.class);

  private final ManifestSourceProvider manifestSourceProvider;
  private final ManifestUpdateStatusRepository manifestUpdateStatusRepository;
  private final Internet internet;
  private final ManifestRepository repo;
  private final EwpDocBuilder docBuilder;
  private final XmlFormatter xmlFormatter;
  private final Map<ManifestSource, ManifestUpdateStatusNotifierFlag> notifierFlags;
  private final NotifierService notifier;
  private final ManifestParser parser;
  private final ManifestOverviewManager manifestOverviewManager;
  private final RegistryClient registryClient;
  private final Map<String, Document> cachedManifestsMap;

  /**
   * @param manifestSourceProvider         to get the list of Manifest sources.
   * @param manifestUpdateStatusRepository to manage the {@link ManifestUpdateStatus}es, for each of
   *                                       the manifests.
   * @param internet                       to fetch the manifest contents.
   * @param repo                           to store the new content of the fetched manifests.
   * @param docBuilder                     to parse the manifests.
   * @param xmlFormatter                   to format the filtered versions of the manifests.
   * @param notifier                       to register our custom {@link NotifierFlag}s.
   * @param parser                         to be able to parse manifests in supported versions.
   * @param manifestOverviewManager        stores
   *         {@link eu.erasmuswithoutpaper.registry.manifestoverview.ManifestOverviewInfo} for
   *         each of covered manifests.
   * @param registryClient                 to be able to check current catalogue state.
   */
  @Autowired
  public RegistryUpdaterImpl(ManifestSourceProvider manifestSourceProvider,
      ManifestUpdateStatusRepository manifestUpdateStatusRepository, Internet internet,
      ManifestRepository repo, EwpDocBuilder docBuilder, XmlFormatter xmlFormatter,
      NotifierService notifier, ManifestParser parser,
      ManifestOverviewManager manifestOverviewManager, RegistryClient registryClient) {
    this.manifestSourceProvider = manifestSourceProvider;
    this.manifestUpdateStatusRepository = manifestUpdateStatusRepository;
    this.internet = internet;
    this.repo = repo;
    this.docBuilder = docBuilder;
    this.xmlFormatter = xmlFormatter;
    this.notifier = notifier;
    this.manifestOverviewManager = manifestOverviewManager;
    this.registryClient = registryClient;
    this.notifierFlags = new HashMap<>();
    this.parser = parser;
    this.cachedManifestsMap = new HashMap<>();
    if (this.manifestSourceProvider.getAll() != null) {
      this.onSourcesUpdated();
    }
  }

  private void onManifestAdminEmailsChanged(String manifestUrl, List<String> adminEmails) {
    this.manifestOverviewManager.setManifestUrlAdminEmails(manifestUrl, adminEmails);
  }

  /**
   * This needs to be called if the sources provided by {@link ManifestSourceProvider} have changed.
   */
  public final void onSourcesUpdated() {

    /*
     * Lock the manifest repository for exclusive write access as read will be acquired by {@link
     * ManifestOverviewManager}. We must be sure to always take repository lock first!
     * Write lock is used in `updateTheCatalogue` method and that is why read is not enough!
     */

    this.repo.acquireWriteLock();
    try {

      List<ManifestSource> manifestSources = manifestSourceProvider.getAll();
      if (manifestSources == null) {
        return;
      }
      /*
       * First, try to find flags which are no longer valid because our manifestSourceProvider
       * stopped
       * serving the ManifestSources related to these flags.
       */
      Set<ManifestSource> sources = Sets.newLinkedHashSet(manifestSources);
      for (Iterator<Map.Entry<ManifestSource, ManifestUpdateStatusNotifierFlag>> iter =
           this.notifierFlags.entrySet().iterator(); iter.hasNext(); ) {
        Map.Entry<ManifestSource, ManifestUpdateStatusNotifierFlag> entry = iter.next();
        ManifestSource source = entry.getKey();
        if (!sources.contains(source)) {
          this.notifier.removeWatchedFlag(entry.getValue());
          this.manifestOverviewManager.updateManifest(source.getUrl());
          iter.remove();
        }
      }

      /*
       * Second, create new flags for all ManifestSources for which no flags have been yet
       * created. In
       * order to properly set the recipients for those flags, we need to fetch the catalogue.
       */
      Map<String, List<String>> recipients = new HashMap<>();
      try {
        Match catalogue =
            $(this.docBuilder.build(new BuildParams(this.repo.getCatalogue())).getDocument()
                .get()).namespaces(KnownNamespace.prefixMap());
        for (Match host : catalogue.xpath("r:host").each()) {
          for (Element child : host.children()) {
            List<String> adminEmails = new ArrayList<>();
            if ("admin-email".equals(child.getLocalName())) {
              adminEmails.add(child.getTextContent());
            } else if ("apis-implemented".equals(child.getLocalName())) {
              for (Node api : Utils.asNodeList(child.getChildNodes())) {
                if ("discovery".equals(api.getLocalName())) {
                  for (Node node : Utils.asNodeList(api.getChildNodes())) {
                    if ("url".equals(node.getLocalName())) {
                      String url = node.getTextContent();
                      recipients.put(url, adminEmails);
                      this.onManifestAdminEmailsChanged(url, adminEmails);
                    }
                  }
                }
              }
            }
          }
        }
      } catch (CatalogueNotFound e) {
        // No recipients yet.
      }
      for (ManifestSource source : sources) {
        ManifestUpdateStatusNotifierFlag flag = new ManifestUpdateStatusNotifierFlag(source);
        if (recipients.containsKey(source.getUrl())) {
          flag.setRecipientEmails(recipients.get(source.getUrl()));
        }
        if (!this.notifierFlags.containsKey(source)) {
          this.notifier.addWatchedFlag(flag);
          this.notifierFlags.put(source, flag);
        }
      }

      /*
       * Clean up all obsolete ManifestUpdateStatus entries.
       */
      Set<String> sourceUrls = new LinkedHashSet<>();
      for (ManifestSource source : sources) {
        sourceUrls.add(source.getUrl());
      }
      for (ManifestUpdateStatus status : this.manifestUpdateStatusRepository.findAll()) {
        if (!sourceUrls.contains(status.getUrl())) {
          this.manifestUpdateStatusRepository.delete(status);
        }
      }

      this.manifestOverviewManager.updateAllManifests();
      this.updateTheCatalogue(true);
    } finally {
      this.repo.releaseWriteLock();
    }
  }

  @Override
  public void reloadAllManifestSources() {
    if (this.manifestSourceProvider.update()) {
      this.onSourcesUpdated();
    }

    List<ManifestSource> manifestProviders = this.manifestSourceProvider.getAll();
    if (manifestProviders == null) {
      return;
    }

    for (ManifestSource source : manifestProviders) {
      this.reloadManifestSource(source);
    }
  }

  @Override
  public void reloadManifestSource(ManifestSource source) {

    // Get the flag for this source.

    ManifestUpdateStatusNotifierFlag notifierFlag = this.notifierFlags.get(source);
    if (notifierFlag == null) {
      /*
       * Developer forgot to run #onSourcesUpdated() after the list of manifestSourceProvider
       * sources were updated. (This list will not be updated in production environment, but it is
       * often updated during tests.)
       */
      throw new RuntimeException("notifierFlags were not updated");
    }

    // This will hold our list of notices/warnings.

    List<UpdateNotice> notices = new ArrayList<>();

    // Fetch the previous status from the database, or create a new one.
    String sourceUrl = source.getUrl();
    ManifestUpdateStatus status = this.manifestUpdateStatusRepository
        .findById(sourceUrl).orElseGet(() -> new ManifestUpdateStatus(sourceUrl));

    try {

      // Update the last access attempt.

      status.setLastAccessAttempt(new Date());

      // Read the original contents from the source.

      byte[] originalContents;
      try {
        originalContents = this.internet.getUrl(source.getUrl());
      } catch (IOException e) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p>The Registry was unable to fetch the manifest from its location.\n");
        sb.append("The IOException reported was:</p>");
        sb.append("<p><code>" + Utils.escapeHtml(e.toString()) + "</code></p>");
        notices.add(new UpdateNotice(Severity.ERROR, sb.toString()));
        status.setLastAccessFlagStatus(Severity.ERROR);
        status.setLastAccessNotices(notices);
        return;
      }

      // Lock the manifest repository for exclusive read and write access.

      this.repo.acquireWriteLock();
      try {

        // Store the original contents to the repository.

        this.repo.putOriginalManifest(source.getUrl(), originalContents);

        // Try to read it (and dynamically convert it to version 5).

        Document doc;
        List<BuildError> nonLethalErrors = new ArrayList<>();
        try {
          doc = this.parser.parseManifest(originalContents, nonLethalErrors);
        } catch (NotValidManifest e) {
          // The manifest failed basic validation. We cannot continue.

          this.repo.commit("Update original (invalid!) contents of manifest: " + source.getUrl());
          notices.add(new UpdateNotice(Severity.ERROR,
              "The file doesn't contain a proper supported manifest element. "
                  + "The manifest will not be imported. We will keep serving the last "
                  + "successfully imported version of this manifest, if we have one."));
          for (String error : e.getErrorList()) {
            notices.add(new UpdateNotice(Severity.ERROR, Utils.escapeHtml(error)));
          }
          status.setLastAccessFlagStatus(Severity.ERROR);
          status.setLastAccessNotices(notices);
          return;
        }

        Severity noticesSeverity = Severity.OK;

        if (!nonLethalErrors.isEmpty()) {
          notices.add(new UpdateNotice(Severity.WARNING,
              "This file contains some invalid elements inside one of "
                  + "<code>&lt;apis-implemented&gt;<code> elements."
                  + " This manifest will be imported but incorrect APIs will be ignored."));
          for (BuildError error : nonLethalErrors) {
            notices.add(new UpdateNotice(Severity.WARNING, Utils.escapeHtml(error.getMessage())));
          }
          noticesSeverity = Severity.WARNING;
        }

        /*
         * The contents passed XML Schema validation, but they are still unsafe. We need to create
         * the filtered version of these contents. We do this by applying all the filters which has
         * been connected to this manifest source.
         *
         * Filters may generate additional notices. These notices won't prevent the whole manifest
         * from being imported, but the filtering process may also change (or remove) parts of the
         * manifest document.
         */

        for (ManifestConstraint constraint : source.getConstraints()) {
          for (FailedConstraintNotice notice : constraint.filter(doc, registryClient)) {
            // We are converting one type of notice to another.
            notices.add(new UpdateNotice(notice.getSeverity(), notice.getMessageHtml()));
            try {
              if (notice.getSeverity().isMoreSevereThan(noticesSeverity)) {
                noticesSeverity = notice.getSeverity();
              }
            } catch (OneOfTheValuesIsUndetermined e) {
              throw new RuntimeException(e); // won't happen
            }
          }
        }

        /*
         * The document is now filtered and safe. We need to store the filtered version to the
         * repository. First, we will run a formatter against it.
         */

        String filteredContents = this.xmlFormatter.format(doc);
        boolean changed = this.repo.putFilteredManifest(source.getUrl(), filteredContents);

        // If anything changed...

        if (changed) {
          this.cachedManifestsMap.remove(source.getUrl());

          // Update the list of our notifierFlag's recipients.

          Match manifest = $(doc).namespaces(KnownNamespace.prefixMap());
          List<String> emails = manifest.xpath("mf6:host/ewp:admin-email").texts();
          notifierFlag.setRecipientEmails(emails);
          this.onManifestAdminEmailsChanged(source.getUrl(), emails);

          // Update the catalogue too.

          this.updateTheCatalogue(false);

          // And update manifest overview info in manifest overview manager.

          this.manifestOverviewManager.updateManifest(source.getUrl());

          // Commit repository changes.

          StringBuilder sb = new StringBuilder();
          sb.append("Update manifest");
          if (notices.size() > 0) {
            sb.append(" (").append(notices.size()).append(" notices)");
          }
          sb.append(": ").append(source.getUrl());
          this.repo.commit(sb.toString());
        }

        // Update the manifest status.

        status.setLastAccessFlagStatus(noticesSeverity);
        status.setLastAccessNotices(notices);
        return;

      } finally {
        this.repo.releaseWriteLock();
      }
    } finally {
      this.manifestUpdateStatusRepository.save(status);
      if (!status.getLastAccessNotices().isEmpty()) {
        logger.info("Manifest update notices for url: {}, notifier flag name: {}, notices: {}",
            status.getUrl(), notifierFlag.getName(), status.getLastAccessNotices());
      }
      notifierFlag.setStatus(status.getLastAccessFlagStatus());

      logger.info("Reloading " + source.getUrl() + " ("
          + status.getLastAccessFlagStatus().toString() + ")");
    }
  }

  private void updateTheCatalogue(boolean commit) {
    this.repo.acquireWriteLock();
    try {
      List<Document> manifests = new ArrayList<>();

      List<ManifestSource> manifestSources = manifestSourceProvider.getAll();
      if (manifestSources == null) {
        throw new UnsupportedOperationException("Manifest sources cannot be empty!");
      }

      for (ManifestSource src : manifestSources) {

        // Get the filtered copy of the manifest XML from the repository.

        byte[] xml;
        try {
          xml = this.repo.getManifestFiltered(src.getUrl()).getBytes(StandardCharsets.UTF_8);
        } catch (ManifestNotFound e) {
          /*
           * This may happen after new manifest sources are added, but the system wasn't able to
           * fetch them yet.
           */
          continue;
        }

        try {
          if (this.cachedManifestsMap.containsKey(src.getUrl())) {
            manifests.add(this.cachedManifestsMap.get(src.getUrl()));
          } else {
            Document manifest = this.parser.parseManifest(xml, null);
            this.cachedManifestsMap.put(src.getUrl(), manifest);
            manifests.add(manifest);
          }
        } catch (NotValidManifest e) {
          logger.error("Ignoring {}, because couldn't load it (should not happen)", src);
          for (String err : e.getErrorList()) {
            logger.error(err);
          }
        }
      }

      // Build the catalogue document.

      CatalogueBuilder builder = new CatalogueBuilder();
      Document catalogueDocument = builder.build(manifests);
      String catalogueXml = this.xmlFormatter.format(catalogueDocument);

      // Store it.

      this.repo.putCatalogue(catalogueXml, registryClient);
      if (commit) {
        this.repo.commit("Update catalogue");
      }
    } finally {
      this.repo.releaseWriteLock();
    }
  }
}
