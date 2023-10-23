package eu.erasmuswithoutpaper.registry.validators;

public enum ContentType {
  APPLICATION_WWW_FORM_URLENCODED("application/x-www-form-urlencoded"),
  TEXT_XML("text/xml");

  private final String contentType;

  ContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getContentTypeString() {
    return this.contentType;
  }
}
