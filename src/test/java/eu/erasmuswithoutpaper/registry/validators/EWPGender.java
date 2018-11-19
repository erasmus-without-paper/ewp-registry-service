package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public enum EWPGender implements XMLSerializable {
  NOT_KNOWN("0"),
  MALE("1"),
  FEMALE("2"),
  NOT_APPLICABLE("9");

  String gender;

  EWPGender(String _gender) {
    gender = _gender;
  }

  public String getGenderCode() {
    return gender;
  }

  @Override
  public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
    XMLSerializableUtils.OpenTag(_name, _ns, _builder);
    _builder.appendEscape(gender);
    XMLSerializableUtils.CloseTag(_name, _ns, _builder);
  }
}
