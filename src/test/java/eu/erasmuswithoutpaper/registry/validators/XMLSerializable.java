package eu.erasmuswithoutpaper.registry.validators;

import java.util.List;

public interface XMLSerializable {
    void XMLSerialize(String _name, String _ns, List<String> _tags, XMLBuilder _builder);
}
