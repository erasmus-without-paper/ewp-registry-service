package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public class XMLSchemaRef<T extends XMLSerializable> implements XMLSerializable {
    public T elem;
    public String ns;

    public XMLSchemaRef(T _elem, String _namespace) {
        elem = _elem;
        ns = _namespace;
    }

    @Override
    public void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder) {
        elem.XMLSerialize(_name, ns, _tags, _builder);
    }
}
