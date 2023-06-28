package eu.erasmuswithoutpaper.registry.cmatrix;

import static org.joox.JOOX.$;

import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.constraints.VerifyApiVersions;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

class ApiVersionsCell extends ApiEntriesCell {

  ApiVersionsCell(int colorClass, RegistryClient client, HeiEntry hei, boolean hasStatsEndpoint,
      KnownElement... apiEntryClasses) {
    super(colorClass, client, hei, apiEntryClasses);

    for (int i = 0; i < this.matchedApiEntries.size(); i++) {
      final Element apiEntry = this.matchedApiEntries.get(i);
      final String version = $(apiEntry).attr("version");
      final String apiNamespace = $(apiEntry).namespaceURI();
      final ContentLine line = new ContentLine(version);
      List<String> expectedVersionPrefixes = VerifyApiVersions.getExpectedVersionPrefixes(
          apiNamespace
      );
      boolean namespaceAndVersionMatch = expectedVersionPrefixes.isEmpty()
          || expectedVersionPrefixes.stream().anyMatch(version::startsWith);
      boolean statsEndpointNotImplemented =
          hasStatsEndpoint && !isStatsEndpointImplemented(apiEntry);

      line.addClass("ewpst__apiVersion");
      if (i == 0) {
        line.addClass("ewpst__apiVersion--first");
      }

      if (namespaceAndVersionMatch) {
        if (this.lastClass.matches(apiEntry) && !statsEndpointNotImplemented) {
          line.addClass("ewpst__apiVersion--upToDate");
        } else if (statsEndpointNotImplemented) {
          line.addClass("ewpst__apiVersion--noStats");
          line.addTooltipLine("Stats endpoint has not been implemented.");
        } else {
          line.addClass("ewpst__apiVersion--obsolete");
          line.addTooltipLine("This major version of this API is obsolete or deprecated.");
        }
      } else {
        line.addClass("ewpst__apiVersion--error");
        String versionsWithAsteriskString = expectedVersionPrefixes.stream()
            .map(v -> String.format("%s*.*", v))
            .collect(Collectors.joining(", "));

        line.addTooltipLine(
            "This version is incompatible with its namespace. "
            + String.format("Namespace %s allows to use versions: %s",
                apiNamespace, versionsWithAsteriskString)
        );
      }

      this.addContentLine(line);
    }
  }

  private boolean isStatsEndpointImplemented(Element apiEntry) {
    for (Node apiEntryChild : Utils.asNodeList(apiEntry.getChildNodes())) {
      if ("stats-url".equals(apiEntryChild.getLocalName())) {
        return true;
      }
    }

    return false;
  }

}
