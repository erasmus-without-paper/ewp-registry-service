package eu.erasmuswithoutpaper.registry.validators;

import java.util.Objects;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.constraints.NotNull;

public class SemanticVersion implements Comparable<SemanticVersion> {
  private static String patternString = "v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-rc(\\d))?";
  private static Pattern pattern = Pattern.compile(patternString);
  public final int major;
  public final int minor;
  public final int patch;
  public final OptionalInt releaseCandidate;

  /**
   * Creates Semantic Version.
   * @param major Major number.
   * @param minor Minor number.
   * @param patch Patch number.
   */
  public SemanticVersion(int major, int minor, int patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.releaseCandidate = OptionalInt.empty();
  }

  /**
   * Creates Semantic Version with release candidate.
   * @param major Major number.
   * @param minor Minor number.
   * @param patch Patch number.
   * @param releaseCandidate Release Candidate number.
   */
  public SemanticVersion(int major, int minor, int patch, int releaseCandidate) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.releaseCandidate = OptionalInt.of(releaseCandidate);
  }

  /**
   * Parse version string to SemanticVersion.
   *
   * @param version
   *     String to parse, should have one of following formats:
   *     <ul>
   *     <li>"%d.%d.%d"</li>
   *     <li>"%d.%d.%d-rc%d"</li>
   *     <li>"v%d.%d.%d"</li>
   *     <li>"v%d.%d.%d-rc%d"</li>
   *     </ul>
   * @throws InvalidVersionString
   *     version doesn't conform to any expected format.
   */
  public SemanticVersion(String version) throws InvalidVersionString {
    Matcher matcher = pattern.matcher(version);
    if (!matcher.matches()) {
      throw new InvalidVersionString();
    }
    try {
      major = Integer.parseInt(matcher.group(1));
      minor = Integer.parseInt(matcher.group(2));
      patch = Integer.parseInt(matcher.group(3));
      String matchedReleaseCandidateString = matcher.group(4);
      if (matchedReleaseCandidateString == null) {
        releaseCandidate = OptionalInt.empty();
      } else {
        releaseCandidate = OptionalInt.of(Integer.parseInt(matchedReleaseCandidateString));
      }
    } catch (NumberFormatException unused) {
      throw new InvalidVersionString();
    }
  }

  /**
   * Check if this version is compatible with other. Version is compatible with other version if:
   * <ul>
   * <li>this.major == other major, and</li>
   * <li>this.minor {@literal >} other.minor, or</li>
   * <li><ul>
   * <li>this.minor == other.minor and</li>
   * <li>this.patch {@literal >}= other.patch.</li>
   * </ul></li>
   * </ul>
   * If this or other version is a release candidate only exact match is consider compatible.
   *
   * @param other
   *     version to check against.
   * @return true if this is compatible with other.
   */
  public boolean isCompatible(SemanticVersion other) {
    if (this.isReleaseCandidate() || other.isReleaseCandidate()) {
      return this.equals(other);
    }
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
    return major == that.major
        && minor == that.minor
        && patch == that.patch
        && this.releaseCandidate.equals(that.releaseCandidate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch, releaseCandidate);
  }

  @Override
  public int compareTo(@NotNull SemanticVersion other) {
    if (this.major != other.major) {
      return this.major - other.major;
    }
    if (this.minor != other.minor) {
      return this.minor - other.minor;
    }
    if (this.patch != other.patch) {
      return this.patch - other.patch;
    }
    //RC versions come before non-RCs.
    if (this.isReleaseCandidate() && !other.isReleaseCandidate()) {
      return -1;
    }
    if (other.isReleaseCandidate() && !this.isReleaseCandidate()) {
      return 1;
    }
    if (other.isReleaseCandidate() && this.isReleaseCandidate()) {
      return this.releaseCandidate.getAsInt() - other.releaseCandidate.getAsInt();
    }
    return 0;
  }

  @Override
  public String toString() {
    String version = String.join(
        ".", Integer.toString(major), Integer.toString(minor), Integer.toString(patch)
    );
    String rc = "";
    if (this.isReleaseCandidate()) {
      rc = "-rc" + this.releaseCandidate;
    }
    return version + rc;
  }

  public boolean isReleaseCandidate() {
    return this.releaseCandidate.isPresent();
  }

  public static class InvalidVersionString extends Exception {}
}
