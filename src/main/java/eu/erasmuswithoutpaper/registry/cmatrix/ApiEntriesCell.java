package eu.erasmuswithoutpaper.registry.cmatrix;

import static org.joox.JOOX.$;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;
import eu.erasmuswithoutpaper.registryclient.ApiSearchConditions;
import eu.erasmuswithoutpaper.registryclient.HeiEntry;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.w3c.dom.Element;

/**
 * This is used for ordering XML API entries by their version.
 */
class ApiEntriesCell extends CoverageMatrixCell {

  private static class ApiVersionComparator implements Comparator<Element>, Serializable {
    private static final long serialVersionUID = 1958058332903191615L;

    private final List<String> apiNamespacesOrder;

    public ApiVersionComparator(KnownElement[] apiEntryClasses) {
      this.apiNamespacesOrder = Arrays.stream(apiEntryClasses)
          .map(KnownElement::getNamespaceUri)
          .collect(Collectors.toList());
    }

    @Override
    public int compare(Element e1, Element e2) {
      List<Integer> p1 = this.extractVersionInts(e1);
      List<Integer> p2 = this.extractVersionInts(e2);
      for (int i = 0; i < p1.size() && i < p2.size(); i++) {
        int result = p1.get(i).compareTo(p2.get(i));
        if (result != 0) {
          return result;
        }
      }
      Integer s1 = p1.size();
      Integer s2 = p2.size();
      int result = s1.compareTo(s2);
      if (result != 0) {
        return result;
      }
      // Version are equal, but maybe their namespaces are not.
      String namespace1 = e1.getNamespaceURI();
      Integer namespace1Index = this.apiNamespacesOrder.indexOf(namespace1);

      String namespace2 = e2.getNamespaceURI();
      Integer namespace2Index = this.apiNamespacesOrder.indexOf(namespace2);

      return namespace1Index.compareTo(namespace2Index);
    }

    private List<Integer> extractVersionInts(Element elem) {
      List<Integer> result = new ArrayList<>(3);
      String version = $(elem).attr("version");
      if (version == null) {
        // Should not happen, but just in case.
        version = "0.0.0";
      }
      for (String entry : version.split("\\.")) {
        result.add(this.parseInt(entry));
      }
      return result;
    }

    private Integer parseInt(String entry) {
      try {
        return Integer.parseUnsignedInt(entry);
      } catch (NumberFormatException e) {
        return Integer.MAX_VALUE;
      }
    }
  }

  final KnownElement[] apiEntryClasses;
  final RegistryClient client;
  final HeiEntry hei;
  final List<Element> matchedApiEntries;
  final KnownElement lastClass;

  ApiEntriesCell(int colorClass, RegistryClient client, HeiEntry hei,
      KnownElement... apiEntryClasses) {
    super(colorClass);
    this.client = client;
    this.hei = hei;
    this.apiEntryClasses = apiEntryClasses;
    this.matchedApiEntries = new ArrayList<>();

    this.lastClass = this.apiEntryClasses[this.apiEntryClasses.length - 1];
    for (int i = 0; i < this.apiEntryClasses.length; i++) {
      KnownElement apiClass = this.apiEntryClasses[i];
      ApiSearchConditions conds = new ApiSearchConditions();
      conds.setRequiredHei(this.hei.getId());
      conds.setApiClassRequired(apiClass.getNamespaceUri(), apiClass.getLocalName());
      this.matchedApiEntries.addAll(this.client.findApis(conds));
    }
    this.matchedApiEntries.sort(new ApiVersionComparator(this.apiEntryClasses).reversed());
    // Check the first one. Is it the most recent API class?
    boolean isLastClassImplemented = (this.matchedApiEntries.size() > 0
        && this.lastClass.matches(this.matchedApiEntries.get(0)));
    if (isLastClassImplemented) {
      this.addClass("upToDate");
    }
  }


}
