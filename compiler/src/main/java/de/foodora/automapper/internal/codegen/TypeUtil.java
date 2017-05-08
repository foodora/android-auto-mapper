package de.foodora.automapper.internal.codegen;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;

import java.util.List;

import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public class TypeUtil {
    /**
     * Returns the name of the package that the given type is in. If the type is in the default
     * (unnamed) package then the name is the empty string.
     *
     * @param type given type
     * @return package name
     */
    public static String packageNameOf(TypeElement type) {
        while (true) {
            Element enclosing = type.getEnclosingElement();
            if (enclosing instanceof PackageElement) {
                return ((PackageElement) enclosing).getQualifiedName().toString();
            }
            type = (TypeElement) enclosing;
        }
    }

    /**
     * Returns the simple class name once package is strip package.
     *
     * @param s fully qualified class name
     * @return simple class name
     */
    public static String simpleNameOf(String s) {
        if (s.contains(".")) {
            return s.substring(s.lastIndexOf('.') + 1);
        } else {
            return s;
        }
    }

    public static boolean isClassOfType(Types typeUtils, TypeMirror type, TypeMirror cls) {
        return type != null && typeUtils.isAssignable(cls, type);
    }

    public static TypeName getRawTypeOfIterable(Element iterable) {
        return ((ParameterizedTypeName) TypeName.get(iterable.asType())).rawType;
    }


    public static Element getGenericElement(Element child, Elements elementUtils) {
        TypeName typeName = (TypeName.get(child.asType()));
        if (typeName instanceof ParameterizedTypeName) {
            List<TypeName> typeNames = ((ParameterizedTypeName) typeName).typeArguments;
            if (typeNames != null && typeNames.size() > 0) {
                child = elementUtils.getTypeElement(typeNames.get(0).toString());
            }
        }
        return child;
    }

    public static boolean isIterable(Element element, Types typeUtils) {
        if (isPrimitiveOrWrapper(element.asType())) {
            return false;
        }
        TypeElement typeElement = (TypeElement) typeUtils.asElement(element.asType());

        return isIterable(typeElement);
    }

    public static boolean isArray(Element variableElement) {
        return variableElement.asType().getKind() == TypeKind.ARRAY;
    }

    public static boolean isIterable(VariableElement variableElement) {
        return TypeName.get(variableElement.asType()) instanceof ParameterizedTypeName;
    }

    public static boolean isIterable(TypeElement typeElement) {
        if (typeElement != null && typeElement.getInterfaces() != null && typeElement.getInterfaces().size() > 0) {
            for (TypeMirror typeMirror : typeElement.getInterfaces()) {
                if (typeMirror.toString().startsWith("java.util.List")
                    || typeMirror.toString().startsWith("java.util.Set")
                    || typeMirror.toString().startsWith("java.util.Collection")
                    ) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean isJavaInternalType(TypeMirror type) {
        TypeName typeName = TypeName.get(type);

        return typeName.toString().startsWith("java.lang") || typeName.toString().startsWith("java.util");
    }

    public static boolean isPrimitiveOrWrapper(TypeMirror type) {
        TypeName typeName = TypeName.get(type);

        return typeName.isPrimitive() || typeName.isBoxedPrimitive();
    }

    public static TypeElement getEnclosedArrayElement(Element element, Elements elementUtils) {
        ArrayType arrayType = (ArrayType) element.asType();
        if (arrayType != null) {
            TypeName tName = TypeName.get(arrayType.getComponentType());
            if (tName != null) {
                return elementUtils.getTypeElement(tName.toString());
            }
        }
        return null;
    }
}
