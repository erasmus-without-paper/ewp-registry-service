package eu.erasmuswithoutpaper.registry.cmatrix;

import static org.joox.JOOX.$;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.w3c.dom.Element;

class ApiVersionsCell extends ApiEntriesCell {

  ApiVersionsCell(int colorClass, RegistryClient client, HeiEntry hei,
      KnownElement... apiEntryClasses) {
    super(colorClass, client, hei, apiEntryClasses);

    for (int i = 0; i < this.matchedApiEntries.size(); i++) {
      Element apiEntry = this.matchedApiEntries.get(i);
      ContentLine line = new ContentLine($(apiEntry).attr("version"));
      line.addClass("ewpst__apiVersion");
      if (i == 0) {
        line.addClass("ewpst__apiVersion--first");
      }
      if (this.lastClass.matches(apiEntry)) {
        line.addClass("ewpst__apiVersion--upToDate");
      } else {
        line.addClass("ewpst__apiVersion--obsolete");
        line.addTooltipLine("This major version of this API is obsolete or deprecated.");
      }
      this.addContentLine(line);
    }
  }

}
