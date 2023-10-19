package eu.erasmuswithoutpaper.registry.cmatrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;
import org.springframework.stereotype.Service;

/**
 * This service allows you generate HEI/API coverage reports.
 */
@Service
public class CoverageMatrixGenerator {

  public static class HeiComparator implements Comparator<HeiEntry>, Serializable {
    private static final long serialVersionUID = -7592792474068837484L;

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
      List<String> heiIds = Arrays.asList(hei.getId().split("\\."));
      Collections.reverse(heiIds);
      return heiIds;
    }
  }

  /**
   * Given an initialized {@link RegistryClient}, generate HTML report describing the HEI/API
   * coverage.
   *
   * @param client    The client to fetch HEI/API data from.
   * @param heiFilter An optional pattern that should be used to filter HEIs
   * @return HTML string with the report.
   */
  public String generateToHtmlTable(RegistryClient client, String heiFilter) {
    List<HeiEntry> heis = this.extractInterestingHeis(client);
    if (heiFilter != null) {
      heis =
          heis.stream().filter(Utils.getHeiFilterPredicate(heiFilter)).collect(Collectors.toList());
    }

    List<CoverageMatrixRow> rows = new ArrayList<>();
    for (int i = 0; i < heis.size(); i++) {
      HeiEntry hei = heis.get(i);
      rows.add(new CoverageMatrixRow(hei, client, i + 1));
    }
    StringBuilder sb = new StringBuilder();
    sb.append("<div class='ewpst'>");
    sb.append("<table class='ewpst__table'>");
    for (CoverageMatrixRow row : rows) {
      row.generateHtmlRow(sb);
    }
    // Header moved after table body to work around sticky-opacity bug in browsers
    sb.append(CoverageMatrixRow.generateTableHeader());
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
