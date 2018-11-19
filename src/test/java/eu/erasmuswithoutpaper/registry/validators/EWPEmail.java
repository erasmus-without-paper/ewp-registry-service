package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public class EWPEmail implements XMLSerializable {
  public String email;

  @Override
  public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
    XMLSerializableUtils.OpenTag(_name, _ns, _builder);
    XMLSerializableUtils.XMLSerializeString("email", _ns, email, _builder);
    XMLSerializableUtils.CloseTag(_name, _ns, _builder);
  }
}
