package de.foodora.android.automapper.model.location;

import de.foodora.android.automapper.model.location.gps.ApiGps;
import de.foodora.automapper.AutoMapper;

@AutoMapper(mapTo = "City")
public class ApiCity {
    public final String name;
    public final int id;
    public final ApiGps gps;

    public ApiCity(String name, int id, ApiGps gps) {
        this.name = name;
        this.id = id;
        this.gps = gps;
    }
}
