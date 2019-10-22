package eu.erasmuswithoutpaper.registry.consoleapplication;

import java.util.List;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextTerminal;

public class UserInput {
  static <T> T userSelectList(TextIO console, List<T> elements, String prompt) {
    return userSelectList(console, elements, prompt, Object::toString);
  }

  static <T> T userSelectList(TextIO console, List<T> elements,
      String prompt, Stringifier<T> stringifier) {
    final TextTerminal<?> textTerminal = console.getTextTerminal();
    textTerminal.print("0: EXIT\n");
    for (int i = 0; i < elements.size(); i++) {
      T element = elements.get(i);
      textTerminal.printf("%d: %s\n", i + 1, stringifier.toString(element));
    }
    int selected = console.newIntInputReader()
        .withMinVal(0)
        .withMaxVal(elements.size() + 1)
        .read(prompt);
    if (selected == 0) {
      return null;
    }

    T selectedElement = elements.get(selected - 1);
    textTerminal.printf("Selected %s\n", stringifier.toString(selectedElement));
    return selectedElement;
  }

}
