package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.Objects;

public class ApiForHeiKey {
  private String heiId;
  private String apiName;

  public String getHeiId() {
    return heiId;
  }

  public String getApiName() {
    return apiName;
  }


  public ApiForHeiKey(String heiId, String apiName) {
    this.heiId = heiId;
    this.apiName = apiName;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    ApiForHeiKey that = (ApiForHeiKey) other;
    return Objects.equals(heiId, that.heiId) && Objects.equals(apiName, that.apiName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(heiId, apiName);
  }

}
