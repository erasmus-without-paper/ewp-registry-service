package eu.erasmuswithoutpaper.registry.cmatrix;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.w3c.dom.Element;

class OMobilityUpdateTypesCell extends ApiEntriesCell {

  OMobilityUpdateTypesCell(int colorClass, RegistryClient client, HeiEntry hei,
      KnownElement... apiEntryClasses) {
    super(colorClass, client, hei, apiEntryClasses);
    for (int i = 0; i < this.matchedApiEntries.size(); i++) {
      Element apiEntry = this.matchedApiEntries.get(i);
      ContentLine line = new OMobilityUpdateTypesContentLine(apiEntry);
      this.addContentLine(line);
    }
  }
}
