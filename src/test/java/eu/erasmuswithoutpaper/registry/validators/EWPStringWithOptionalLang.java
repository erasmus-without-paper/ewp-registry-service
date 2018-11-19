package eu.erasmuswithoutpaper.registry.validators;

import org.eclipse.jgit.annotations.NonNull;

import java.util.List;

public class EWPStringWithOptionalLang implements XMLSerializable {
  public String string;
  private EWPOptionalLangTag tag;

  public static String schema = "https://github.com/erasmus-without-paper/ewp-specs-architecture/blob/stable-v1/common-types.xsd";
  public static String abbrev = "ewp";


  public EWPStringWithOptionalLang(@NonNull String _string) {
    tag = new EWPOptionalLangTag(null);
    string = _string;
  }

  public EWPStringWithOptionalLang(@NonNull String _string, String _lang) {
    tag = new EWPOptionalLangTag(_lang);
    string = _string;
  }

  @Override
  public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
    XMLSerializableUtils.OpenTag(_name, _ns, tag.getTags(), _builder);
    _builder.appendEscape(string);
    XMLSerializableUtils.CloseTag(_name, _ns, _builder);
  }
}
