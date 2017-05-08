package de.foodora.automapper.internal.codegen;

import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.NameAllocator;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import de.foodora.automapper.AutoMapper;
import de.foodora.automapper.ParcelAdapter;
import de.foodora.automapper.ParcelVersion;
import de.foodora.automapper.internal.codegen.dependencygraph.DependencySolver;
import de.foodora.automapper.internal.common.MoreElements;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

@SupportedAnnotationTypes("de.foodora.automapper.AutoMapper")
public final class AutoMappperProcessor extends AbstractProcessor {
    private ErrorReporter mErrorReporter;
    private Types mTypeUtils;
    private DependencySolver dependencyResolver;

    static final class Property {
        final String fieldName;
        final VariableElement element;
        final TypeName typeName;
        final ImmutableSet<String> annotations;
        final int version;
        final boolean isMapped;
        TypeMirror typeAdapter;

        Property(String fieldName, TypeName typeName, VariableElement element, boolean isMapped) {
            this.fieldName = fieldName;
            this.element = element;
            this.typeName = typeName;
            this.annotations = getAnnotations(element);
            this.isMapped = isMapped;

            // get the parcel adapter if any
            ParcelAdapter parcelAdapter = element.getAnnotation(ParcelAdapter.class);
            if (parcelAdapter != null) {
                try {
                    parcelAdapter.value();
                } catch (MirroredTypeException e) {
                    this.typeAdapter = e.getTypeMirror();
                }
            }

            // get the element version, default 0
            ParcelVersion parcelVersion = element.getAnnotation(ParcelVersion.class);
            this.version = parcelVersion == null ? 0 : parcelVersion.from();
        }

        Property(String fieldName, VariableElement element) {
            this(fieldName, TypeName.get(element.asType()), element, false);
        }

        public boolean isNullable() {
            return this.annotations.contains("Nullable");
        }

        public int version() {
            return this.version;
        }

        private ImmutableSet<String> getAnnotations(VariableElement element) {
            ImmutableSet.Builder<String> builder = ImmutableSet.builder();
            for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
                builder.add(annotation.getAnnotationType().asElement().getSimpleName().toString());
            }

            return builder.build();
        }
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mErrorReporter = new ErrorReporter(processingEnv);
        mTypeUtils = processingEnv.getTypeUtils();
        dependencyResolver = new DependencySolver(processingEnv);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        Collection<? extends Element> annotatedElements = env.getElementsAnnotatedWith(AutoMapper.class);
        List<TypeElement> types = new ImmutableList.Builder<TypeElement>()
                .addAll(ElementFilter.typesIn(annotatedElements))
                .build();
        Set<TypeElement> mappedElements = new HashSet<>();
        Map<TypeElement, String> elementTargetNames = new HashMap<>();
        Map<TypeElement, TypeElement> mapFromExtends = new HashMap<>();
        TypeElement mapperElement = null;
        for (TypeElement type : types) {
            TypeElement mapFrom = getClassToMapFrom(type);
            if (mapFrom != null) {
                AutoMapper autoMapper = type.getAnnotation(AutoMapper.class);
                if (autoMapper.extendMapper()) {
                    mapFromExtends.put(mapFrom, type);
                }
                elementTargetNames.put(mapFrom, getMappingTargetFullClassName(type, mapFrom));
                mappedElements.add(mapFrom);
                mapperElement = type;
            }
        }
        List<TypeElement> topologicalMappedElements = dependencyResolver.resolveAllDependencies(mappedElements);
        completeElementTargetName(mapperElement, topologicalMappedElements, elementTargetNames);

        if (topologicalMappedElements.size() > 0) {
            processMappingElements(elementTargetNames, mapFromExtends, topologicalMappedElements, types.get(0));
        } else {
            for (TypeElement type : types) {
                processType(type, new HashMap<>());
            }
        }

        // We are the only ones handling AutoParcel annotations
        return true;
    }

    private void processMappingElements(
        Map<TypeElement, String> elementTargetNames,
        Map<TypeElement, TypeElement> mapFromExtends,
        List<TypeElement> topologicalMappedElements,
        TypeElement baseElement
    ) {
        for (TypeElement mappedElement : topologicalMappedElements) {
            TypeElement extendElement = mapFromExtends.get(mappedElement);
            processMapping(
                extendElement != null ? extendElement : baseElement,
                mappedElement,
                elementTargetNames.get(mappedElement),
                (extendElement != null ? extendElement.asType().toString() : null),
                elementTargetNames
            );
        }
    }

    private void processMapping(
        TypeElement baseElement,
        TypeElement mapFrom,
        String targetName,
        String extend,
        Map<TypeElement, String> elementTargetNames
    ) {
        if (extend != null) {
            checkModifiersIfNested(baseElement);
        }
        String source = generateClass(baseElement, TypeUtil.simpleNameOf(targetName), extend, mapFrom, elementTargetNames);
        source = Reformatter.fixup(source);
        writeSourceFile(targetName, source, baseElement);
    }

    private void processType(TypeElement type, Map<TypeElement, String> elementTargetNames) {
        TypeElement mapFrom = getClassToMapFrom(type);
        AutoMapper autoMapper = type.getAnnotation(AutoMapper.class);
        if (autoMapper == null) {
            mErrorReporter.abortWithError(
                "annotation processor for @AutoParcel was invoked with a type annotated differently; compiler bug? O_o",
                type
            );
        }
        if (type.getKind() != ElementKind.CLASS) {
            mErrorReporter.abortWithError("@" + AutoMapper.class.getName() + " only applies to classes", type);
        }
        if (ancestorIsAutoParcel(type)) {
            mErrorReporter.abortWithError("One @AutoParcel class shall not extend another", type);
        }

        checkModifiersIfNested(type);

        // get the fully-qualified class name
        String fqClassName = generatedSubclassName(type, 0);
        // class name
        String className = TypeUtil.simpleNameOf(fqClassName);

        String source = generateClass(type, className, type.getSimpleName().toString(), mapFrom, elementTargetNames);
        source = Reformatter.fixup(source);
        writeSourceFile(fqClassName, source, type);

    }

    private void writeSourceFile(String className, String text, TypeElement originatingType) {
        try {
            JavaFileObject sourceFile =
                    processingEnv.getFiler().createSourceFile(className, originatingType);
            Writer writer = sourceFile.openWriter();
            try {
                writer.write(text);
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            // This should really be an error, but we make it a warning in the hope of resisting Eclipse
            // bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=367599. If that bug manifests, we may get
            // invoked more than once for the same file, so ignoring the ability to overwrite it is the
            // right thing to do. If we are unable to write for some other reason, we should get a compile
            // error later because user code will have a reference to the code we were supposed to
            // generate (new AutoValue_Foo() or whatever) and that reference will be undefined.
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                    "Could not write generated class " + className + ": " + e);
        }
    }

    private boolean hasCustomMappingMethod(TypeElement type) {
        List<ExecutableElement> methods = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement method : methods) {
            if (method.getSimpleName().toString().equals("map")) {
               return true;
            }
        }

        return false;
    }

    private TypeElement getClassToMapFrom(TypeElement type) {
        TypeMirror map = getMap(type.getAnnotation(AutoMapper.class));
        if (map != null && map.getClass() != null && !map.getClass().getCanonicalName().equals(void.class.getCanonicalName())) {
            return (TypeElement) processingEnv.getTypeUtils().asElement(map);
        }

        return null;
    }

    private String generateClass(TypeElement type, String className, String classToExtend, TypeElement mapFrom, Map<TypeElement, String> elementTargetNames) {
        if (type == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null type", type);
        }
        if (className == null) {
            mErrorReporter.abortWithError("generateClass was invoked with null class name", type);
        }
        List<VariableElement> nonPrivateFields = new ArrayList<>();
        List<VariableElement> mappedOnlyFields = new ArrayList<>();
        if (classToExtend != null) {
            addNonPrivateFields(type, nonPrivateFields);
        }

        if (mapFrom != null) {
            addNonPrivateFields(mapFrom, mappedOnlyFields);
            nonPrivateFields.addAll(mappedOnlyFields);
        }

        if (nonPrivateFields.isEmpty()) {
            mErrorReporter.abortWithError("generateClass error, all fields are declared PRIVATE", type);
        }

        // get the properties
        ImmutableList<Property> properties = buildProperties(nonPrivateFields, elementTargetNames);

        ImmutableList<Property> mappedProperties = buildProperties(mappedOnlyFields, elementTargetNames);

        // get the type adapters
        ImmutableMap<TypeMirror, FieldSpec> typeAdapters = getTypeAdapters(properties);

        // get the parcel version
        //noinspection ConstantConditions
//        int version = type.getAnnotation(AutoMapper.class).version();
        boolean isImplementingParcelable = ancestoIsParcelable(processingEnv, type);
        boolean isParcelable = isImplementingParcelable || type.getAnnotation(AutoMapper.class).parcelable();

        // Generate the AutoParcel_??? class
        String pkg = generatePackageName(type, elementTargetNames.get(mapFrom));
        TypeName classTypeName = ClassName.get(pkg, className);
        TypeSpec.Builder subClass = TypeSpec.classBuilder(className)
                // Class must be always final
                .addModifiers(new Modifier[]{FINAL, PUBLIC})
                // Add the DEFAULT constructor
                .addMethod(generateConstructor(properties))
                // create empty constructor
                .addMethod(MethodSpec.constructorBuilder().addModifiers(PUBLIC).build())
                // Add fields from mapping only
                .addFields(generateFieldSpecs(properties/*, elementTargetNames*/))
                // Add mapFrom from constructor
                .addMethod(
                    generateMapFromCreator(
                        classToExtend != null ? type : null,
                        classTypeName,
                        mapFrom,
                        mappedProperties,
                        elementTargetNames
                    )
                );

        if (classToExtend != null) {
            // extends from original abstract class
            subClass.superclass(ClassName.get(pkg, classToExtend));
        }

        if (isParcelable) {
            subClass
                // Add the private constructor
                .addMethod(generateConstructorFromParcel(processingEnv, properties, typeAdapters))
                // overrides describeContents()
                .addMethod(generateDescribeContents())
                // static final CREATOR
                .addField(generateCreator(processingEnv, properties, classTypeName, typeAdapters))
                // overrides writeToParcel()
                .addMethod(generateWriteToParcel(0, processingEnv, properties, typeAdapters))
            ;

            subClass.addSuperinterface(ClassName.get("android.os", "Parcelable"));
        }

        if (!typeAdapters.isEmpty()) {
            typeAdapters.values().forEach(subClass::addField);
        }

        JavaFile javaFile = JavaFile.builder(pkg, subClass.build()).build();

        return javaFile.toString();
    }

    private String generatePackageName(TypeElement type, String targetName) {
        return targetName != null ? ClassName.bestGuess(targetName).packageName() : TypeUtil.packageNameOf(type);
    }

    private ImmutableMap<TypeMirror, FieldSpec> getTypeAdapters(ImmutableList<Property> properties) {
        Map<TypeMirror, FieldSpec> typeAdapters = new LinkedHashMap<>();
        NameAllocator nameAllocator = new NameAllocator();
        nameAllocator.newName("CREATOR");
        for (Property property : properties) {
            if (property.typeAdapter != null && !typeAdapters.containsKey(property.typeAdapter)) {
                ClassName typeName = (ClassName) TypeName.get(property.typeAdapter);
                String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, typeName.simpleName());
                name = nameAllocator.newName(name, typeName);

                typeAdapters.put(property.typeAdapter, FieldSpec.builder(
                        typeName, NameAllocator.toJavaIdentifier(name), PRIVATE, STATIC, FINAL)
                        .initializer("new $T()", typeName).build());
            }
        }
        return ImmutableMap.copyOf(typeAdapters);
    }

    private ImmutableList<Property> buildProperties(List<VariableElement> elements, Map<TypeElement, String> elementTargetNames) {
        ImmutableList.Builder<Property> builder = ImmutableList.builder();
        String fieldName;
        TypeElement typeElement;
        for (VariableElement element : elements) {
            fieldName = element.getSimpleName().toString();
            typeElement = processingEnv.getElementUtils().getTypeElement(element.asType().toString());
            if (TypeUtil.isIterable(element)) {
                Element enclosedElement = TypeUtil.getGenericElement(element, processingEnv.getElementUtils());
                if (enclosedElement instanceof TypeElement && elementTargetNames.containsKey(enclosedElement)) {
                    TypeName rawType = TypeUtil.getRawTypeOfIterable(element);
                    ClassName collection = ClassName.bestGuess(rawType.toString());
                    String target = elementTargetNames.get(enclosedElement);
                    if (target != null) {
                        TypeName finalCollection = ParameterizedTypeName.get(collection, ClassName.bestGuess(target));
                        builder.add(new Property(fieldName, finalCollection, element, true));
                        continue;
                    }
                }
            } else if (TypeUtil.isArray(element)) {
                Element enclosedElement = TypeUtil.getEnclosedArrayElement(element, processingEnv.getElementUtils());
                if (enclosedElement != null && elementTargetNames.containsKey(enclosedElement)) {
                    ClassName className = ClassName.bestGuess(elementTargetNames.get(enclosedElement));
                    ArrayTypeName mappedArray = ArrayTypeName.of(className);
                    builder.add(new Property(fieldName, mappedArray, element, true));
                    continue;
                }
            } else if (elementTargetNames.containsKey(typeElement)) {
                Element mapped = processingEnv.getElementUtils().getTypeElement(elementTargetNames.get(typeElement));
                builder.add(
                    new Property(
                        fieldName,
                        (mapped != null ? TypeName.get(mapped.asType()) : ClassName.bestGuess(elementTargetNames.get(typeElement))),
                        (mapped != null ? (VariableElement) mapped : element),
                        true
                    )
                );
                continue;
            }
            builder.add(new Property(fieldName, element));
        }

        return builder.build();
    }

    /**
     * This method returns a list of all non private fields. If any <code>private</code> fields is
     * found, the method errors out
     *
     * @param type element
     * @return list of all non-<code>private</code> fields
     */
    private List<VariableElement> getParcelableFieldsOrError(TypeElement type) {
        List<VariableElement> allFields = ElementFilter.fieldsIn(type.getEnclosedElements());
        List<VariableElement> nonPrivateFields = new ArrayList<>();

        for (VariableElement field : allFields) {
            if (!field.getModifiers().contains(PRIVATE)) {
                nonPrivateFields.add(field);
            } else {
                // return error, PRIVATE fields are not allowed
                mErrorReporter.abortWithError("getFieldsError error, PRIVATE fields not allowed", type);
            }
        }

        return nonPrivateFields;
    }

    private MethodSpec generateConstructor(ImmutableList<Property> properties) {

        List<ParameterSpec> params = Lists.newArrayListWithCapacity(properties.size());
        for (Property property : properties) {
            params.add(ParameterSpec.builder(property.typeName, property.fieldName).build());
        }

        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(PUBLIC)
                .addParameters(params);

        for (ParameterSpec param : params) {
            builder.addStatement("this.$N = $N", param.name, param.name);
        }

        return builder.build();
    }

    private MethodSpec generateConstructorFromParcel(
            ProcessingEnvironment env,
            ImmutableList<Property> properties,
            ImmutableMap<TypeMirror, FieldSpec> typeAdapters) {

        // Create the PRIVATE constructor from Parcel
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(PRIVATE)      // private
                .addParameter(ClassName.bestGuess("android.os.Parcel"), "in"); // input param

        // get a code block builder
        CodeBlock.Builder block = CodeBlock.builder();

//        // First thing is reading the Parcelable object version
//        block.add("this.version = in.readInt();\n");

        // Now, iterate all properties, check the version initialize them
        for (Property p : properties) {

//            // get the property version
//            int pVersion = p.version();
//            if (pVersion > 0) {
//                block.beginControlFlow("if (this.version >= $L)", pVersion);
//            }

            block.add("this.$N = ", p.fieldName);

            if (p.typeAdapter != null && typeAdapters.containsKey(p.typeAdapter)) {
                Parcelables.readValueWithTypeAdapter(block, p, typeAdapters.get(p.typeAdapter));
            } else {
                TypeName parcelableType = Parcelables.getTypeNameFromProperty(p, env.getTypeUtils());
                if (parcelableType == null) {
                    mErrorReporter.abortWithError("could not create parcelable for type " + p.typeName, p.element);
                }
                Parcelables.readValue(block, p, parcelableType);
            }

            block.add(";\n");

//            if (pVersion > 0) {
//                block.endControlFlow();
//            }
        }

        builder.addCode(block.build());

        return builder.build();
    }

    private String generatedSubclassName(TypeElement type, int depth) {
        String prefix = type.getAnnotation(AutoMapper.class).prefix();
        String finalName = type.getAnnotation(AutoMapper.class).mapTo();
        String typeName = type.getSimpleName().toString();
        String name = prefix.trim() + typeName;
        if (finalName.equals(name)) {
            mErrorReporter.abortWithError("You need to specify a different final name or a prefix", type);
        }

        return generatedClassName(type, Strings.repeat("$", depth) + (finalName.trim().length() > 0 ? finalName : name));
    }

    private String generatedClassName(TypeElement type, String name) {
        while (type.getEnclosingElement() instanceof TypeElement) {
            type = (TypeElement) type.getEnclosingElement();
            name = type.getSimpleName() + name;
        }
        String pkg = TypeUtil.packageNameOf(type);
        String dot = pkg.isEmpty() ? "" : ".";
        return pkg + dot + name;
    }

    private MethodSpec generateWriteToParcel(
        int version,
        ProcessingEnvironment env,
        ImmutableList<Property> properties,
        ImmutableMap<TypeMirror, FieldSpec> typeAdapters
    ) {
        ParameterSpec dest = ParameterSpec
                .builder(ClassName.get("android.os", "Parcel"), "dest")
                .build();
        ParameterSpec flags = ParameterSpec.builder(int.class, "flags").build();
        MethodSpec.Builder builder = MethodSpec.methodBuilder("writeToParcel")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(dest)
                .addParameter(flags);
//
//        // write first the parcelable object version...
//        builder.addCode(Parcelables.writeVersion(version, dest));

        // ...then write all the properties
        for (Property p : properties) {
            if (p.typeAdapter != null && typeAdapters.containsKey(p.typeAdapter)) {
                FieldSpec typeAdapter = typeAdapters.get(p.typeAdapter);
                builder.addCode(Parcelables.writeValueWithTypeAdapter(typeAdapter, p, dest));
            } else {
                builder.addCode(Parcelables.writeValue(p, dest, flags, env.getTypeUtils()));
            }
        }

        return builder.build();
    }

    private MethodSpec generateDescribeContents() {
        return MethodSpec.methodBuilder("describeContents")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .returns(int.class)
                .addStatement("return 0")
                .build();
    }

    private FieldSpec generateCreator(
        ProcessingEnvironment env,
        ImmutableList<Property> properties,
        TypeName type,
        ImmutableMap<TypeMirror, FieldSpec> typeAdapters
    ) {
        ClassName creator = ClassName.bestGuess("android.os.Parcelable.Creator");
        TypeName creatorOfClass = ParameterizedTypeName.get(creator, type);

        Types typeUtils = env.getTypeUtils();
        CodeBlock.Builder ctorCall = CodeBlock.builder();
        boolean requiresSuppressWarnings = false;
        ctorCall.add("return new $T(in);\n", type);

        // Method createFromParcel()
        MethodSpec.Builder createFromParcel = MethodSpec.methodBuilder("createFromParcel")
                .addAnnotation(Override.class);
        if (requiresSuppressWarnings) {
            createFromParcel.addAnnotation(createSuppressUncheckedWarningAnnotation());
        }
        createFromParcel
                .addModifiers(PUBLIC)
                .returns(type)
                .addParameter(ClassName.bestGuess("android.os.Parcel"), "in");
        createFromParcel.addCode(ctorCall.build());

        TypeSpec creatorImpl = TypeSpec.anonymousClassBuilder("")
                .superclass(creatorOfClass)
                .addMethod(createFromParcel
                        .build())
                .addMethod(MethodSpec.methodBuilder("newArray")
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .returns(ArrayTypeName.of(type))
                        .addParameter(int.class, "size")
                        .addStatement("return new $T[size]", type)
                        .build())
                .build();

        return FieldSpec
                .builder(creatorOfClass, "CREATOR", PUBLIC, FINAL, STATIC)
                .initializer("$L", creatorImpl)
                .build();
    }

    private void checkModifiersIfNested(TypeElement type) {
        ElementKind enclosingKind = type.getEnclosingElement().getKind();
        if (enclosingKind.isClass() || enclosingKind.isInterface()) {
            if (type.getModifiers().contains(PRIVATE)) {
                mErrorReporter.abortWithError("@AutoParcel class must not be private", type);
            }
            if (!type.getModifiers().contains(STATIC)) {
                mErrorReporter.abortWithError("Nested @AutoParcel class must be static", type);
            }
        }
        // In principle type.getEnclosingElement() could be an ExecutableElement (for a class
        // declared inside a method), but since RoundEnvironment.getElementsAnnotatedWith doesn't
        // return such classes we won't see them here.
    }

    private boolean ancestorIsAutoParcel(TypeElement type) {
        while (true) {
            TypeMirror parentMirror = type.getSuperclass();
            if (parentMirror.getKind() == TypeKind.NONE) {
                return false;
            }
            TypeElement parentElement = (TypeElement) mTypeUtils.asElement(parentMirror);
            if (MoreElements.isAnnotationPresent(parentElement, AutoMapper.class)) {
                return true;
            }
            type = parentElement;
        }
    }

    private boolean ancestoIsParcelable(ProcessingEnvironment env, TypeElement type) {
        // TODO: 15/07/16 check recursively
        TypeMirror classType = type.asType();
        TypeMirror parcelable = env.getElementUtils().getTypeElement("android.os.Parcelable").asType();
        return TypeUtil.isClassOfType(env.getTypeUtils(), parcelable, classType);
    }

    private static AnnotationSpec createSuppressUncheckedWarningAnnotation() {
        return AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "\"unchecked\"")
                .build();
    }

    private Iterable<FieldSpec> generateFieldSpecs(ImmutableList<Property> properties) {
        List<FieldSpec> fields = new ArrayList<>();
        for (Property property : properties) {
            fields.add(FieldSpec.builder(property.typeName, property.fieldName, new Modifier[]{PUBLIC}).build());
        }

        return fields;
    }

    private MethodSpec generateMapFromCreator(
        TypeElement classToExtend,
        TypeName typeName,
        TypeElement source,
        Iterable<Property> params,
        Map<TypeElement, String> elementTargetNames
    ) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("mapFrom")
            .addModifiers(STATIC, PUBLIC, FINAL)
            .returns(typeName);

        if (source != null) {
            builder.addParameter(ClassName.bestGuess(source.toString()), "source");

            builder.addStatement("$T mapped = new $T()", typeName, typeName);
            for (Property param : params) {
                if (param.isMapped) {
                    if (TypeUtil.isIterable(param.element)) {
                        Element enclosedElement = TypeUtil.getGenericElement(param.element, processingEnv.getElementUtils());
                        if (enclosedElement instanceof TypeElement && elementTargetNames.containsKey(enclosedElement)) {
                            String target = elementTargetNames.get(enclosedElement);
                            TypeName rawType = TypeUtil.getRawTypeOfIterable(param.element);
                            String collectionName = TypeUtil.simpleNameOf(rawType.toString());
                            if (target != null) {
                                TypeName enclosedMapperType = ClassName.bestGuess(target);
                                builder.addStatement("mapped.$N = $T.$N()", param.fieldName, ClassName.bestGuess("java.util.Collections"), getEmptyIteratorMethodName(collectionName));
                                builder.beginControlFlow("if (source.$N != null)", param.fieldName);
                                builder.beginControlFlow("for ($T item : source.$N)", enclosedElement.asType(), param.fieldName);
                                builder.addStatement("mapped.$N.add($T.mapFrom(item))",param.fieldName, enclosedMapperType);
                                builder.endControlFlow();
                                builder.endControlFlow();
                                continue;
                            }
                        }
                    } else if (TypeUtil.isArray(param.element)) {
                        Element enclosedElement = TypeUtil.getEnclosedArrayElement(param.element, processingEnv.getElementUtils());
                        if (enclosedElement != null && elementTargetNames.containsKey(enclosedElement)) {
                            ClassName className = ClassName.bestGuess(elementTargetNames.get(enclosedElement));
                            builder.beginControlFlow("if (source.$N != null)", param.fieldName);
                            builder.addStatement("mapped.$N = new $T[source.$N.length]", param.fieldName, className, param.fieldName);
                            builder.beginControlFlow("for (int i = 0; i < source.$N.length; i++)", param.fieldName);
                            builder.addStatement("mapped.$N[i] = $T.mapFrom(source.$N[i])", param.fieldName, className, param.fieldName);
                            builder.endControlFlow();
                            builder.nextControlFlow("else");
                            builder.addStatement("mapped.$N = null", param.fieldName);
                            builder.endControlFlow();
                            continue;
                        }
                    }
                    builder.addStatement("mapped.$N = $T.mapFrom(source.$N)",param.fieldName, param.typeName, param.fieldName);
                } else {
                    builder.addStatement("mapped.$N = source.$N", param.fieldName, param.fieldName);
                }
            }
            if (classToExtend != null && hasCustomMappingMethod(classToExtend)) {
                builder.addStatement("mapped.map(mapped)");
            }

            builder.addStatement("return mapped");
        } else {
            builder.addStatement("return null");
        }

        return builder.build();
    }

    private String getEmptyIteratorMethodName(String collectionName) {
        switch (collectionName) {
            case "List":
            case "Collection":
                return "emptyList";

            case "Set":
                return "emptySet";

            case "ArrayList":
                return "ArrayList";

            case "Map":
                return "emptyMap";

            default:
                throw new RuntimeException("Collection type `" + collectionName + "` not supported");
        }


    }

    private static TypeMirror getMap(AutoMapper annotation) {
        try {
            annotation.mapFrom(); // this should throw
        } catch (MirroredTypeException mte) {
            return mte.getTypeMirror();
        } catch (Exception ignore) {
        }

        return null; // can this ever happen ??
    }

    private void addNonPrivateFields(TypeElement element, List<VariableElement> nonPrivateFields) {
        if (hasSuperClass(element)) {
            Types typeUtils = processingEnv.getTypeUtils();
            Element elm = typeUtils.asElement(element.getSuperclass());
            addNonPrivateFields((TypeElement) elm, nonPrivateFields);
        }
        nonPrivateFields.addAll(getParcelableFieldsOrError(element));
    }

    private boolean hasSuperClass(TypeElement element) {
        return !element.getSuperclass().toString().equals(Object.class.getCanonicalName());
    }

    private void completeElementTargetName(TypeElement baseElement, List<TypeElement> elements, Map<TypeElement, String> targetNames) {
        if (elements.size() == 0) {
            return;
        }
        if (baseElement == null) {
            mErrorReporter.abortWithError("You need to specify at least one element in mapTo, elements = " + elements.size(), null);

            return;
        }
        for (TypeElement elm : elements) {
            if (!targetNames.containsKey(elm)) {
                targetNames.put(elm, getMappingTargetFullClassName(baseElement, elm));
            }
        }
    }

    private String getMappingTargetFullClassName(TypeElement base, TypeElement mapFrom) {
        AutoMapper mapperAnnotation = base.getAnnotation(AutoMapper.class);
        AutoMapper mapFromAnnotation = mapFrom.getAnnotation(AutoMapper.class);
        TypeElement mapperGetFrom = getClassToMapFrom(base);
        boolean isBaseMapperForTarget
            = mapperGetFrom != null && mapperGetFrom.asType().toString().equals(mapFrom.asType().toString());

        if (isBaseMapperForTarget && mapperAnnotation.mapTo().length() > 0) {
            return mapperAnnotation.mapTo().contains(".")
                ? mapperAnnotation.mapTo()
                : TypeUtil.packageNameOf(base) + "." + mapperAnnotation.mapTo();
        } else if (mapFromAnnotation != null && mapFromAnnotation.mapTo().length() > 0) {
            return mapFromAnnotation.mapTo().contains(".")
                ? mapFromAnnotation.mapTo()
                : TypeUtil.packageNameOf(base) + "." + mapFromAnnotation.mapTo();
        }
        if (mapperAnnotation == null) {
            System.out.println("Null annotation for " + base.toString());
        }

        return ClassName.get(base).packageName() + "." + mapperAnnotation.prefix() + ClassName.get(mapFrom).simpleName();
    }
}
