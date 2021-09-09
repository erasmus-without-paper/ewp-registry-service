package eu.erasmuswithoutpaper.registry.validators.omobilitylasvalidator;

import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.SuiteState;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD")
public class OMobilityLAsSuiteState extends SuiteState {
  public String sendingHeiId;
  public String receivingHeiId;
  public String omobilityId;
  public int maxOmobilityIds;
  public String notPermittedHeiId;
  public String receivingAcademicYearId;

  public String changesProposalId;

  public OMobilityLAsSuiteState(String url, SemanticVersion version) {
    super(url, version);
  }
}
