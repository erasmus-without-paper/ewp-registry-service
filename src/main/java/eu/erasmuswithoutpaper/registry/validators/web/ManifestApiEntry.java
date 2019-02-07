package eu.erasmuswithoutpaper.registry.validators.web;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownNamespace;
import eu.erasmuswithoutpaper.registry.validators.ApiValidatorsManager;
import eu.erasmuswithoutpaper.registry.validators.CombEntry;
import eu.erasmuswithoutpaper.registry.validators.HttpSecurityDescription;
import eu.erasmuswithoutpaper.registry.validators.SemanticVersion;
import eu.erasmuswithoutpaper.registry.validators.echovalidator.HttpSecuritySettings;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.joox.Match;
import org.w3c.dom.Element;


@SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD",
                    justification = "Fields are referenced while rendering the webpage")
public class ManifestApiEntry {
  public final String name;
  public final String version;
  public final String url;
  public final List<HttpSecurityDescription> securities;
  public final boolean available;

  /**
   * Description of API endpoint.
   *
   * @param name
   *     name of API, e.g. echo.
   * @param version
   *     semantic version of this API.
   * @param url
   *     endpoint under which this API can be reached.
   * @param securities
   *     list of all supported HTTP security combinations.
   * @param available
   *     whether tests for this API are available.
   */
  public ManifestApiEntry(String name, String version, String url,
      List<HttpSecurityDescription> securities, boolean available) {
    this.name = name;
    this.version = version;
    this.url = url;
    this.securities = securities;
    this.available = available;
  }

  /**
   * Creates descriptions for all APIs implemented in provided manifest.
   *
   * @param manifest
   *     contents of a v5 manifest
   * @return List of ManifestApiEntrys, one for each API implemented in the manifest.
   */
  public static List<ManifestApiEntry> parseManifest(String manifest,
      ApiValidatorsManager manager) {
    List<ManifestApiEntry> ret = new ArrayList<>();
    List<Match> apis =
        $(manifest).namespaces(KnownNamespace.prefixMap()).xpath("mf5:host/r:apis-implemented/*")
            .each();
    for (Match api : apis) {
      try {
        ret.add(parseApiEntry(api, manager));
      } catch (ParseException e) {
        //ignore
      }
    }
    return ret;
  }

  private static ManifestApiEntry parseApiEntry(Match api, ApiValidatorsManager manager)
      throws ParseException {
    final Element apiElement = api.get(0);

    final String apiName = apiElement.getLocalName();

    final String version = apiElement.getAttribute("version");
    SemanticVersion semanticVersion = null;
    try {
      semanticVersion = new SemanticVersion(version);
    } catch (SemanticVersion.InvalidVersionString invalidVersionString) {
      throw new ParseException("Cannot parse version " + version);
    }

    final String url = api.xpath("*[local-name()='url']").text();

    final Element securityElement = api.xpath("*[local-name()='http-security']").get(0);
    List<HttpSecurityDescription> securitySettings =
        parseSecurity(new HttpSecuritySettings(securityElement));

    return new ManifestApiEntry(apiName, version, url, securitySettings,
        manager.hasCompatibleTests(apiName, semanticVersion));
  }

  private static List<CombEntry> getResponseEncryptionMethods(HttpSecuritySettings sec) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsResEncrTls()) {
      ret.add(CombEntry.RESENCR_TLS);
    }
    if (sec.supportsResEncrEwp()) {
      ret.add(CombEntry.RESENCR_EWP);
    }
    return ret;
  }

  private static List<CombEntry> getRequestEncryptionMethods(HttpSecuritySettings sec) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsReqEncrTls()) {
      ret.add(CombEntry.REQENCR_TLS);
    }
    if (sec.supportsReqEncrEwp()) {
      ret.add(CombEntry.REQENCR_EWP);
    }
    return ret;
  }

  private static List<CombEntry> getServerAuthenticationMethods(HttpSecuritySettings sec) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsSrvAuthTlsCert()) {
      ret.add(CombEntry.SRVAUTH_TLSCERT);
    }
    if (sec.supportsSrvAuthHttpSig()) {
      ret.add(CombEntry.SRVAUTH_HTTPSIG);
    }
    return ret;
  }

  private static List<CombEntry> getClientAuthenticationMethods(HttpSecuritySettings sec) {
    List<CombEntry> ret = new ArrayList<>();
    if (sec.supportsCliAuthNone()) {
      ret.add(CombEntry.CLIAUTH_NONE);
    }

    if (sec.supportsCliAuthTlsCert()) {
      if (sec.supportsCliAuthTlsCertSelfSigned()) {
        ret.add(CombEntry.CLIAUTH_TLSCERT_SELFSIGNED);
      } else {
        //TODO not implemented
      }
    }

    if (sec.supportsCliAuthHttpSig()) {
      ret.add(CombEntry.CLIAUTH_HTTPSIG);
    }
    return ret;
  }

  private static List<HttpSecurityDescription> parseSecurity(
      HttpSecuritySettings securitySettings) {
    List<HttpSecurityDescription> result = new ArrayList<>();
    List<CombEntry> clientAuth = getClientAuthenticationMethods(securitySettings);
    List<CombEntry> serverAuth = getServerAuthenticationMethods(securitySettings);
    List<CombEntry> reqEncr = getRequestEncryptionMethods(securitySettings);
    List<CombEntry> resEncr = getResponseEncryptionMethods(securitySettings);

    for (CombEntry clientAuthMethod : clientAuth) {
      for (CombEntry serverAuthMethod : serverAuth) {
        for (CombEntry reqEncrMethod : reqEncr) {
          for (CombEntry resEncMethod : resEncr) {
            result.add(
                new HttpSecurityDescription(clientAuthMethod, serverAuthMethod, reqEncrMethod,
                    resEncMethod));
          }
        }
      }
    }

    return result;
  }

  private static class ParseException extends Exception {
    public ParseException(String reason) {
      super(reason);
    }
  }

}
