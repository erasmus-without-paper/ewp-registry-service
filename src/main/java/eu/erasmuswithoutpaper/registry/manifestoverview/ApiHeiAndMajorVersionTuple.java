package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.Objects;

public class ApiHeiAndMajorVersionTuple {
  private String heiId;
  private String apiName;
  private String majorVersion;

  public String getHeiId() {
    return heiId;
  }

  public String getApiName() {
    return apiName;
  }

  public String getMajorVersion() {
    return majorVersion;
  }

  /**
   * Creates a tuple with heiId, apiName and version.
   * @param heiId
   *      ID of the HEI.
   * @param apiName
   *      Name of the API.
   * @param version
   *      Version of the API.
   */
  public ApiHeiAndMajorVersionTuple(String heiId, String apiName, ApiVersion version) {
    this.heiId = heiId;
    this.apiName = apiName;
    if (!version.isValid()) {
      this.majorVersion = version.toString();
    } else {
      this.majorVersion = String.valueOf(version.getValidVersion().major);
    }
  }


  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    ApiHeiAndMajorVersionTuple that = (ApiHeiAndMajorVersionTuple) other;
    return Objects.equals(heiId, that.heiId)
        && Objects.equals(apiName, that.apiName)
        && Objects.equals(majorVersion, that.majorVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(heiId, apiName, majorVersion);
  }
}
