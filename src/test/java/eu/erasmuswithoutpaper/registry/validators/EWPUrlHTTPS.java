package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public class EWPUrlHTTPS implements XMLSerializable {
  private String url;

  public EWPUrlHTTPS(String _url) {
    if (!_url.matches("https://.+")) {
      throw new IllegalArgumentException("HTTPS URL should begin with \"https://\".");
    }
    url = _url;
  }

  @Override
  public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
    XMLSerializableUtils.OpenTag(_name, _ns, _builder);
    _builder.appendEscape(url);
    XMLSerializableUtils.CloseTag(_name, _ns, _builder);
  }
}
