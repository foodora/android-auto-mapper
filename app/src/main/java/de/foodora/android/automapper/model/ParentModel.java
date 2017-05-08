package de.foodora.android.automapper.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ParentModel implements Parcelable {
    public String name;
    public String description;
    public List<ChildModel> children;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.description);
        dest.writeList(this.children);
    }

    public ParentModel() {
    }

    protected ParentModel(Parcel in) {
        this.name = in.readString();
        this.description = in.readString();
        this.children = new ArrayList<ChildModel>();
        in.readList(this.children, ChildModel.class.getClassLoader());
    }

    public static final Parcelable.Creator<ParentModel> CREATOR = new Parcelable.Creator<ParentModel>() {
        @Override
        public ParentModel createFromParcel(Parcel source) {
            return new ParentModel(source);
        }

        @Override
        public ParentModel[] newArray(int size) {
            return new ParentModel[size];
        }
    };
}
