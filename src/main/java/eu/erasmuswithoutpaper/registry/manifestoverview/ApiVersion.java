package eu.erasmuswithoutpaper.registry.manifestoverview;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;

public class ApiVersion {
  private final boolean valid;
  private final boolean empty;
  private final SemanticVersion validVersion;
  private final String versionString;

  /**
   * Constructs new ApiVersion object from given string.
   * If string is empty, then empty is set to true.
   * If string contains invalid version, then valid is set to false.
   * Otherwise valid is set to true and validVersion contain parsed representation.
   *
   * @param version
   *     string to parse.
   */
  public ApiVersion(String version) {
    this.versionString = version;
    if (version.isEmpty()) {
      this.empty = true;
      this.valid = false;
      this.validVersion = null;
    } else {
      this.empty = false;
      SemanticVersion semanticVersion;
      try {
        semanticVersion = new SemanticVersion(version);
      } catch (SemanticVersion.InvalidVersionString invalidVersionString) {
        semanticVersion = null;
      }
      this.valid = semanticVersion != null;
      this.validVersion = semanticVersion;
    }
  }

  @Override
  public String toString() {
    if (this.empty) {
      return "<no version>";
    }
    if (!this.valid) {
      return "<invalid: " + this.versionString + ">";
    }
    return this.validVersion.toString();
  }

  public SemanticVersion getValidVersion() {
    return validVersion;
  }

  public boolean isValid() {
    return this.valid;
  }
}
