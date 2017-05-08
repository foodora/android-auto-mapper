package de.foodora.android.automapper.model;

import android.support.annotation.Nullable;

import java.util.Date;

import de.foodora.automapper.ParcelAdapter;

public class ApiRestaurant {

    @Nullable
    public String name;

    @Nullable
    public String slogan;

    @ParcelAdapter(DateTypeAdapter.class)
    public Date foundationDate;

    public int branches;

    public ApiAddress address;
}
