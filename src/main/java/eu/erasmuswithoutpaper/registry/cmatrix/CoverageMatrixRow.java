package eu.erasmuswithoutpaper.registry.cmatrix;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

class CoverageMatrixRow {

  static void generateHtmlTableHeader(StringBuilder sb) {

    StringBuilder row1 = new StringBuilder();
    StringBuilder row2 = new StringBuilder();
    StringBuilder row3 = new StringBuilder();
    row1.append("<tr class='ewpst__row ewpst__row--header'>");
    row2.append("<tr class='ewpst__row ewpst__row--header'>");
    row3.append("<tr class='ewpst__row ewpst__row--header'>");

    /* Institution */

    row1.append("<th rowspan='3' class='ewpst__cell ewpst__cell--cc1'>Institution</th>");

    /* General Purpose APIs */

    row1.append("<th colspan='4' class='ewpst__cell ewpst__cell--cc3'>General Purpose APIs</th>");
    row2.append("<th rowspan='2' class='ewpst__cell ewpst__cell--cc3'>inst.</th>");
    row2.append("<th rowspan='2' class='ewpst__cell ewpst__cell--cc3'>ounits</th>");
    row2.append("<th rowspan='2' class='ewpst__cell ewpst__cell--cc3'>courses</th>");
    row2.append("<th rowspan='2' class='ewpst__cell ewpst__cell--cc3'>course replic.</th>");

    /* IIAs */

    row1.append("<th colspan='3' class='ewpst__cell ewpst__cell--cc2'>IIAs</th>");
    row2.append("<th rowspan='2' class='ewpst__cell ewpst__cell--cc2'>ver.</th>");
    row2.append("<th colspan='2' class='ewpst__cell ewpst__cell--cc2'>CNR</th>");
    row3.append("<th class='ewpst__cell ewpst__cell--cc2'>sends</th>");
    row3.append("<th class='ewpst__cell ewpst__cell--cc2'>recv.</th>");

    /* OMobilities */

    row1.append("<th colspan='4' class='ewpst__cell ewpst__cell--cc3'>OMobilities</th>");
    row2.append("<th rowspan='2' class='ewpst__cell ewpst__cell--cc3'>ver.</th>");
    row2.append("<th rowspan='2' class='ewpst__cell ewpst__cell--cc3'>update types</th>");
    row2.append("<th colspan='2' class='ewpst__cell ewpst__cell--cc3'>CNR</th>");
    row3.append("<th class='ewpst__cell ewpst__cell--cc3'>sends</th>");
    row3.append("<th class='ewpst__cell ewpst__cell--cc3'>recv.</th>");

    /* IMobilities */

    row1.append("<th colspan='3' class='ewpst__cell ewpst__cell--cc2'>IMobilities</th>");
    row2.append("<th rowspan='2' class='ewpst__cell ewpst__cell--cc2'>ver.</th>");
    row2.append("<th colspan='2' class='ewpst__cell ewpst__cell--cc2'>CNR</th>");
    row3.append("<th class='ewpst__cell ewpst__cell--cc2'>sends</th>");
    row3.append("<th class='ewpst__cell ewpst__cell--cc2'>recv.</th>");

    /* IMobility ToRs */

    row1.append("<th colspan='3' class='ewpst__cell ewpst__cell--cc3'>IMobility ToRs</th>");
    row2.append("<th rowspan='2' class='ewpst__cell ewpst__cell--cc3'>ver.</th>");
    row2.append("<th colspan='2' class='ewpst__cell ewpst__cell--cc3'>CNR</th>");
    row3.append("<th class='ewpst__cell ewpst__cell--cc3'>sends</th>");
    row3.append("<th class='ewpst__cell ewpst__cell--cc3'>recv.</th>");

    /* Other APIs */

    row1.append("<th rowspan='3' class='ewpst__cell ewpst__cell--cc4'>Other APIs</th>");

    /* Finalize */

    row1.append("</tr>");
    row2.append("</tr>");
    row3.append("</tr>");
    sb.append(row1.toString());
    sb.append(row2.toString());
    sb.append(row3.toString());
  }

  private final HeiEntry hei;
  private final List<CoverageMatrixCell> cells;
  private final OtherApisCell otherApisCell;

  CoverageMatrixRow(HeiEntry hei, RegistryClient client) {
    this.hei = hei;
    this.cells = new ArrayList<>();

    CoverageMatrixCell cell;

    /* Institution */

    cell = new CoverageMatrixCell(1);
    this.cells.add(cell);
    cell.addContentLine(this.hei.getId());

    /* General Purpose APIs */

    // inst.
    cell = new ApiVersionsCell(3, client, hei, KnownElement.APIENTRY_INSTITUTIONS_V1,
        KnownElement.APIENTRY_INSTITUTIONS_V2);
    this.cells.add(cell);

    // ounits
    cell = new ApiVersionsCell(3, client, hei, KnownElement.APIENTRY_OUNITS_V1,
        KnownElement.APIENTRY_OUNITS_V2);
    this.cells.add(cell);

    // courses
    cell = new ApiVersionsCell(3, client, hei, KnownElement.APIENTRY_COURSES_V1);
    this.cells.add(cell);

    // course replic.
    cell = new ApiVersionsCell(3, client, hei, KnownElement.APIENTRY_COURSE_REPLICATION_V1);
    this.cells.add(cell);

    /* IIAs */

    // ver. + CNR sends
    cell = new ApiVersionsCell(2, client, hei, KnownElement.APIENTRY_IIAS_V1,
        KnownElement.APIENTRY_IIAS_V2);
    this.cells.add(cell);

    // CNR sends
    cell = new ApiCnrSendsCell(2, client, hei, KnownElement.APIENTRY_IIAS_V1,
        KnownElement.APIENTRY_IIAS_V2);
    this.cells.add(cell);

    // CNR recv.
    cell = new ApiVersionsCell(2, client, hei, KnownElement.APIENTRY_IIA_CNR_V1,
        KnownElement.APIENTRY_IIA_CNR_V2);
    this.cells.add(cell);

    /* OMobilities */

    // ver.
    cell = new ApiVersionsCell(3, client, hei, KnownElement.APIENTRY_OMOBILITIES_V1);
    this.cells.add(cell);

    // update types
    cell = new OMobilityUpdateTypesCell(3, client, hei, KnownElement.APIENTRY_OMOBILITIES_V1);
    this.cells.add(cell);

    // CNR sends
    cell = new ApiCnrSendsCell(3, client, hei, KnownElement.APIENTRY_OMOBILITIES_V1);
    this.cells.add(cell);

    // CNR recv.
    cell = new ApiVersionsCell(3, client, hei, KnownElement.APIENTRY_OMOBILITY_CNR_V1);
    this.cells.add(cell);

    /* IMobilities */

    // ver.
    cell = new ApiVersionsCell(2, client, hei, KnownElement.APIENTRY_IMOBILITIES_V1);
    this.cells.add(cell);

    // CNR sends
    cell = new ApiCnrSendsCell(2, client, hei, KnownElement.APIENTRY_IMOBILITIES_V1);
    this.cells.add(cell);

    // CNR recv.
    cell = new ApiVersionsCell(2, client, hei, KnownElement.APIENTRY_IMOBILITY_CNR_V1);
    this.cells.add(cell);

    /* IMobility ToRs */

    // ver.
    cell = new ApiVersionsCell(3, client, hei, KnownElement.APIENTRY_IMOBILITY_TORS_V1);
    this.cells.add(cell);

    // CNR sends
    cell = new ApiCnrSendsCell(3, client, hei, KnownElement.APIENTRY_IMOBILITY_TORS_V1);
    this.cells.add(cell);

    // CNR recv.
    cell = new ApiVersionsCell(3, client, hei, KnownElement.APIENTRY_IMOBILITY_TOR_CNR_V1);
    this.cells.add(cell);

    /* Other APIs */

    this.otherApisCell = new OtherApisCell(4, client, hei);
    this.cells.add(this.otherApisCell);
  }

  void generateHtmlRow(StringBuilder sb) {
    sb.append("<tr class='ewpst_row'>");

    for (CoverageMatrixCell cell : this.cells) {
      cell.renderHtmlCell(sb);
    }

    sb.append("</tr>");
  }

  OtherApisCell getOtherApisCell() {
    return this.otherApisCell;
  }
}
