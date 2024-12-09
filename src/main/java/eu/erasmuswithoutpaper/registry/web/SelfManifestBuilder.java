package eu.erasmuswithoutpaper.registry.web;

import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import eu.erasmuswithoutpaper.registry.common.EncodedCertificateAndKeys;

import https.github_com.erasmus_without_paper.ewp_specs_api_discovery.tree.stable_v6.Host;
import https.github_com.erasmus_without_paper.ewp_specs_api_discovery.tree.stable_v6.Manifest;
import https.github_com.erasmus_without_paper.ewp_specs_api_registry.tree.stable_v1.ApisImplemented;
import https.github_com.erasmus_without_paper.ewp_specs_api_registry.tree.stable_v1.Hei;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.MultilineString;
import https.github_com.erasmus_without_paper.ewp_specs_architecture.blob.stable_v1.common_types.StringWithOptionalLang;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SelfManifestBuilder {
  private final Document document;
  private final Host host;
  private final ApisImplemented apis;
  private final Manifest manifest;
  private final JAXBContext jaxbContext;
  private final Marshaller jaxbMarshaller;

  /**
   * Used to build registry's manifests.
   */
  public SelfManifestBuilder() {
    try {
      this.jaxbContext = JAXBContext.newInstance(Manifest.class);
      this.jaxbMarshaller = this.jaxbContext.createMarshaller();
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }

    this.manifest = new Manifest();
    this.apis = new ApisImplemented();
    this.host = new Host();
    this.host.setApisImplemented(this.apis);
    this.host.setAdminProvider("EWP");
    this.manifest.setHost(this.host);

    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = dbf.newDocumentBuilder();
      this.document = builder.newDocument();
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Add admin emails to the manifest.
   * @param emails
   *     a list of emails to add.
   * @return
   *     self, to allow for method chaining.
   */
  public SelfManifestBuilder addAdminEmails(List<String> emails) {
    for (String email : emails) {
      addAdminEmail(email);
    }

    return this;
  }

  /**
   * Add admin email to the manifest.
   * @param email
   *     an email to add.
   * @return
   *     self, to allow for method chaining.
   */
  public SelfManifestBuilder addAdminEmail(String email) {
    this.host.getAdminEmail().add(email);
    return this;
  }

  /**
   * Set admin notes in the manifest.
   * @param notes
   *     notes to add.
   * @return
   *     self, to allow for method chaining.
   */
  public SelfManifestBuilder setAdminNotes(String notes) {
    MultilineString note = new MultilineString();
    note.setValue(notes);
    this.host.setAdminNotes(note);
    return this;
  }

  /**
   * Add API to the manifest.
   * @param name
   *     name of the API.
   * @param version
   *     version of the API. Can be null.
   * @param xmlns
   *     xmlns attribute.
   * @param additionalTags
   *     list of additional tag names and their values. They are appended as
   *     child elements of the main API XML element.
   * @return
   *     self, to allow for method chaining.
   */
  public SelfManifestBuilder addApi(String name, String version, String xmlns,
      boolean addHttpSecurity, List<Map.Entry<String, String>> additionalTags) {

    Element api = this.document.createElementNS(xmlns, name);

    if (version != null) {
      api.setAttribute("version", version);
    }

    if (addHttpSecurity) {
      Element httpSecurity = this.document.createElementNS(xmlns, "http-security");
      api.appendChild(httpSecurity);
      addBasicAuthMethods(httpSecurity);
    }

    for (Map.Entry<String, String> entry : additionalTags) {
      String tagName = entry.getKey();
      String tagValue = entry.getValue();

      Element element = this.document.createElementNS(xmlns, tagName);
      element.setTextContent(tagValue);
      api.appendChild(element);
    }

    this.apis.getAny().add(api);
    return this;
  }

  private void addBasicAuthMethods(Element httpSecurity) {
    Element clientAuthMethods = this.document.createElementNS(
        "https://github.com/erasmus-without-paper/ewp-specs-sec-intro/tree/stable-v2",
        "client-auth-methods");
    httpSecurity.appendChild(clientAuthMethods);
    Element httpSig = this.document.createElementNS(
        "https://github.com/erasmus-without-paper/ewp-specs-sec-cliauth-httpsig/tree/stable-v1",
        "httpsig");
    clientAuthMethods.appendChild(httpSig);
    Element serverAuthMethods = this.document.createElementNS(
        "https://github.com/erasmus-without-paper/ewp-specs-sec-intro/tree/stable-v2",
        "server-auth-methods");
    httpSecurity.appendChild(serverAuthMethods);
    Element tlsCert = this.document.createElementNS(
        "https://github.com/erasmus-without-paper/ewp-specs-sec-srvauth-tlscert/tree/stable-v1",
        "tlscert");
    serverAuthMethods.appendChild(tlsCert);
  }

  /**
   * Set HEI in the manifest.
   * @param heiId
   *     HEI ID.
   * @param name
   *     name of HEI.
   * @return
   *     self, to allow for method chaining.
   */
  public SelfManifestBuilder setHei(String heiId, String name) {
    StringWithOptionalLang nameWithOptionalLang = new StringWithOptionalLang();
    nameWithOptionalLang.setValue(name);

    Hei hei = new Hei();
    hei.getName().add(nameWithOptionalLang);
    hei.setId(heiId);

    Host.InstitutionsCovered institutions = new Host.InstitutionsCovered();
    institutions.setHei(hei);
    this.host.setInstitutionsCovered(institutions);
    return this;
  }

  /**
   * Add certificates to the manifest
   * @param validatorHostCertificatesAndKeys
   *     list of EncodedCertificateAndKeys with certificates to add.
   * @return
   *     self, to allow for method chaining.
   */
  public SelfManifestBuilder addClientCertificates(
      List<EncodedCertificateAndKeys> validatorHostCertificatesAndKeys) {

    Host.ClientCredentialsInUse clientCredentials = new Host.ClientCredentialsInUse();

    // Add client keys in use.
    for (EncodedCertificateAndKeys encodedCertificateAndKeys : validatorHostCertificatesAndKeys) {
      clientCredentials.getRsaPublicKey().add(
          Base64.decodeBase64(encodedCertificateAndKeys.getClientPublicKeyEncoded()));
    }

    // Add server credentials in use.
    Host.ServerCredentialsInUse serverCredentials = new Host.ServerCredentialsInUse();
    for (EncodedCertificateAndKeys encodedCertificateAndKeys : validatorHostCertificatesAndKeys) {
      serverCredentials.getRsaPublicKey().add(
          Base64.decodeBase64(encodedCertificateAndKeys.getServerPublicKeyEncoded()));
    }

    this.host.setClientCredentialsInUse(clientCredentials);
    this.host.setServerCredentialsInUse(serverCredentials);
    return this;
  }

  /**
   * Builds XML from the collected information.
   * @return
   *     manifest XML as a String.
   */
  public String buildXml() {
    try {
      StringWriter xmlStringWriter = new StringWriter();
      this.jaxbMarshaller.marshal(this.manifest, xmlStringWriter);
      return xmlStringWriter.toString();
    } catch (JAXBException e) {
      throw new RuntimeException(e);
    }
  }
}