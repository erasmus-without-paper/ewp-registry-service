package eu.erasmuswithoutpaper.registry.echovalidator;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;

import org.w3c.dom.Element;

class HttpSecuritySettings {

  private final List<String> warnings;
  private boolean cliAuthHttpSig;
  private boolean cliAuthNone;
  private boolean cliAuthTlsCert;
  private boolean cliAuthTlsCertAllowsSelfSigned;
  private boolean reqEncrTls;
  private boolean resEncrTls;
  private boolean srvAuthHttpSig;
  private boolean srvAuthTlsCert;


  /**
   * Constructs a new object from an XML http-security element, as described <a href=
   * 'https://github.com/erasmus-without-paper/ewp-specs-sec-intro/blob/stable-v2/schema.xsd'>here
   * </a>.
   *
   * @param httpSecurityElement The http-security element to be parsed.
   */
  public HttpSecuritySettings(Element httpSecurityElement) {

    this.warnings = new ArrayList<>();

    // Parse Client Authentication Methods

    if ($(httpSecurityElement).children("client-auth-methods").isNotEmpty()) {
      this.cliAuthNone = false;
      this.cliAuthTlsCert = false;
      this.cliAuthTlsCertAllowsSelfSigned = false;
      this.cliAuthHttpSig = false;
      for (Element elem : $(httpSecurityElement).children("client-auth-methods").children()) {
        if (KnownElement.SECENTRY_CLIAUTH_NONE_V1.matches(elem)) {
          this.cliAuthNone = true;
        } else if (KnownElement.SECENTRY_CLIAUTH_TLSCERT_V1.matches(elem)) {
          this.cliAuthTlsCert = true;
          if ($(elem).attr("allows-self-signed").equals("true")
              || $(elem).attr("allows-self-signed").equals("1")) {
            this.cliAuthTlsCertAllowsSelfSigned = true;
          } else {
            this.cliAuthTlsCertAllowsSelfSigned = false;
          }
        } else if (KnownElement.SECENTRY_CLIAUTH_HTTPSIG_V1.matches(elem)) {
          this.cliAuthHttpSig = true;
        } else {
          this.addWarning("Unrecognized client authentication method", elem);
        }
      }
    } else {
      // Using defaults.
      this.cliAuthNone = false;
      this.cliAuthTlsCert = true;
      this.cliAuthTlsCertAllowsSelfSigned = true;
      this.cliAuthHttpSig = false;
    }

    // Parse Server Authentication Methods

    if ($(httpSecurityElement).children("server-auth-methods").isNotEmpty()) {
      this.srvAuthTlsCert = false;
      this.srvAuthHttpSig = false;
      for (Element elem : $(httpSecurityElement).children("server-auth-methods").children()) {
        if (KnownElement.SECENTRY_SRVAUTH_TLSCERT_V1.matches(elem)) {
          this.srvAuthTlsCert = true;
        } else if (KnownElement.SECENTRY_SRVAUTH_HTTPSIG_V1.matches(elem)) {
          this.srvAuthHttpSig = true;
        } else {
          this.addWarning("Unrecognized server authentication method", elem);
        }
      }
    } else {
      // Using defaults.
      this.srvAuthTlsCert = true;
      this.srvAuthHttpSig = false;
    }

    // Parse Request Encryption Methods

    if ($(httpSecurityElement).children("request-encryption-methods").isNotEmpty()) {
      this.reqEncrTls = false;
      for (Element elem : $(httpSecurityElement).children("request-encryption-methods")
          .children()) {
        if (KnownElement.SECENTRY_REQENCR_TLS_V1.matches(elem)) {
          this.reqEncrTls = true;
        } else {
          this.addWarning("Unrecognized request encryption method", elem);
        }
      }
    } else {
      // Using defaults.
      this.reqEncrTls = true;
    }

    // Parse Response Encryption Methods

    if ($(httpSecurityElement).children("response-encryption-methods").isNotEmpty()) {
      this.resEncrTls = false;
      for (Element elem : $(httpSecurityElement).children("response-encryption-methods")
          .children()) {
        if (KnownElement.SECENTRY_RESENCR_TLS_V1.matches(elem)) {
          this.resEncrTls = true;
        } else {
          this.addWarning("Unrecognized response encryption method", elem);
        }
      }
    } else {
      // Using defaults.
      this.resEncrTls = true;
    }
  }

  public List<String> getWarnings() {
    return Collections.unmodifiableList(this.warnings);
  }

  public boolean supportsCliAuthHttpSig() {
    return this.cliAuthHttpSig;
  }

  public boolean supportsCliAuthNone() {
    return this.cliAuthNone;
  }

  public boolean supportsCliAuthTlsCert() {
    return this.cliAuthTlsCert;
  }

  public boolean supportsCliAuthTlsCertSelfSigned() {
    return this.cliAuthTlsCert && this.cliAuthTlsCertAllowsSelfSigned;
  }

  public boolean supportsReqEncrTls() {
    return this.reqEncrTls;
  }

  public boolean supportsResEncrTls() {
    return this.resEncrTls;
  }

  public boolean supportsSrvAuthHttpSig() {
    return this.srvAuthHttpSig;
  }

  public boolean supportsSrvAuthTlsCert() {
    return this.srvAuthTlsCert;
  }

  private void addWarning(String message, Element elem) {
    this.warnings.add(message + ": {" + elem.getNamespaceURI() + "}" + elem.getLocalName());
  }
}
