package de.foodora.android.automapper.model.location;

import android.support.annotation.NonNull;

import de.foodora.android.automapper.model.location.gps.ApiGps;
import de.foodora.automapper.AutoMapper;

@AutoMapper(mapTo = "de.foodora.android.automapper.model.autogen.extrapackage.Area")
public class ApiArea {
    @NonNull
    public String name;
    @NonNull
    public ApiCity city;
    @NonNull
    public ApiGps location;
}
