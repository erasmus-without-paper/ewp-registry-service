package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public class EWPUrlHTTP implements XMLSerializable {
  private String url;

  public EWPUrlHTTP(String _url) {
    if (!_url.matches("https?://.+")) {
      throw new IllegalArgumentException("HTTP URL should begin with \"http://\" or \"https://\".");
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
