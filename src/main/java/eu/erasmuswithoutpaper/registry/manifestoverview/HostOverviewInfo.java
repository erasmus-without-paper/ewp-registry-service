package eu.erasmuswithoutpaper.registry.manifestoverview;

import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public class HostOverviewInfo {
  public List<String> adminEmails = new ArrayList<>();
  public String adminProvider;
  public List<String> adminNotes = new ArrayList<>();
  public List<ImplementedApiInfo> apisImplemented = new ArrayList<>();
  public List<String> coveredHeiIds = new ArrayList<>();
}
