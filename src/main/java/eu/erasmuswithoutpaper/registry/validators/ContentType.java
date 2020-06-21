package eu.erasmuswithoutpaper.registry.validators;

public enum ContentType {
  ApplicationWwwFormUrlencoded("application/x-www-form-urlencoded"),
  TextXml("text/xml");

  private final String contentType;

  ContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getContentTypeString() {
    return this.contentType;
  }
}
