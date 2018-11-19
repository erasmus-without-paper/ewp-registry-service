package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public class EWPCountryCode implements XMLSerializable {
  String code;

  public static String schema = "https://github.com/erasmus-without-paper/ewp-specs-architecture/blob/stable-v1/common-types.xsd";
  public static String abbrev = "ewp";

  public EWPCountryCode(char _c1, char _c2) {
    if (!Character.isUpperCase(_c1) || !Character.isUpperCase(_c2)) {
      throw new IllegalArgumentException();
    }
    code = Character.toString(_c1).concat(Character.toString(_c2));
  }
  public EWPCountryCode(String _code) {
    if (_code == null ||
        _code.length() != 2 ||
        !Character.isUpperCase(_code.charAt(0)) ||
        !Character.isUpperCase(_code.charAt(1))) {
      throw new IllegalArgumentException();
    }
    code = _code;
  }

  public String GetCode() {
  return code;
  }

  @Override
  public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
    XMLSerializableUtils.OpenTag(_name, _ns, _builder);
    _builder.appendEscape(code);
    XMLSerializableUtils.CloseTag(_name, _ns, _builder);
  }
}
