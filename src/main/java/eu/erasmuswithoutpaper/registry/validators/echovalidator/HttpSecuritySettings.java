package eu.erasmuswithoutpaper.registry.validators.echovalidator;

import static org.joox.JOOX.$;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.erasmuswithoutpaper.registry.documentbuilder.KnownElement;

import org.w3c.dom.Element;

public class HttpSecuritySettings {

  private final List<String> notices;
  private boolean cliAuthHttpSig;
  private boolean cliAuthNone;
  private boolean reqEncrTls;
  private boolean reqEncrEwp;
  private boolean resEncrTls;
  private boolean resEncrEwp;
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

    this.notices = new ArrayList<>();

    // Parse Client Authentication Methods

    if ($(httpSecurityElement).children("client-auth-methods").isNotEmpty()) {
      this.cliAuthNone = false;
      this.cliAuthHttpSig = false;
      for (Element elem : $(httpSecurityElement).children("client-auth-methods").children()) {
        if (KnownElement.SECENTRY_CLIAUTH_NONE_V1.matches(elem)) {
          this.cliAuthNone = true;
        } else if (KnownElement.SECENTRY_CLIAUTH_HTTPSIG_V1.matches(elem)) {
          this.cliAuthHttpSig = true;
        } else {
          this.addNotice("Unrecognized client authentication method", elem);
        }
      }
    } else {
      // Using defaults.
      this.cliAuthNone = false;
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
          this.addNotice("Unrecognized server authentication method", elem);
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
      this.reqEncrEwp = false;
      for (Element elem : $(httpSecurityElement).children("request-encryption-methods")
          .children()) {
        if (KnownElement.SECENTRY_REQENCR_TLS_V1.matches(elem)) {
          this.reqEncrTls = true;
        } else if (KnownElement.SECENTRY_REQENCR_EWP_RSA_AES128GCM_V1.matches(elem)) {
          this.reqEncrEwp = true;
        } else {
          this.addNotice("Unrecognized request encryption method", elem);
        }
      }
    } else {
      // Using defaults.
      this.reqEncrTls = true;
      this.reqEncrEwp = false;
    }

    // Parse Response Encryption Methods

    if ($(httpSecurityElement).children("response-encryption-methods").isNotEmpty()) {
      this.resEncrTls = false;
      this.resEncrEwp = false;
      for (Element elem : $(httpSecurityElement).children("response-encryption-methods")
          .children()) {
        if (KnownElement.SECENTRY_RESENCR_TLS_V1.matches(elem)) {
          this.resEncrTls = true;
        } else if (KnownElement.SECENTRY_RESENCR_EWP_RSA_AES128GCM_V1.matches(elem)) {
          this.resEncrEwp = true;
        } else {
          this.addNotice("Unrecognized response encryption method", elem);
        }
      }
    } else {
      // Using defaults.
      this.resEncrTls = true;
      this.resEncrEwp = false;
    }
  }

  public List<String> getNotices() {
    return Collections.unmodifiableList(this.notices);
  }

  public boolean supportsCliAuthHttpSig() {
    return this.cliAuthHttpSig;
  }

  public boolean supportsCliAuthNone() {
    return this.cliAuthNone;
  }

  public boolean supportsReqEncrEwp() {
    return this.reqEncrEwp;
  }

  public boolean supportsReqEncrTls() {
    return this.reqEncrTls;
  }

  public boolean supportsResEncrEwp() {
    return this.resEncrEwp;
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

  private void addNotice(String message, Element elem) {
    this.notices.add(message + ": {" + elem.getNamespaceURI() + "}" + elem.getLocalName());
  }
}
