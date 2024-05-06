package net.phoenix.handlers;

import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import net.phoenix.annotations.Exclude;
import org.jetbrains.annotations.NotNull;

import javax.lang.model.element.Element;

public class SettersHandler extends SetHandler {
    public SettersHandler(Trees trees, TreeMaker treeMaker, Context context) {
        super(trees, treeMaker, context);
    }

    @Override
    public void handle(@NotNull Element element) {
        element.getEnclosedElements().stream()
                .filter(e -> e.getKind().isField())
                .forEach(fieldElement -> {
                    if (fieldElement.getAnnotation(Exclude.class) != null) {
                        return;
                    }

                    setter(fieldElement);
                });
    }
}
