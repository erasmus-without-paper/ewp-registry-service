package eu.erasmuswithoutpaper.registry.manifestoverview;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "REG_MANIFESTS_ALREADY_NOTIFIED_ABOUT_DUPLICATES")
@ConditionalOnWebApplication
public class ManifestAlreadyNotifiedAboutDuplicates {
  @Id
  private String manifestUrl;

  /**
   * Needed for Hibernate. Don't use explicitly.
   */
  public ManifestAlreadyNotifiedAboutDuplicates() {
  }

  public ManifestAlreadyNotifiedAboutDuplicates(String manifestUrl) {
    this.manifestUrl = manifestUrl;
  }

  public String getManifestUrl() {
    return manifestUrl;
  }
}
