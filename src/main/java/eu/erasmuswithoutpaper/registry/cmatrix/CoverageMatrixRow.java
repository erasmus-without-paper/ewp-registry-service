package eu.erasmuswithoutpaper.registry.cmatrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

class CoverageMatrixRow {

  private static String genRow(String text, int colorClass) {
    return genRow(text, colorClass, 1, 1, "");
  }

  private static String genRow(String text, int colorClass, int rowspan, int colspan) {
    return genRow(text, colorClass, rowspan, colspan, "");
  }

  private static String genRow(String text, int colorClass, int rowspan, int colspan,
      String classes) {
    StringBuilder sb = new StringBuilder();
    sb.append("<th ");
    if (colspan != 1) {
      sb.append("colspan='");
      sb.append(colspan);
      sb.append('\'');
    }
    if (rowspan != 1) {
      sb.append("rowspan='");
      sb.append(rowspan);
      sb.append('\'');
    }
    sb.append("class='ewpst__cell ewpst__cell--cc");
    sb.append(colorClass);
    if (!classes.isEmpty()) {
      sb.append(' ');
      sb.append(classes);
    }
    sb.append("'>");
    sb.append(text);
    sb.append("</th>");
    return sb.toString();
  }

  static void generateHtmlTableHeader(StringBuilder sb) {

    StringBuilder row1 = new StringBuilder();
    StringBuilder row2 = new StringBuilder();
    StringBuilder row3 = new StringBuilder();
    row1.append("<tr class='ewpst__row ewpst__row--header'>");
    row2.append("<tr class='ewpst__row ewpst__row--header'>");
    row3.append("<tr class='ewpst__row ewpst__row--header'>");

    /* Row index */

    row1.append(genRow("#", NAME_COLOR_CLASS, 3, 1));

    /* Institution */

    row1.append(genRow("Institution", NAME_COLOR_CLASS, 3, 1, "ewpst__cell--institution"));

    /* SCHAC */

    row1.append(genRow("SCHAC", NAME_COLOR_CLASS, 3, 1,
            "ewpst__cell-schac ewpst__cell--institution"));

    /* Erasmus code */

    row1.append(genRow("Erasmus code", NAME_COLOR_CLASS, 3, 1,
            "ewpst__cell-schac ewpst__cell--institution"));

    /* Primary Network APIs */

    int colorClass = getNextColorClass(NAME_COLOR_CLASS);

    row1.append(genRow("Primary Network APIs", colorClass, 1, 2));
    row2.append(genRow("discov.", colorClass, 2, 1));
    row2.append(genRow("echo", colorClass, 2, 1));

    /* General Purpose APIs */

    colorClass = getNextColorClass(colorClass);

    row1.append(genRow("General Purpose APIs", colorClass, 1, 4));
    row2.append(genRow("inst.", colorClass, 2, 1));
    row2.append(genRow("ounits", colorClass, 2, 1));
    row2.append(genRow("courses", colorClass, 2, 1));
    row2.append(genRow("course replic.", colorClass, 2, 1));

    /* IIAs */
    colorClass = getNextColorClass(colorClass);

    row1.append(genRow("IIAs", colorClass, 1, 4));
    row2.append(genRow("ver.", colorClass, 2, 1));
    row2.append(genRow("CNR", colorClass, 1, 2));
    row3.append(genRow("sends", colorClass));
    row3.append(genRow("recv.", colorClass));
    row2.append(genRow("fact.", colorClass, 2, 1));

    /* IIAs Approval */
    colorClass = getNextColorClass(colorClass);

    row1.append(genRow("IIAs Approval", colorClass, 1, 3));
    row2.append(genRow("ver.", colorClass, 2, 1));
    row2.append(genRow("CNR", colorClass, 1, 2));
    row3.append(genRow("sends", colorClass));
    row3.append(genRow("recv.", colorClass));

    /* OMobilities */
    colorClass = getNextColorClass(colorClass);

    row1.append(genRow("OMobilities", colorClass, 1, 3));
    row2.append(genRow("ver.", colorClass, 2, 1));
    row2.append(genRow("CNR", colorClass, 1, 2));
    row3.append(genRow("sends", colorClass));
    row3.append(genRow("recv.", colorClass));

    /* OMobility LAs */

    colorClass = getNextColorClass(colorClass);

    row1.append(genRow("OMobility LAs", colorClass, 1, 4));
    row2.append(genRow("ver.", colorClass, 2, 1));
    row2.append(genRow("update types", colorClass, 2, 1));
    row2.append(genRow("CNR", colorClass, 1, 2));
    row3.append(genRow("sends", colorClass));
    row3.append(genRow("recv.", colorClass));

    /* IMobilities */

    colorClass = getNextColorClass(colorClass);

    row1.append(genRow("IMobilities", colorClass, 1, 3));
    row2.append(genRow("ver.", colorClass, 2, 1));
    row2.append(genRow("CNR", colorClass, 1, 2));
    row3.append(genRow("sends", colorClass));
    row3.append(genRow("recv.", colorClass));

    /* IMobility ToRs */

    colorClass = getNextColorClass(colorClass);

    row1.append(genRow("IMobility ToRs", colorClass, 1, 3));
    row2.append(genRow("ver.", colorClass, 2, 1));
    row2.append(genRow("CNR", colorClass, 1, 2));
    row3.append(genRow("sends", colorClass));
    row3.append(genRow("recv.", colorClass));

    /* Other APIs */

    row1.append(genRow("Other APIs", OTHER_APIS_COLOR_CLASS, 3, 1));

    /* Finalize */

    row1.append("</tr>");
    row2.append("</tr>");
    row3.append("</tr>");
    sb.append(row1.toString());
    sb.append(row2.toString());
    sb.append(row3.toString());
  }

  private static final int NAME_COLOR_CLASS = 1;
  private static final int EVEN_COLOR_CLASS = 2;
  private static final int ODD_COLOR_CLASS = 3;
  private static final int OTHER_APIS_COLOR_CLASS = 4;

  private final List<CoverageMatrixCell> cells;
  private final OtherApisCell otherApisCell;

  CoverageMatrixRow(HeiEntry hei, RegistryClient client, int rowIndex) {
    this.cells = new ArrayList<>();

    CoverageMatrixCell cell;

    /* Row index */

    cell = new CoverageMatrixCell(NAME_COLOR_CLASS);
    this.cells.add(cell);
    cell.addContentLine(String.valueOf(rowIndex));

    /* Institution */

    cell = new CoverageMatrixCell(NAME_COLOR_CLASS);
    this.cells.add(cell);
    cell.addContentLine(hei.getName());

    /* SCHAC */

    cell = new CoverageMatrixCell(NAME_COLOR_CLASS);
    cell.addClass("ewpst__cell-schac");
    this.cells.add(cell);
    cell.addContentLine(hei.getId());

    /* Erasmus code */

    cell = new CoverageMatrixCell(NAME_COLOR_CLASS);
    cell.addClass("ewpst__cell-schac");
    this.cells.add(cell);
    Collection<String> erasmusCodes = hei.getOtherIds("erasmus");
    Optional<String> erasmusCode = erasmusCodes.stream().findFirst();
    cell.addContentLine(erasmusCode.orElse(""));

    /* Primary Network APIs */
    int colorClass = getNextColorClass(NAME_COLOR_CLASS);

    // discov.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_DISCOVERY_V4,
            KnownElement.APIENTRY_DISCOVERY_V5);
    this.cells.add(cell);

    // echo
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_ECHO_V1,
            KnownElement.APIENTRY_ECHO_V2);
    this.cells.add(cell);

    /* General Purpose APIs */

    colorClass = getNextColorClass(colorClass);

    // inst.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_INSTITUTIONS_V1,
        KnownElement.APIENTRY_INSTITUTIONS_V2);
    this.cells.add(cell);

    // ounits
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_OUNITS_V1,
        KnownElement.APIENTRY_OUNITS_V2);
    this.cells.add(cell);

    // courses
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_COURSES_V1);
    this.cells.add(cell);

    // course replic.
    cell = new ApiVersionsCell(colorClass, client, hei,
        KnownElement.APIENTRY_COURSE_REPLICATION_V1);
    this.cells.add(cell);

    /* IIAs */

    colorClass = getNextColorClass(colorClass);

    // ver.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_IIAS_V1,
        KnownElement.APIENTRY_IIAS_V2, KnownElement.APIENTRY_IIAS_V3,
        KnownElement.APIENTRY_IIAS_V4, KnownElement.APIENTRY_IIAS_V5,
        KnownElement.APIENTRY_IIAS_V6);
    this.cells.add(cell);

    // CNR sends
    cell = new ApiCnrSendsCell(colorClass, client, hei, KnownElement.APIENTRY_IIAS_V1,
        KnownElement.APIENTRY_IIAS_V2, KnownElement.APIENTRY_IIAS_V3,
        KnownElement.APIENTRY_IIAS_V4);
    this.cells.add(cell);

    // CNR recv.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_IIA_CNR_V1,
        KnownElement.APIENTRY_IIA_CNR_V2);
    this.cells.add(cell);

    // Factsheet
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_FACTSHEET_V1);
    this.cells.add(cell);

    /* IIAs Approval */

    colorClass = getNextColorClass(colorClass);

    // ver.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_IIAS_APPROVAL_V1);
    this.cells.add(cell);

    // CNR sends
    cell = new ApiCnrSendsCell(colorClass, client, hei, KnownElement.APIENTRY_IIAS_APPROVAL_V1);
    this.cells.add(cell);

    // CNR recv.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_IIA_APPROVAL_CNR_V1);
    this.cells.add(cell);

    /* OMobilities */

    colorClass = getNextColorClass(colorClass);

    // ver.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_OMOBILITIES_V1);
    this.cells.add(cell);

    // CNR sends
    cell = new ApiCnrSendsCell(colorClass, client, hei, KnownElement.APIENTRY_OMOBILITIES_V1);
    this.cells.add(cell);

    // CNR recv.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_OMOBILITY_CNR_V1);
    this.cells.add(cell);

    /* OMobility LAs */

    colorClass = getNextColorClass(colorClass);

    // ver.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_OMOBILITY_LAS_V1);
    this.cells.add(cell);

    // update types
    cell = new OMobilityUpdateTypesCell(colorClass, client, hei,
        KnownElement.APIENTRY_OMOBILITY_LAS_V1);
    this.cells.add(cell);

    // CNR sends
    cell = new ApiCnrSendsCell(colorClass, client, hei, KnownElement.APIENTRY_OMOBILITY_LAS_V1);
    this.cells.add(cell);

    // CNR recv.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_OMOBILITY_LA_CNR_V1);
    this.cells.add(cell);

    /* IMobilities */

    colorClass = getNextColorClass(colorClass);

    // ver.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_IMOBILITIES_V1);
    this.cells.add(cell);

    // CNR sends
    cell = new ApiCnrSendsCell(colorClass, client, hei, KnownElement.APIENTRY_IMOBILITIES_V1);
    this.cells.add(cell);

    // CNR recv.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_IMOBILITY_CNR_V1);
    this.cells.add(cell);

    /* IMobility ToRs */

    colorClass = getNextColorClass(colorClass);

    // ver.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_IMOBILITY_TORS_V1);
    this.cells.add(cell);

    // CNR sends
    cell = new ApiCnrSendsCell(colorClass, client, hei, KnownElement.APIENTRY_IMOBILITY_TORS_V1);
    this.cells.add(cell);

    // CNR recv.
    cell = new ApiVersionsCell(colorClass, client, hei, KnownElement.APIENTRY_IMOBILITY_TOR_CNR_V1);
    this.cells.add(cell);

    /* Other APIs */

    this.otherApisCell = new OtherApisCell(OTHER_APIS_COLOR_CLASS, client, hei);
    this.cells.add(this.otherApisCell);
  }

  private static int getNextColorClass(int prevClass) {
    if (prevClass == ODD_COLOR_CLASS) {
      return EVEN_COLOR_CLASS;
    }
    return ODD_COLOR_CLASS;
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
