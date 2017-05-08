package de.foodora.automapper.internal.codegen.dependencygraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;

import de.foodora.automapper.internal.codegen.TypeUtil;

public class DependencySolver {

    private final ProcessingEnvironment processingEnv;

    public DependencySolver(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public List<TypeElement> resolveAllDependencies(Set<TypeElement> elements) {
        Map<TypeElement, Set<TypeElement>> dependencies = new HashMap<>();
        for (TypeElement element : elements) {
            if (!dependencies.containsKey(element)) {
                dependencies.put(element, new HashSet<>());
            }
            findAllDependents(element, dependencies);
        }

        return sortElementTopologically(dependencies);
    }

    private List<TypeElement> sortElementTopologically(Map<TypeElement, Set<TypeElement>> dependencies) {
        List<TypeElement> elements = new ArrayList<>();
        Set<TypeElement> visited = new HashSet<>();
        Deque<TypeElement> stack = new ArrayDeque<>();

        for (Map.Entry<TypeElement, Set<TypeElement>> entry : dependencies.entrySet()) {
            if (!visited.contains(entry.getKey())) {
                exploreChildrenDFS(dependencies, entry.getKey(), stack, visited);
            }
        }

        while (stack.size() > 0) {
            elements.add(stack.removeLast());
        }

        return elements;
    }

    private void exploreChildrenDFS(
        Map<TypeElement, Set<TypeElement>> dependencies,
        TypeElement element,
        Deque<TypeElement> stack,
        Set<TypeElement> visited
    ) {
        visited.add(element);
        for (TypeElement childVertex : dependencies.get(element)) {
            if (!visited.contains(childVertex)) {
                exploreChildrenDFS(dependencies, childVertex, stack, visited);
            }
        }
        stack.offerFirst(element);
    }

    private void findAllDependents(TypeElement element, Map<TypeElement, Set<TypeElement>> deps) {
        List<VariableElement> fields = ElementFilter.fieldsIn(element.getEnclosedElements());
        for (Element child : fields) {
            while (TypeUtil.isIterable(child, processingEnv.getTypeUtils())) {
                child = TypeUtil.getGenericElement(child, processingEnv.getElementUtils());
            }
            if (TypeUtil.isArray(child)) {
                child = processingEnv.getElementUtils().getTypeElement(child.getEnclosingElement().asType().toString());
            }
            if (!TypeUtil.isPrimitiveOrWrapper(child.asType()) && !TypeUtil.isJavaInternalType(child.asType())) {
                TypeElement childElement = (TypeElement) processingEnv.getTypeUtils().asElement(child.asType());
                if (!deps.get(element).contains(childElement)) {
                    // add this child as dependency for the parent
                    deps.get(element).add(childElement);
                }
                // if we have already explored this child
                if (!deps.containsKey(childElement)) {
                    // initialize list of empty dependencies for the child
                    deps.put(childElement, new HashSet<>());
                    // loop on all children of child
                    findAllDependents(childElement, deps);
                }
            }
        }
    }
}
