package eu.erasmuswithoutpaper.registry.validators;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SemanticVersion implements Comparable<SemanticVersion> {
  public final int major;
  public final int minor;
  public final int patch;

  /**
   * Creates Semantic version.
   */
  public SemanticVersion(int major, int minor, int patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
  }

  /**
   * Parse version string to SemanticVersion.
   *
   * @param version String to parse, should have following format: "%d.%d.%d"
   * @throws InvalidVersionString version doesn't conform to expected format.
   */
  public SemanticVersion(String version) throws InvalidVersionString {
    String patternString = "(\\d+)\\.(\\d+)\\.(\\d+)";
    Pattern pattern = Pattern.compile(patternString);
    Matcher matcher = pattern.matcher(version);
    if (!matcher.matches()) {
      throw new InvalidVersionString();
    }
    try {
      major = Integer.parseInt(matcher.group(1));
      minor = Integer.parseInt(matcher.group(2));
      patch = Integer.parseInt(matcher.group(3));
    } catch (NumberFormatException unused) {
      throw new InvalidVersionString();
    }
  }

  /**
   * Check if this version is compatible with other. Version is compatible with other version if: -
   * this.major == other major, and - this.minor > other.minor, or - this.minor == other.minor and
   * this.patch >= other.patch.
   *
   * @param other
   *         version to check against.
   * @return true if this is compatible with other.
   */
  public boolean isCompatible(SemanticVersion other) {
    boolean majorsMatch = major == other.major;
    boolean patchesMatch = minor > other.minor || (minor == other.minor && patch >= other.patch);
    return majorsMatch && patchesMatch;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    SemanticVersion that = (SemanticVersion) other;
    return major == that.major && minor == that.minor && patch == that.patch;
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch);
  }

  @Override
  public int compareTo(SemanticVersion other) {
    if (major != other.major) {
      return major - other.major;
    }
    if (minor != other.minor) {
      return minor - other.minor;
    }
    return patch - other.patch;
  }

  public static class InvalidVersionString extends Exception {}
}
