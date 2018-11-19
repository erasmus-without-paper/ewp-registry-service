package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public class EWPPhoneNumber implements XMLSerializable {
  public String e164;
  public String ext;
  public String other_format;

  @Override
  public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
    XMLSerializableUtils.OpenTag(_name, _ns, _tags, _builder);
    if (e164 != null) {
      XMLSerializableUtils.XMLSerializeString("e164", _ns, e164, _builder);
    }
    if (ext != null) {
      XMLSerializableUtils.XMLSerializeString("ext", _ns, ext, _builder);
    }
    if (other_format != null) {
      XMLSerializableUtils.XMLSerializeString("other-format", _ns, other_format, _builder);
    }
    XMLSerializableUtils.CloseTag(_name, _ns, _builder);

  }
}
