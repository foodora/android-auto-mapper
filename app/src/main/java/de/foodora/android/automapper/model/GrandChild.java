package de.foodora.android.automapper.model;

import android.os.Parcel;
import android.os.Parcelable;

class GrandChild implements Parcelable {
    public String name;
    public String details;
    public Double length;
    public int id;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.name);
        dest.writeString(this.details);
        dest.writeValue(this.length);
        dest.writeInt(this.id);
    }

    public GrandChild() {
    }

    protected GrandChild(Parcel in) {
        this.name = in.readString();
        this.details = in.readString();
        this.length = (Double) in.readValue(Double.class.getClassLoader());
        this.id = in.readInt();
    }

    public static final Parcelable.Creator<GrandChild> CREATOR = new Parcelable.Creator<GrandChild>() {
        @Override
        public GrandChild createFromParcel(Parcel source) {
            return new GrandChild(source);
        }

        @Override
        public GrandChild[] newArray(int size) {
            return new GrandChild[size];
        }
    };
}
