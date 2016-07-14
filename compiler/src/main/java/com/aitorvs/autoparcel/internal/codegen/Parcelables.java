package com.aitorvs.autoparcel.internal.codegen;

/*
 * Copyright (C) 13/07/16 aitorvs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

import java.util.Set;

import javax.lang.model.element.VariableElement;

/**
 * This class implementation has been based and refactored from <code>Parcelables</code> auto-value
 * extension implementation.
 *
 * https://github.com/rharter/auto-value-parcel/blob/master/auto-value-parcel/src/main/java/com/ryanharter/auto/value/parcel/Parcelables.java
 */
class Parcelables {
    private static final TypeName STRING = ClassName.get("java.lang", "String");
    private static final TypeName MAP = ClassName.get("java.util", "Map");
    private static final TypeName LIST = ClassName.get("java.util", "List");
    private static final TypeName BOOLEANARRAY = ArrayTypeName.of(boolean.class);
    private static final TypeName BYTEARRAY = ArrayTypeName.of(byte.class);
    private static final TypeName CHARARRAY = ArrayTypeName.of(char.class);
    private static final TypeName INTARRAY = ArrayTypeName.of(int.class);
    private static final TypeName LONGARRAY = ArrayTypeName.of(long.class);
    private static final TypeName STRINGARRAY = ArrayTypeName.of(String.class);
    private static final TypeName SPARSEARRAY = ClassName.get("android.util", "SparseArray");
    private static final TypeName SPARSEBOOLEANARRAY = ClassName.get("android.util", "SparseBooleanArray");
    private static final TypeName BUNDLE = ClassName.get("android.os", "Bundle");
    private static final TypeName PARCELABLE = ClassName.get("android.os", "Parcelable");
    private static final TypeName PARCELABLEARRAY = ArrayTypeName.of(PARCELABLE);
    private static final TypeName CHARSEQUENCE = ClassName.get("java.lang", "CharSequence");
    private static final TypeName IBINDER = ClassName.get("android.os", "IBinder");
    private static final TypeName OBJECTARRAY = ArrayTypeName.of(TypeName.OBJECT);
    private static final TypeName SERIALIZABLE = ClassName.get("java.io", "Serializable");
    private static final TypeName PERSISTABLEBUNDLE = ClassName.get("android.os", "PersistableBundle");
    private static final TypeName SIZE = ClassName.get("android.util", "Size");
    private static final TypeName SIZEF = ClassName.get("android.util", "SizeF");
    private static final TypeName TEXTUTILS = ClassName.get("android.text", "TextUtils");
    private static final TypeName ENUM = ClassName.get(Enum.class);

    private static final Set<TypeName> VALID_TYPES = ImmutableSet.of(STRING, MAP, LIST, BOOLEANARRAY,
            BYTEARRAY, CHARARRAY, INTARRAY, LONGARRAY, STRINGARRAY, SPARSEARRAY, SPARSEBOOLEANARRAY,
            BUNDLE, PARCELABLE, PARCELABLEARRAY, CHARSEQUENCE, IBINDER, OBJECTARRAY,
            SERIALIZABLE, PERSISTABLEBUNDLE, SIZE, SIZEF);

    static void readValue(CodeBlock.Builder block, VariableElement field, final TypeName parcelableType) {

        TypeName fieldType = TypeName.get(field.asType());

        if (parcelableType.equals(STRING)) {
            block.add("in.readString()");
        } else if (parcelableType.equals(TypeName.BYTE) || parcelableType.equals(TypeName.BYTE.box())) {
            block.add("in.readByte()");
        } else if (parcelableType.equals(TypeName.INT) || parcelableType.equals(TypeName.INT.box())) {
            block.add("in.readInt()");
        } else if (parcelableType.equals(TypeName.SHORT) || parcelableType.equals(TypeName.SHORT.box())) {
            block.add("(short) in.readInt()");
        } else if (parcelableType.equals(TypeName.CHAR) || parcelableType.equals(TypeName.CHAR.box())) {
            block.add("(char) in.readInt()");
        } else if (parcelableType.equals(TypeName.LONG) || parcelableType.equals(TypeName.LONG.box())) {
            block.add("in.readLong()");
        } else if (parcelableType.equals(TypeName.FLOAT) || parcelableType.equals(TypeName.FLOAT.box())) {
            block.add("in.readFloat()");
        } else if (parcelableType.equals(TypeName.DOUBLE) || parcelableType.equals(TypeName.DOUBLE.box())) {
            block.add("in.readDouble()");
        } else if (parcelableType.equals(TypeName.BOOLEAN) || parcelableType.equals(TypeName.BOOLEAN.box())) {
            block.add("in.readInt() == 1");
        } else if (parcelableType.equals(PARCELABLE)) {
//            if (fieldType.equals(PARCELABLE)) {
//                block.add("in.readParcelable($T.class.getClassLoader())",
//                        getParcelableComponent(types, property.element.getReturnType()));
//            } else {
//                block.add("($T) in.readParcelable($T.class.getClassLoader())", fieldType,
//                        getParcelableComponent(types, property.element.getReturnType()));
//            }
        } else if (parcelableType.equals(CHARSEQUENCE)) {
            block.add("$T.CHAR_SEQUENCE_CREATOR.createFromParcel(in)", TEXTUTILS);
        } else if (parcelableType.equals(MAP)) {
//            block.add("($T) in.readHashMap($T.class.getClassLoader())", fieldType,
//                    getParcelableComponent(types, property.element.getReturnType()));
        } else if (parcelableType.equals(LIST)) {
//            block.add("($T) in.readArrayList($T.class.getClassLoader())", fieldType,
//                    getParcelableComponent(types, property.element.getReturnType()));
        } else if (parcelableType.equals(BOOLEANARRAY)) {
            block.add("in.createBooleanArray()");
        } else if (parcelableType.equals(BYTEARRAY)) {
            block.add("in.createByteArray()");
        } else if (parcelableType.equals(CHARARRAY)) {
            block.add("in.createCharArray()");
        } else if (parcelableType.equals(STRINGARRAY)) {
            block.add("in.readStringArray()");
        } else if (parcelableType.equals(IBINDER)) {
            if (fieldType.equals(IBINDER)) {
                block.add("in.readStrongBinder()");
            } else {
                block.add("($T) in.readStrongBinder()", fieldType);
            }
        } else if (parcelableType.equals(OBJECTARRAY)) {
//            block.add("in.readArray($T.class.getClassLoader())",
//                    getParcelableComponent(types, property.element.getReturnType()));
        } else if (parcelableType.equals(INTARRAY)) {
            block.add("in.createIntArray()");
        } else if (parcelableType.equals(LONGARRAY)) {
            block.add("in.createLongArray()");
        } else if (parcelableType.equals(SERIALIZABLE)) {
            if (fieldType.equals(SERIALIZABLE)) {
                block.add("in.readSerializable()");
            } else {
                block.add("($T) in.readSerializable()", fieldType);
            }
        } else if (parcelableType.equals(PARCELABLEARRAY)) {
//            ArrayTypeName atype = (ArrayTypeName) fieldType;
//            if (atype.componentType.equals(PARCELABLE)) {
//                block.add("in.readParcelableArray($T.class.getClassLoader())",
//                        getParcelableComponent(types, property.element.getReturnType()));
//            } else {
//                block.add("($T) in.readParcelableArray($T.class.getClassLoader())", fieldType,
//                        getParcelableComponent(types, property.element.getReturnType()));
//            }
        } else if (parcelableType.equals(SPARSEARRAY)) {
//            block.add("in.readSparseArray($T.class.getClassLoader())",
//                    getParcelableComponent(types, property.element.getReturnType()));
        } else if (parcelableType.equals(SPARSEBOOLEANARRAY)) {
            block.add("in.readSparseBooleanArray()");
        } else if (parcelableType.equals(BUNDLE)) {
            block.add("in.readBundle($T.class.getClassLoader())", fieldType);
        } else if (parcelableType.equals(PERSISTABLEBUNDLE)) {
            block.add("in.readPersistableBundle($T.class.getClassLoader())", fieldType);
        } else if (parcelableType.equals(SIZE)) {
            block.add("in.readSize()");
        } else if (parcelableType.equals(SIZEF)) {
            block.add("in.readSizeF()");
        } else if (parcelableType.equals(ENUM)) {
            block.add("$T.valueOf(in.readString())", fieldType);
        } else {
//            block.add("($T) in.readValue($T.class.getClassLoader())", fieldType,
//                    getParcelableComponent(types, property.element.getReturnType()));
        }
    }

    public static CodeBlock writeValue(VariableElement field, ParameterSpec out, ParameterSpec flags) {
        CodeBlock.Builder block = CodeBlock.builder();

        TypeName type = TypeName.get(field.asType());
        if (type.equals(STRING))
            block.add("$N.writeString($N)", out, field.getSimpleName());
        else if (type.equals(TypeName.BYTE) || type.equals(TypeName.BYTE.box()))
            block.add("$N.writeInt($N)", out, field.getSimpleName());
        else if (type.equals(TypeName.INT) || type.equals(TypeName.INT.box()))
            block.add("$N.writeInt($N)", out, field.getSimpleName());
        else if (type.equals(TypeName.SHORT))
            block.add("$N.writeInt(((Short) $N).intValue())", out, field.getSimpleName());
        else if (type.equals(TypeName.SHORT.box()))
            block.add("$N.writeInt($N.intValue())", out, field.getSimpleName());
        else if (type.equals(TypeName.CHAR) || type.equals(TypeName.CHAR.box()))
            block.add("$N.writeInt($N)", out, field.getSimpleName());
        else if (type.equals(TypeName.LONG) || type.equals(TypeName.LONG.box()))
            block.add("$N.writeLong($N)", out, field.getSimpleName());
        else if (type.equals(TypeName.FLOAT) || type.equals(TypeName.FLOAT.box()))
            block.add("$N.writeFloat($N)", out, field.getSimpleName());
        else if (type.equals(TypeName.DOUBLE) || type.equals(TypeName.DOUBLE.box()))
            block.add("$N.writeDouble($N)", out, field.getSimpleName());
        else if (type.equals(TypeName.BOOLEAN) || type.equals(TypeName.BOOLEAN.box()))
            block.add("$N.writeInt($N ? 1 : 0)", out, field.getSimpleName());
        else if (type.equals(PARCELABLE))
            block.add("$N.writeParcelable($N, $N)", out, field.getSimpleName(), flags);
        else if (type.equals(CHARSEQUENCE))
            block.add("$T.writeToParcel($N, $N, $N)", TEXTUTILS, field.getSimpleName(), out, flags);
        else if (type.equals(MAP))
            block.add("$N.writeMap($N)", out, field.getSimpleName());
        else if (type.equals(LIST))
            block.add("$N.writeList($N)", out, field.getSimpleName());
        else if (type.equals(BOOLEANARRAY))
            block.add("$N.writeBooleanArray($N)", out, field.getSimpleName());
        else if (type.equals(BYTEARRAY))
            block.add("$N.writeByteArray($N)", out, field.getSimpleName());
        else if (type.equals(CHARARRAY))
            block.add("$N.writeCharArray($N)", out, field.getSimpleName());
        else if (type.equals(STRINGARRAY))
            block.add("$N.writeStringArray($N)", out, field.getSimpleName());
        else if (type.equals(IBINDER))
            block.add("$N.writeStrongBinder($N)", out, field.getSimpleName());
        else if (type.equals(OBJECTARRAY))
            block.add("$N.writeArray($N)", out, field.getSimpleName());
        else if (type.equals(INTARRAY))
            block.add("$N.writeIntArray($N)", out, field.getSimpleName());
        else if (type.equals(LONGARRAY))
            block.add("$N.writeLongArray($N)", out, field.getSimpleName());
        else if (type.equals(SERIALIZABLE))
            block.add("$N.writeSerializable($N)", out, field.getSimpleName());
        else if (type.equals(PARCELABLEARRAY))
            block.add("$N.writeParcelableArray($N)", out, field.getSimpleName());
        else if (type.equals(SPARSEARRAY))
            block.add("$N.writeSparseArray($N)", out, field.getSimpleName());
        else if (type.equals(SPARSEBOOLEANARRAY))
            block.add("$N.writeSparseBooleanArray($N)", out, field.getSimpleName());
        else if (type.equals(BUNDLE))
            block.add("$N.writeBundle($N)", out, field.getSimpleName());
        else if (type.equals(PERSISTABLEBUNDLE))
            block.add("$N.writePersistableBundle($N)", out, field.getSimpleName());
        else if (type.equals(SIZE))
            block.add("$N.writeSize($N)", out, field.getSimpleName());
        else if (type.equals(SIZEF))
            block.add("$N.writeSizeF($N)", out, field.getSimpleName());
        else if (type.equals(ENUM))
            block.add("$N.writeString($N.name())", out, field.getSimpleName());
        else
            block.add("$N.writeValue($N)", out, field.getSimpleName());

        block.add(";\n");
        return block.build();
    }

    static boolean isTypeRequiresSuppressWarnings(TypeName type) {
        return type.equals(LIST) ||
                type.equals(MAP);
    }
}