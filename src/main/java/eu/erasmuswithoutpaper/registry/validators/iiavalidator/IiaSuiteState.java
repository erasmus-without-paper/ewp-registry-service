package eu.erasmuswithoutpaper.registry.validators.iiavalidator;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

public class IiaSuiteState extends SuiteState {
  public static class IiaInfo {
    public String heiId;
    public String partnerHeiId;
    public List<String> receivingAcademicYears = new ArrayList<>();
  }

  public int maxIiaIds;
  public int maxIiaCodes;
  public String selectedHeiId;
  public String selectedIiaId;
  public IiaInfo selectedIiaInfo = new IiaInfo();

  public IiaSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
