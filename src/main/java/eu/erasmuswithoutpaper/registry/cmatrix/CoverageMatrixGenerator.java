package eu.erasmuswithoutpaper.registry.cmatrix;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This service allows you generate HEI/API coverage reports.
 */
@Service
public class CoverageMatrixGenerator {

  @SuppressFBWarnings("SE_COMPARATOR_SHOULD_BE_SERIALIZABLE")
  private static class HeiComparator implements Comparator<HeiEntry> {

    @Override
    public int compare(HeiEntry e1, HeiEntry e2) {
      List<String> p1 = this.extractSlug(e1);
      List<String> p2 = this.extractSlug(e2);
      for (int i = 0; i < p1.size() && i < p2.size(); i++) {
        int result = p1.get(i).compareTo(p2.get(i));
        if (result != 0) {
          return result;
        }
      }
      Integer s1 = p1.size();
      Integer s2 = p2.size();
      return s1.compareTo(s2);
    }

    private List<String> extractSlug(HeiEntry hei) {
      return Lists.reverse((Arrays.asList(hei.getId().split("\\."))));
    }
  }

  /**
   * Given an initialized {@link RegistryClient}, generate HTML report describing the HEI/API
   * coverage.
   *
   * @param client The client to fetch HEI/API data from.
   * @return HTML string with the report.
   */
  public String generateToHtmlTable(RegistryClient client) {
    List<HeiEntry> heis = this.extractInterestingHeis(client);
    List<CoverageMatrixRow> rows = new ArrayList<>();
    for (int i = 0; i < heis.size(); i++) {
      HeiEntry hei = heis.get(i);
      rows.add(new CoverageMatrixRow(hei, client, i + 1));
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<div class='ewpst'>");
    sb.append("<table class='ewpst__table'>");
    CoverageMatrixRow.generateHtmlTableHeader(sb);
    for (CoverageMatrixRow row : rows) {
      row.generateHtmlRow(sb);
    }
    sb.append("</table>");
    sb.append("<div class='ewpst__footnotes'>");
    for (CoverageMatrixRow row : rows) {
      OtherApisCell cell = row.getOtherApisCell();
      cell.renderHtmlFootnote(sb);
    }
    sb.append("</div>");
    sb.append("</div>");
    return sb.toString();
  }

  private List<HeiEntry> extractInterestingHeis(RegistryClient client) {

    /* Find all HEIs which implement at least one stable EWP API. */

    ApiSearchConditions conds = new ApiSearchConditions();
    conds.setMinVersionRequired("1.0.0");
    List<HeiEntry> heis = new ArrayList<>(client.findHeis(conds));

    /* Order them by "reversed domain". */

    heis.sort(new HeiComparator());
    return heis;
  }
}
