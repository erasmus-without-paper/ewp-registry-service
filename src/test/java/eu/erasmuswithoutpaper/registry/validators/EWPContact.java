package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public class EWPContact implements XMLSerializable {
  public EWPStringWithOptionalLang contact_name;
  public List<EWPStringWithOptionalLang> contact_name_;
  public List<EWPStringWithOptionalLang> person_given_names;
  public List<EWPStringWithOptionalLang> person_family_name;
  public EWPGender person_gender;
  public XMLSchemaRef<EWPPhoneNumber> phone_number;
  public XMLSchemaRef<EWPPhoneNumber> fax_number;
  public EWPEmail email;
  public XMLSchemaRef<EWPFlexibleAddress> street_address;
  public XMLSchemaRef<EWPFlexibleAddress> mailing_address;
  public List<EWPStringWithOptionalLang> role_description;


  @Override
  public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _to) {
    XMLSerializableUtils.OpenTag(_name, _ns, _to);
    XMLSerializableUtils.XMLSerializeSerializable("contact-name", _ns, contact_name, _to);
    XMLSerializableUtils.XMLSerializeList("contact-name", _ns, contact_name_, _to);
    XMLSerializableUtils.XMLSerializeList("person-given-names", _ns, person_given_names, _to);
    XMLSerializableUtils.XMLSerializeList("person-family-name", _ns, person_family_name, _to);
    XMLSerializableUtils.XMLSerializeSerializable("person-gender", _ns, person_gender, _to);
    XMLSerializableUtils.XMLSerializeSerializable("phone-number", "p", phone_number, _to);
    XMLSerializableUtils.XMLSerializeSerializable("fax-number", "p", fax_number, _to);
    XMLSerializableUtils.XMLSerializeSerializable("email", _ns, email, _to);
    XMLSerializableUtils.XMLSerializeSerializable("street-address", "a", street_address, _to);
    XMLSerializableUtils.XMLSerializeSerializable("mailing-address", "a", mailing_address, _to);
    XMLSerializableUtils.XMLSerializeList("role-description", _ns, role_description, _to);
    XMLSerializableUtils.CloseTag(_name, _ns, _to);
  }
}
