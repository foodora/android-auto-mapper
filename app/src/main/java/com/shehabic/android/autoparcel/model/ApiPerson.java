package com.shehabic.android.autoparcel.model;

import android.support.annotation.Nullable;

import com.shehabic.autoparcel.ParcelAdapter;

import java.util.Date;

/**
 * Created by shehabic on 02.05.17.
 */

public class ApiPerson {

    @Nullable
    public String name;

    @Nullable
    public String lastName;

    @ParcelAdapter(DateTypeAdapter.class)
    public Date birthday;

    public int age;

    // this is another parcelable object
    public Address address;
}
