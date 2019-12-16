package eu.erasmuswithoutpaper.registry.manifestoverview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.TestManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdater;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdaterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.junit.Before;
import org.junit.Test;

/**
 * Testing integration between {@link ManifestOverviewManager} and {@link RegistryUpdater}.
 * Tests whether duplicates are detected when manifests are updated and is repository correctly
 * updated.
 */
public class ManifestOverviewManagerTest extends WRTest {

  @Autowired
  private FakeInternet internet;

  @Autowired
  private TestManifestSourceProvider sourceProvider;

  @Autowired
  private ManifestsNotifiedAboutDuplicatesRepository manifestsNotifiedAboutDuplicatesRepository;

  @Autowired
  private RegistryUpdaterImpl updater;

  @Value("${app.admin-emails}")
  private List<String> adminEmails;

  private void reloadManifest(String manifestUrl) {
    this.updater.reloadManifestSource(this.sourceProvider.getOne(manifestUrl).get());
  }

  private boolean isEmailWithRecipients(String content, List<String> recipients, String subject,
      List<String> expectedInContent) {
    List<String> lines = Arrays.asList(content.split("\n"));
    if (!lines.get(0).startsWith("To:")) {
      return false;
    }
    for (String recipient : recipients) {
      if (!lines.get(0).contains(recipient)) {
        return false;
      }
    }
    if (!lines.get(1).equals("Subject: " + subject)) {
      return false;
    }
    String emailContent = String.join("\n", lines.subList(2, lines.size()));
    for (String expectedString : expectedInContent) {
      if (!emailContent.contains(expectedString)) {
        return false;
      }
    }
    return true;
  }

  private static final String duplicateDetectedSubject =
      "EWP Duplicate API implementation detected.";
  private static final String duplicateRemovedSubject =
      "EWP Duplicate API implementation was removed.";
  private static final String adminEmailSubject = "EWP Duplicate API status has changed.";
  private static final String manifestUrl1 = "https://example.com/1";
  private static final ManifestSource manifestSource1 =
      ManifestSource.newTrustedSource(manifestUrl1);
  private static final List<String> manifestEmails1 =
      Arrays.asList("manifest1-admin1@example.com", "manifest1-admin2@example.com");

  private static final String manifestUrl2 = "https://example.com/2";
  private static final ManifestSource manifestSource2 =
      ManifestSource.newTrustedSource(manifestUrl2);
  private static final List<String> manifestEmails2 =
      Arrays.asList("manifest2-admin1@example.com", "manifest2-admin2@example.com");
  private static final int adminEmailsCount1 = manifestEmails1.size();
  private static final int adminEmailsCount2 = manifestEmails2.size();

  @Before
  public void setUp() {
    this.internet.putURL(manifestUrl1, getManifest1WithoutDuplicate());
    this.internet.putURL(manifestUrl2, getManifest2WithoutDuplicate());

    this.sourceProvider.addSource(manifestSource1);
    this.sourceProvider.addSource(manifestSource2);

    this.updater.onSourcesUpdated();
  }

  @Test
  public void testEmailsAreNotSentWhenThereAreManifestsWithoutDuplicates() {
    assertThat(this.internet.popEmailsSent()).isEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl1)).isNull();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl2)).isNull();
  }

  @Test
  public void testEmailsAboutDuplicatesAreSentForDuplicatesInSingleManifest() {
    this.internet.putURL(manifestUrl1, getManifest1WithInternalDuplicate());
    this.reloadManifest(manifestUrl1);

    // Duplicate found - emails sent
    List<String> emailsSent = this.internet.popEmailsSent();
    assertThat(emailsSent).hasSize(adminEmailsCount1 + 1);
    this.checkEmail(emailsSent, manifestEmails1.get(0), duplicateDetectedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails1.get(1), duplicateDetectedSubject, manifestUrl1);
    this.checkEmail(emailsSent, adminEmails, adminEmailSubject, manifestUrl1);

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl1)).isNotNull();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl2)).isNull();

    // Already notified about duplicates - no emails sent
    this.reloadManifest(manifestUrl1);
    assertThat(this.internet.popEmailsSent()).isEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl1)).isNotNull();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl2)).isNull();

    // And change back to manifest without duplicates
    this.internet.putURL(manifestUrl1, getManifest1WithoutDuplicate());
    this.reloadManifest(manifestUrl1);

    emailsSent = this.internet.popEmailsSent();
    assertThat(emailsSent).hasSize(adminEmailsCount1 + 1);
    this.checkEmail(emailsSent, manifestEmails1.get(0), duplicateRemovedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails1.get(1), duplicateRemovedSubject, manifestUrl1);
    this.checkEmail(emailsSent, adminEmails, adminEmailSubject, manifestUrl1);
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl1)).isNull();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl2)).isNull();
  }

  @Test
  public void testEmailsAboutDuplicatesAreSentForDuplicatesInMultipleManifests() {
    this.internet.putURL(manifestUrl1, getManifest1WithExternalDuplicate());
    this.internet.putURL(manifestUrl2, getManifest2WithExternalDuplicate());

    this.reloadManifest(manifestUrl1);

    // No duplicates yet.
    assertThat(this.internet.popEmailsSent()).isEmpty();

    this.reloadManifest(manifestUrl2);

    // Now the duplicates are detected.
    List<String> emailsSent = this.internet.popEmailsSent();
    assertThat(emailsSent).hasSize(adminEmailsCount1 + adminEmailsCount2 + 1);
    this.checkEmail(emailsSent, manifestEmails1.get(0), duplicateDetectedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails1.get(1), duplicateDetectedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails2.get(0), duplicateDetectedSubject, manifestUrl2);
    this.checkEmail(emailsSent, manifestEmails2.get(1), duplicateDetectedSubject, manifestUrl2);
    this.checkEmail(emailsSent, adminEmails, adminEmailSubject,
        Arrays.asList(manifestUrl1, manifestUrl2));

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl1)).isNotNull();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl2)).isNotNull();

    // Manifest 2 changes - not it will contain different major version and duplicates will be gone.
    this.internet.putURL(manifestUrl2, getManifest2WithDuplicatedHeiIdButDifferentVersion());
    this.reloadManifest(manifestUrl2);

    emailsSent = this.internet.popEmailsSent();
    assertThat(emailsSent).hasSize(adminEmailsCount1 + adminEmailsCount2 + 1);
    this.checkEmail(emailsSent, manifestEmails1.get(0), duplicateRemovedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails1.get(1), duplicateRemovedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails2.get(0), duplicateRemovedSubject, manifestUrl2);
    this.checkEmail(emailsSent, manifestEmails2.get(1), duplicateRemovedSubject, manifestUrl2);
    this.checkEmail(emailsSent, adminEmails, adminEmailSubject,
        Arrays.asList(manifestUrl1, manifestUrl2));

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl1)).isNull();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl2)).isNull();
  }

  @Test
  public void testManifestsWithInternalAndExternalDuplicates() {
    this.internet.putURL(manifestUrl1, getManifest1WithInternalAndExternalDuplicate());
    this.internet.putURL(manifestUrl2, getManifest2WithExternalDuplicate());

    this.reloadManifest(manifestUrl1);

    // Duplicates in manifest1 detected.
    List<String> emailsSent = this.internet.popEmailsSent();
    assertThat(emailsSent).hasSize(adminEmailsCount1 + 1);
    this.checkEmail(emailsSent, manifestEmails1.get(0), duplicateDetectedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails1.get(1), duplicateDetectedSubject, manifestUrl1);
    this.checkEmail(emailsSent, adminEmails, adminEmailSubject, manifestUrl1);

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl1)).isNotNull();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl2)).isNull();


    this.reloadManifest(manifestUrl2);

    // Duplicates in manifest2 detected.
    emailsSent = this.internet.popEmailsSent();
    assertThat(emailsSent).hasSize(adminEmailsCount2 + 1);
    this.checkEmail(emailsSent, manifestEmails2.get(0), duplicateDetectedSubject, manifestUrl2);
    this.checkEmail(emailsSent, manifestEmails2.get(1), duplicateDetectedSubject, manifestUrl2);
    this.checkEmail(emailsSent, adminEmails, adminEmailSubject, manifestUrl2);

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl1)).isNotNull();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl2)).isNotNull();

    // Removing all duplicates from manifest 1.
    this.internet.putURL(manifestUrl1, getManifest1WithoutDuplicate());
    this.reloadManifest(manifestUrl1);

    emailsSent = this.internet.popEmailsSent();
    assertThat(emailsSent).hasSize(adminEmailsCount1 + adminEmailsCount2 + 1);
    this.checkEmail(emailsSent, manifestEmails1.get(0), duplicateRemovedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails1.get(1), duplicateRemovedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails2.get(0), duplicateRemovedSubject, manifestUrl2);
    this.checkEmail(emailsSent, manifestEmails2.get(1), duplicateRemovedSubject, manifestUrl2);
    this.checkEmail(emailsSent, adminEmails, adminEmailSubject,
        Arrays.asList(manifestUrl1, manifestUrl2));

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl1)).isNull();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findOne(manifestUrl2)).isNull();
  }

  private void checkEmail(List<String> emailsSent, String recipient, String subject,
      String contents) {
    checkEmail(emailsSent, Arrays.asList(recipient), subject, Arrays.asList(contents));
  }

  private void checkEmail(List<String> emailsSent, List<String> recipients, String subject,
      String contents) {
    checkEmail(emailsSent, recipients, subject, Arrays.asList(contents));
  }

  private void checkEmail(List<String> emailsSent, List<String> recipients,
      String subject, List<String> contents) {
    assertThat(emailsSent.stream().filter(
        s -> isEmailWithRecipients(s, recipients, subject, contents)))
        .hasSize(1);
  }

  private byte[] getManifest1WithInternalDuplicate() {
    return this.getFile("manifestoverview/manifest1-with-internal-duplicates.xml");
  }

  private byte[] getManifest1WithoutDuplicate() {
    return this.getFile("manifestoverview/manifest1-no-duplicates.xml");
  }

  private byte[] getManifest1WithExternalDuplicate() {
    return this.getFile("manifestoverview/manifest1-external-duplicates.xml");
  }

  private byte[] getManifest1WithInternalAndExternalDuplicate() {
    return this.getFile("manifestoverview/manifest1-with-internal-and-external-duplicates.xml");
  }

  private byte[] getManifest2WithoutDuplicate() {
    return this.getFile("manifestoverview/manifest2-no-duplicates.xml");
  }

  private byte[] getManifest2WithExternalDuplicate() {
    return this.getFile("manifestoverview/manifest2-external-duplicates.xml");
  }

  private byte[] getManifest2WithDuplicatedHeiIdButDifferentVersion() {
    return this.getFile("manifestoverview/manifest2-duplicated-hei-different-version.xml");
  }

}
