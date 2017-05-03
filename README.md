# Notice
Based on the great work from :
https://github.com/aitorvs/auto-parcel


# Auto-Parcel-Map

A fast annotation processor to make your objects `Parcelable` without
writing any of the boilerplate.

The project is inspired in [auto-value-parcel](https://github.com/rharter/auto-value-parcel/)
extension and some of the code and utils are borrowed from it.

## Background

[Parcelable](https://developer.android.com/reference/android/os/Parcelable.html) classes
are great to put your objects into a [Bundle](https://developer.android.com/reference/android/os/Bundle.html)
and/or send them across an [Intent](https://developer.android.com/reference/android/content/Intent.html).
But there is a non-negligible boilerplate one has to write to make a class
parcelable and, let's be clear, nobody should ever need to write this code.
It is uninteresting code and highly prone to error.

There are a number of libraries out there to make your life simpler when
using parcelables. Some create [wrappers](https://github.com/johncarl81/parceler) around your object to parcel/unparcel,
some others generate code.
This is yet-another-parcelable-library that uses Android Studio
annotation processors to generate your parcelable classes at compile time.

## Installation
[![](https://jitpack.io/v/shehabic/auto-parcel-map.svg)](https://jitpack.io/#shehabic/auto-parcel-map)

### Repository

Add the following repo to your `app/build.gradle`

```gradle
repositories {
    maven { url "https://jitpack.io" }
}
```

### Dependencies

Add the following to gradle dependencies:

```gradle
dependencies {

    //... other dependencies here

    provided 'com.github.shehabic.auto-parcel-map:library:1.0.3'
    apt 'com.github.shehabic.auto-parcel-map:compiler:1.0.3'
}
```

The annotation processor requires [android-apt](https://bitbucket.org/hvisser/android-apt) plugin.

## Usage

The use of the library is very simple.
Just create an abstract `Parcelable`-to-be class, annotate it with `@AutoParcel` and
it will do the rest.

```java
import com.shehabic.autoparcel.AutoParcelMap;

@AutoParcelMap(map = ClassToMapFrom.class, prefix = "YourOptionalClassNamePrefix", finalName = "OrSimplyFinalClassName")
public abstract class MappingPerson implements Parcelable { }
```

AutoParcelMap will generate a parcelable class that extends from the abstract
class you created. The generated class name follows the convention `AutoParcel_<YouClassName>`.

You will need to add a convenience builder method (e.g. `creator`) that
calls the generated class constructor and that is all there is to it.

You are ready now to e.g. send an instance of `Person` across an intent

```
intent.putExtra(EXTRA_PERSON, (Parcelable) person);
```

To avoid the need to cast `Person` to `(Parcelable)` just add `implements Parcelable`
to your abstract class definition. AutoParcel will detect it and do the rest anyway.

```
@AutoParcelMap
public abstract class Person implements Parcelable {...}
```

It is important to note that `AutoParcel` errors out when **private** fields are found, because
they are not accessible from the generated class. Use either `protected` or `public` instead.

## Parcel Adapters

AutoParcelMap supports all types supported by [Parcel](https://developer.android.com/reference/android/os/Parcel.html)
with the exception of `Map` -- why? read [here](https://developer.android.com/reference/android/os/Parcel.html).

At times you will also need to parcel more complex types. For that, use `ParcelAdapter`s.
Let's see an example for a `Date` parcel adapter.

```java
import android.os.Parcel;
import com.shehabic.autoparcel.ParcelTypeAdapter;
import java.util.Date;

class DateTypeAdapter implements ParcelTypeAdapter<Date> {
    @Override
    public Date fromParcel(Parcel in) {
        return new Date(in.readLong());
    }

    @Override
    public void toParcel(Date value, Parcel dest) {
        dest.writeLong(value.getTime());
    }
}
```

Now you can use your adapter into your classes.

```java
import com.shehabic.autoparcel.AutoParcel;

@AutoParcelMap(finalName = "Person")
public abstract class MappedPerson {
    @Nullable
    public String name;
    @ParcelAdapter(DateTypeAdapter.class)
    public Date birthday;
    public int age;

    public static MappedPerson create(@NonNull String name, @NonNull Date birthday, int age) {
        return new Person(name, birthday, age);
    }
}
```

Parcel adapters are optional and the require the `ParcelTypeAdapter` runtime component.
To use them just add to your gradle the following dependency.

```
compile 'com.github.shehabic.auto-parcel:adapter:1.0.3'
```

## Version-able Parcels

**Use case**: your app issues a notification and within the pending intent, it parcels some model object.
Overtime, your model changes, adding some new fields, so you update the app. A user of your app has a notification
yet to be read but, before that happens, the app gets updated.
Now when your user opens the notification, the new version of the app will try to render from the `Parcel`
a different version of the of the object model...not good!

Using `@ParcelVersion` field annotation in combination with the `version`
optional parameter in `@AutoParcel` class annotation you can render data from `Parcel`s with different
versions.

Let's say we have an object model `Person`.

```java
@AutoParcelMap
public abstract class Person implements Parcelable {
    @NonNull
    public String name;

    @ParcelAdapter(DateTypeAdapter.class)
    public Date birthday;

    public int age;

    public static Person create(@NonNull String name, @NonNull Date birthday, int age) {
        return new AutoParcel_Person(name, birthday, age);
    }
}
```

At some point in our development, we decided that `Person` deserves also a field `lastName`, so we update the
model, add the field and release a new version of the app.
But we want our app to also render correctly previous object models (without the `lastName`)

Easy, just update the model like this:

```java
@AutoParcelMap(version = 1, prefix = "FD")
public abstract class Person implements Parcelable {
    @NonNull
    public String name;

    @ParcelVersion(from = 1)
    @Nullable
    public String lastName;

    @ParcelAdapter(DateTypeAdapter.class)
    public Date birthday;

    public int age;

    public static Person create(@NonNull String name, String lastName, @NonNull Date birthday, int age) {
        return new FDPerson(name, lastName, birthday, age);
    }
}
```

What we've done:
- Increase the `version` of our `Parcelable` object to 1 -- default version is 0
- Annotate the new field  with `@ParcelVersion(from = 1)` that indicates the field was added in version 1

The library will take care of the rest.

## Pitfalls

- Bootstrap is somehow annoying because when typing your first `AutoParcel_Foo`
it will turn red in the IDE until the first build is performed.
- Order of the constructor `AutoParcel_Foo` parameters is important and
according to their appearance in the source file.

## License

```
Copyright 2017 Mohamed Shehab Osman.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Copyright 2016 Aitor Viana Sánchez.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
