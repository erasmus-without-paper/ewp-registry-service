package eu.erasmuswithoutpaper.registry.cmatrix;

import static org.joox.JOOX.$;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.w3c.dom.Element;

class ApiCnrSendsCell extends ApiEntriesCell {

  ApiCnrSendsCell(int colorClass, RegistryClient client, HeiEntry hei,
      KnownElement... apiEntryClasses) {
    super(colorClass, client, hei, apiEntryClasses);

    for (int i = 0; i < this.matchedApiEntries.size(); i++) {
      Element apiEntry = this.matchedApiEntries.get(i);

      ContentLine line = new ContentLine();
      line.addClass("ewpst__yesNo");
      line.addClass("ewpst__yesNo--cnrSends");
      boolean sends = $(apiEntry).child("sends-notifications").size() > 1;
      if (sends) {
        line.setContent("yes");
        line.addClass("ewpst__yesNo--yes");
        line.addTooltipLine("Matching \"sends-notifications\" element was found, so this "
            + "HEI states that it will send proper notifications to all CNR APIs.");
      } else {
        line.setContent("no");
        line.addClass("ewpst__yesNo--no");
        line.addTooltipLine("No matching \"sends-notifications\" element was found.");
      }
      this.addContentLine(line);
    }

    if (this.matchedApiEntries.size() == 0) {
      this.addTooltipLine("No matching API entry found.");
    }
  }

}
