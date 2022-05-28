package ru.mai.smartdetect;

import com.google.gson.annotations.SerializedName;

public class SenMLPacket {
    @SerializedName("n")
    public String name;
    @SerializedName("v")
    public Integer value;
}
