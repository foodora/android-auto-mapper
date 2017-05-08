package de.foodora.android.automapper.model;

/*
 * Copyright (C) 03/05/17 Foodora GmbH
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

import android.support.annotation.NonNull;

import de.foodora.android.automapper.model.location.ApiArea;

public class ApiAddress {
    @NonNull
    public String street;

    public String postCode;

    public String city;

    public String country;

    public ApiArea area;
}
