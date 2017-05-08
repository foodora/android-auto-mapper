package de.foodora.android.automapper.model;

import android.os.Parcel;
import android.os.Parcelable;

class ChildModel implements Parcelable {
    public String name;
    public String description;
    public Integer id;
    public GrandChild children;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.description);
        dest.writeValue(this.id);
        dest.writeParcelable(this.children, flags);
    }

    public ChildModel() {
    }

    protected ChildModel(Parcel in) {
        this.name = in.readString();
        this.description = in.readString();
        this.id = (Integer) in.readValue(Integer.class.getClassLoader());
        this.children = in.readParcelable(GrandChild.class.getClassLoader());
    }

    public static final Parcelable.Creator<ChildModel> CREATOR = new Parcelable.Creator<ChildModel>() {
        @Override
        public ChildModel createFromParcel(Parcel source) {
            return new ChildModel(source);
        }

        @Override
        public ChildModel[] newArray(int size) {
            return new ChildModel[size];
        }
    };
}
