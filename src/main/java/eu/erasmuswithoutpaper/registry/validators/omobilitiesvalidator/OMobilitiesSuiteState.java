package eu.erasmuswithoutpaper.registry.validators.omobilitiesvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
public class OMobilitiesSuiteState extends SuiteState {
  public String sendingHeiId;
  public String receivingHeiId;
  public String omobilityId;
  public int maxOmobilityIds;
  public String notPermittedHeiId;
  public String receivingAcademicYearId;

  public OMobilitiesSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
