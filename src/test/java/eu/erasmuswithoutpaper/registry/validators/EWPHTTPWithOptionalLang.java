package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public class EWPHTTPWithOptionalLang implements XMLSerializable {
  EWPUrlHTTP url;
  EWPOptionalLangTag tag;

  public EWPHTTPWithOptionalLang(EWPUrlHTTP _url, String _lang) {
    tag = new EWPOptionalLangTag(_lang);
    url = _url;
  }

  public EWPHTTPWithOptionalLang(EWPUrlHTTP _url) {
    tag = new EWPOptionalLangTag(null);
    url = _url;
  }

  @Override
  public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
    List<String> tags = tag.getTags();
    if (_tags != null) {
      tags.addAll(_tags);
    }
    url.XMLSerialize(_name, _ns, tags, _builder);
  }
}
