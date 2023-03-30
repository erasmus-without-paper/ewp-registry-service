package eu.erasmuswithoutpaper.registry.constraints;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import eu.erasmuswithoutpaper.registry.common.Severity;
import eu.erasmuswithoutpaper.registry.common.Utils;
import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registryclient.RegistryClient;

import org.joox.Match;
import org.w3c.dom.Document;

/**
 * Scan the manifest for all the API namespaces which seem to originate from the EWP project and
 * compare the <code>stable-*</code> branches with the <code>version</code> attributes.
 *
 * <p>
 * For example, if the API's namespace contains the <code>/stable-v4/</code> string, but its
 * <code>version</code> attribute starts with <code>"3."</code>, then a warning will be reported.
 * </p>
 */
public class VerifyApiVersions implements ManifestConstraint {

  /**
   * Returns prefixes of API versions allowed to be used with provided namespace,
   * in format "X.", where X is major version.
   */
  public static List<String> getExpectedVersionPrefixes(String namespaceUri) {
    Matcher pm = Pattern.compile(".*/stable-v([0-9]+).*").matcher(namespaceUri);
    List<String> expectedVersionPrefixes = new ArrayList<>();
    if (!pm.matches()) {
      // No "stable-*" sequence found.
      if (namespaceUri.contains("/master")) {
        // Most probably a draft API.
        expectedVersionPrefixes.add("0.");
      } else {
        // Non-standard. We will ignore this API.
      }
    } else {
      expectedVersionPrefixes.add(pm.group(1) + ".");
      if (pm.group(1).equals("1")) {
        // It is also allowed for 0.x.y APIs to use stable-v1 namespace.
        expectedVersionPrefixes.add("0.");
      }
    }
    return expectedVersionPrefixes;
  }

  @Override
  public List<FailedConstraintNotice> filter(Document doc, RegistryClient registryClient) {
    List<FailedConstraintNotice> notices = new ArrayList<>();
    Match root = $(doc).namespaces(KnownNamespace.prefixMap());

    for (Match match : root.xpath("mf6:host/r:apis-implemented/*").each()) {
      String namespaceUri = match.namespaceURI();
      if (!namespaceUri.startsWith("https://github.com/erasmus-without-paper/")) {
        /*
         * Most probably, this API is not related to EWP. And, as such, it does not necessarily
         * follow EWP namespace-naming rules. We will ignore this API.
         */
        continue;
      }
      List<String> expectedVersionPrefixes = getExpectedVersionPrefixes(namespaceUri);
      if (match.attr("version") == null) {
        continue;
      }
      boolean foundExpected = false;
      for (String prefix : expectedVersionPrefixes) {
        if (match.attr("version").startsWith(prefix)) {
          foundExpected = true;
          break;
        }
      }
      if (!foundExpected) {
        StringBuilder sb = new StringBuilder();
        sb.append("<p>According to EWP's <a href='https://github.com/erasmus-without-paper/");
        sb.append("ewp-specs-management#git-branches-and-xml-namespaces'>namespace-naming ");
        sb.append("rules</a>, the version of your API in the <code>");
        sb.append(Utils.escapeHtml(namespaceUri));
        sb.append("</code> namespace should start with ");
        List<String> htmledAlternatives = expectedVersionPrefixes.stream()
            .map(s -> "<code>" + Utils.escapeHtml(s) + "</code>").collect(Collectors.toList());
        String joinedAlternatives = htmledAlternatives.stream().collect(Collectors.joining(" or "));
        sb.append(joinedAlternatives);
        sb.append(", but <code>");
        sb.append(Utils.escapeHtml(match.attr("version")));
        sb.append("</code> was found instead.</p>");
        sb.append("<p>Note, that this check is applied only for API namespaces beginning ");
        sb.append("with <code>https://github.com/erasmus-without-paper/</code>.</p>");
        notices.add(new FailedConstraintNotice(Severity.WARNING, sb.toString()));
      }
    }
    return notices;
  }

}
