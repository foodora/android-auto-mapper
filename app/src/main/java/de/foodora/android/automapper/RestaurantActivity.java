package de.foodora.android.automapper;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.TextView;

import de.foodora.android.automapper.model.Restaurant;
import de.foodora.android.automapper.model.RestaurantAutoMapper;

public class RestaurantActivity extends AppCompatActivity {

    private static final String EXTRA_PERSON = "EXTRA_PERSON";

    @Nullable
    public static Intent createIntent(@NonNull Context context, RestaurantAutoMapper person) {
        //noinspection ConstantConditions
        if (context == null) {
            return null;
        }
        Intent intent = new Intent(context, RestaurantActivity.class);
        // we need to cast it to Parcelable because Person does not itself implement parcelable
        intent.putExtra(EXTRA_PERSON, (Parcelable) person);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);

        TextView fullName = (TextView) findViewById(R.id.fullName);
        TextView date = (TextView) findViewById(R.id.dateOfBirth);
        TextView age = (TextView) findViewById(R.id.age);
        TextView fullAddress = (TextView) findViewById(R.id.fullAddress);

        // get the passed intent
        Intent intent = getIntent();
        if (intent != null) {
            Restaurant restaurant = intent.getParcelableExtra(EXTRA_PERSON);
            fullName.setText(getString(R.string.formatName, restaurant.name));
            date.setText(getString(R.string.format_date, restaurant.foundationDate.toString()));
            age.setText(getString(R.string.format_branches, restaurant.branches));
            fullAddress.setText(getString(R.string.full_address,
                    TextUtils.isEmpty(restaurant.address.street) ? "<street>" : restaurant.address.street,
                    TextUtils.isEmpty(restaurant.address.postCode) ? "<PC>" : restaurant.address.postCode,
                    TextUtils.isEmpty(restaurant.address.city) ? "<city>" : restaurant.address.city,
                    TextUtils.isEmpty(restaurant.address.country) ? "<country>" : restaurant.address.country));
        }
    }
}
