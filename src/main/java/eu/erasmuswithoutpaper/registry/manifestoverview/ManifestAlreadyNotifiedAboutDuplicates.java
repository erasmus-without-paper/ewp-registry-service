package eu.erasmuswithoutpaper.registry.manifestoverview;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;

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
