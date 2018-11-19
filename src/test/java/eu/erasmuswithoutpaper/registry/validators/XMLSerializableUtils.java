package eu.erasmuswithoutpaper.registry.validators;

import javax.validation.constraints.NotNull;
import java.util.List;

public class XMLSerializableUtils {
  public static <T extends XMLSerializable> void XMLSerializeList(
      @NotNull String _name,
      String _ns,
      List<T> _list,
      @NotNull XMLBuilder _to) {
    if (_list == null) {
      return;
    }
    for (XMLSerializable o: _list) {
      XMLSerializeSerializable(_name, _ns, o, _to);
    }
  }

  public static void XMLSerializeListString(
      @NotNull String _name,
      String _ns,
      List<String> _list,
      @NotNull XMLBuilder _to) {
    if (_list == null) {
      return;
    }
    for (String o: _list) {
      XMLSerializeString(_name, _ns, o, _to);
    }
  }

  public static <L extends XMLSerializable, R extends XMLSerializable> void XMLSerialize(
      EWPEither<L, R> _either,
      String _name,
      String _ns,
      @NotNull XMLBuilder _to) {
    _either.mapVoid(
        x -> x.XMLSerialize(_name, _ns, null, _to),
        x -> x.XMLSerialize(_name, _ns, null, _to)
    );
  }

  public static void XMLSerializeSerializable(
      @NotNull String _name,
      String _ns,
      XMLSerializable _object,
      @NotNull XMLBuilder _to) {
    if (_object != null) {
      _object.XMLSerialize(_name, _ns, null, _to);
    }
  }

  public static void XMLSerializeString(
      @NotNull String _name,
      String _ns,
      String _object,
      @NotNull XMLBuilder _to) {
    if (_object != null) {
      OpenTag(_name, _ns, _to);
      _to.appendEscape(_object);
      CloseTag(_name, _ns, _to);
    }
  }

  private static String MergeNameNS(
      String _name,
      String _ns) {
    if (_ns == null || _ns.isEmpty())
      return _name;
    return _ns + ":" + _name;
  }

  public static void CloseTag(
      @NotNull String _name,
      String _ns,
      @NotNull XMLBuilder _to) {
    if (_name == null) {
      return;
    }
    _to.append("</");
    _to.appendEscape(MergeNameNS(_name, _ns));
    _to.append(">");
  }

  public static void OpenTag(
      String _name,
      String _ns,
      XMLBuilder _to) {
    OpenTag(_name, _ns, null, _to);
  }

  public static void OpenTag(
      String _name,
      String _ns,
      List<String> _attributes,
      XMLBuilder _to) {
    if (_name == null) {
      return;
    }
    _to.append("<");
    _to.appendEscape(MergeNameNS(_name, _ns));
    if (_attributes != null) {
      for (String attrib : _attributes) {
        _to.append(" ").append(attrib);
      }
    }
    _to.append(">");
  }
}
