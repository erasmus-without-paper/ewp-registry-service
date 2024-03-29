package eu.erasmuswithoutpaper.registry.manifestoverview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.WRTest;
import eu.erasmuswithoutpaper.registry.internet.FakeInternet;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSource;
import eu.erasmuswithoutpaper.registry.sourceprovider.ManifestSourceFactory;
import eu.erasmuswithoutpaper.registry.sourceprovider.TestManifestSourceProvider;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdater;
import eu.erasmuswithoutpaper.registry.updater.RegistryUpdaterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.annotation.DirtiesContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testing integration between {@link ManifestOverviewManager} and {@link RegistryUpdater}.
 * Tests whether duplicates are detected when manifests are updated and is repository correctly
 * updated.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
// fixes problem of @DirtiesContext: https://stackoverflow.com/questions/59591979/
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class ManifestOverviewManagerTest extends WRTest {

  @Autowired
  private FakeInternet internet;

  @Autowired
  private TestManifestSourceProvider sourceProvider;

  @Autowired
  private ManifestsNotifiedAboutDuplicatesRepository manifestsNotifiedAboutDuplicatesRepository;

  @Autowired
  private RegistryUpdaterImpl updater;

  @Autowired
  private ManifestSourceFactory manifestFactory;

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
      "EWP Duplicate API instances detected.";
  private static final String duplicateRemovedSubject =
      "EWP Duplicate API instances was removed.";
  private static final String adminEmailSubject = "EWP Duplicate API status has changed.";
  private static final String manifestUrl1 = "https://example.com/1";
  private ManifestSource manifestSource1;
  private static final List<String> manifestEmails1 =
      Arrays.asList("manifest1-admin1@example.com", "manifest1-admin2@example.com");

  private static final String manifestUrl2 = "https://example.com/2";
  private ManifestSource manifestSource2;
  private static final List<String> manifestEmails2 =
      Arrays.asList("manifest2-admin1@example.com", "manifest2-admin2@example.com");
  private static final int adminEmailsCount1 = manifestEmails1.size();
  private static final int adminEmailsCount2 = manifestEmails2.size();

  @BeforeEach
  public void setUp() {
    manifestSource1 = manifestFactory.newTrustedSource(manifestUrl1);
    manifestSource2 = manifestFactory.newTrustedSource(manifestUrl2);

    this.internet.putURL(manifestUrl1, getManifest1WithoutDuplicate());
    this.internet.putURL(manifestUrl2, getManifest2WithoutDuplicate());

    this.sourceProvider.addSource(manifestSource1);
    this.sourceProvider.addSource(manifestSource2);

    this.updater.onSourcesUpdated();
  }

  @Test
  public void testEmailsAreNotSentWhenThereAreManifestsWithoutDuplicates() {
    assertThat(this.internet.popEmailsSent()).isEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl1)).isEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl2)).isEmpty();
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

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl1)).isNotEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl2)).isNotEmpty();

    // Manifest 2 changes - now it will contain different major version and duplicates will be gone.
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

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl1)).isEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl2)).isEmpty();
  }

  @Test
  public void testDuplicateInfoIsRemovedAfterDuplicateManifestRemoval() {
    this.internet.putURL(manifestUrl1, getManifest1WithExternalDuplicate());
    this.internet.putURL(manifestUrl2, getManifest2WithExternalDuplicate());

    this.reloadManifest(manifestUrl1);
    this.reloadManifest(manifestUrl2);

    assertThat(this.internet.popEmailsSent()).hasSize(adminEmailsCount1 + adminEmailsCount2 + 1);

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl1)).isNotEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl2)).isNotEmpty();

    // Manifest 2 is removed - duplicates will be gone.
    this.sourceProvider.removeSource(manifestSource2);

    List<String> emailsSent = this.internet.popEmailsSent();
    assertThat(emailsSent).hasSize(adminEmailsCount1 + 1);
    this.checkEmail(emailsSent, manifestEmails1.get(0), duplicateRemovedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails1.get(1), duplicateRemovedSubject, manifestUrl1);
    this.checkEmail(emailsSent, adminEmails, adminEmailSubject,
        Collections.singletonList(manifestUrl1));

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl1)).isEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl2)).isEmpty();
  }

  @Test
  public void testManifestsWithDuplicatedEchoAndDiscoveryAreNotReported() {
    this.internet.putURL(manifestUrl1, getManifest1WithEchoAndDiscovery());
    this.internet.putURL(manifestUrl2, getManifest2WithEchoAndDiscovery());

    this.reloadManifest(manifestUrl1);
    this.reloadManifest(manifestUrl2);
    assertThat(this.internet.popEmailsSent()).isEmpty();

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl1)).isEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl2)).isEmpty();
  }

  @Test
  public void testManifestsWithDuplicatedEchoAndDiscoveryReportsOtherDuplicates() {
    this.internet.putURL(manifestUrl1, getManifest1WithEchoDiscoveryAndInstitutions());
    this.internet.putURL(manifestUrl2, getManifest2WithEchoDiscoveryAndInstitutions());

    this.reloadManifest(manifestUrl1);
    this.reloadManifest(manifestUrl2);

    assertThat(this.internet.popEmailsSent()).isNotEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl1)).isNotEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl2)).isNotEmpty();
  }

  @Test
  public void testManifestWithDuplicateInsideASingleHostIsReported() {
    this.internet.putURL(manifestUrl1, getManifest1WithDuplicateInsideASingleHost());
    this.reloadManifest(manifestUrl1);
    assertThat(this.internet.popEmailsSent()).isNotEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl1)).isNotEmpty();
  }

  @Test
  public void testManifestWithEchoDuplicateInsideASingleHostIsReported() {
    this.internet.putURL(manifestUrl1, getManifest1WithEchoDuplicateInsideASingleHost());
    this.reloadManifest(manifestUrl1);
    assertThat(this.internet.popEmailsSent()).isNotEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl1)).isNotEmpty();
  }

  @Test
  public void testEchoDuplicateInsideASingleHostAndInAnotherHost() {
    this.internet.putURL(manifestUrl1, getManifest1WithEchoDuplicateInsideASingleHost());
    this.internet.putURL(manifestUrl2, getManifest2WithEchoDiscoveryAndInstitutions());

    this.reloadManifest(manifestUrl1);
    this.reloadManifest(manifestUrl2);

    List<String> emailsSent = this.internet.popEmailsSent();
    assertThat(emailsSent).hasSize(adminEmailsCount1 + 1);
    this.checkEmail(emailsSent, manifestEmails1.get(0), duplicateDetectedSubject, manifestUrl1);
    this.checkEmail(emailsSent, manifestEmails1.get(1), duplicateDetectedSubject, manifestUrl1);
    this.checkEmail(emailsSent, adminEmails, adminEmailSubject, manifestUrl1);
    assertThat(emailsSent).allMatch(s -> !s.contains(manifestUrl2));

    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl1)).isNotEmpty();
    assertThat(this.manifestsNotifiedAboutDuplicatesRepository.findById(manifestUrl2)).isEmpty();
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

  private byte[] getManifest1WithoutDuplicate() {
    return this.getFile("manifestoverview/manifest1-no-duplicates.xml");
  }

  private byte[] getManifest1WithExternalDuplicate() {
    return this.getFile("manifestoverview/manifest1-external-duplicates.xml");
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

  private byte[] getManifest1WithEchoAndDiscovery() {
    return this.getFile("manifestoverview/manifest1-with-echo-and-discovery.xml");
  }

  private byte[] getManifest2WithEchoAndDiscovery() {
    return this.getFile("manifestoverview/manifest2-with-echo-and-discovery.xml");
  }

  private byte[] getManifest1WithEchoDiscoveryAndInstitutions() {
    return this.getFile("manifestoverview/manifest1-with-echo-discovery-and-institutions.xml");
  }

  private byte[] getManifest2WithEchoDiscoveryAndInstitutions() {
    return this.getFile("manifestoverview/manifest2-with-echo-discovery-and-institutions.xml");
  }

  private byte[] getManifest1WithDuplicateInsideASingleHost() {
    return this.getFile("manifestoverview/manifest1-with-duplicate-inside-a-single-host.xml");
  }

  private byte[] getManifest1WithEchoDuplicateInsideASingleHost() {
    return this.getFile("manifestoverview/manifest1-with-echo-duplicate-inside-a-single-host.xml");
  }

}
