package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

/*
* */
public class EWPFlexibleAddress implements XMLSerializable {
  public static String schema =
      "https://github.com/erasmus-without-paper/ewp-specs-types-address/blob/stable-v1/schema.xsd";
  public static String abbrev = "a";
  public static class AdvancedAddress implements XMLSerializable {
    public String buildingNumber;
    public String buildingName;
    public String streetName;
    public String unit;
    public String floor;
    public String postBoxOffice;
    public List<String> deliveryPointCode;
    private String real_abbrev = abbrev;

    @Override
    public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
      XMLSerializableUtils.OpenTag(_name, _ns, _builder);
      XMLSerializableUtils.XMLSerializeString("buildingNumber" , _ns, buildingNumber, _builder);
      XMLSerializableUtils.XMLSerializeString("buildingName", _ns, buildingName, _builder);
      XMLSerializableUtils.XMLSerializeString("streetName", _ns, streetName, _builder);
      XMLSerializableUtils.XMLSerializeString("unit", _ns, unit, _builder);
      XMLSerializableUtils.XMLSerializeString("floor", _ns, floor, _builder);
      XMLSerializableUtils.XMLSerializeString("postOfficeBox", _ns, postBoxOffice, _builder);
      XMLSerializableUtils.XMLSerializeListString("deliveryPointCode", _ns, deliveryPointCode, _builder);
      XMLSerializableUtils.OpenTag(_name, _ns, _builder);
    }
  }

  public class SimpleAddress implements XMLSerializable {
    private String real_abbrev = abbrev;
    public String[] lines = new String[4];

    @Override
    public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder builder) {
      for (String line: lines) {
        XMLSerializableUtils.XMLSerializeString("addressLine", _ns, line, builder);
      }
    }
  }
  public List<String> recipientName;
  public EWPEither<SimpleAddress, AdvancedAddress> address;
  public String postalCode;
  public String locality;
  public String region;
  public EWPCountryCode country;

  @Override
  public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
    XMLSerializableUtils.OpenTag(_name, _ns, _builder);
    XMLSerializableUtils.XMLSerializeListString("recipientName", _ns, recipientName, _builder);
    XMLSerializableUtils.XMLSerialize(address, null, _ns, _builder);
    XMLSerializableUtils.XMLSerializeString("postalCode", _ns, postalCode, _builder);
    XMLSerializableUtils.XMLSerializeString("locality", _ns, locality, _builder);
    XMLSerializableUtils.XMLSerializeString("region", _ns, region, _builder);
    XMLSerializableUtils.XMLSerializeSerializable("country", _ns, country, _builder);
    XMLSerializableUtils.CloseTag(_name, _ns, _builder);
  }
}
