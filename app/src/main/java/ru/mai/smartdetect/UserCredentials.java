package ru.mai.smartdetect;

import android.content.Context;
import android.content.SharedPreferences;

public class UserCredentials {
    final String userName;
    final String password;
    final String controlTopic;
    final String communicationTopic;


    public UserCredentials(String userName, String password, String controlTopic, String communicationTopic) {
        this.userName = userName;
        this.password = password;
        this.controlTopic = controlTopic;
        this.communicationTopic = communicationTopic;
    }

    public static UserCredentials load(Context ctx) {
        SharedPreferences sharedPref =
                ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key),
                        Context.MODE_PRIVATE);

        String unamePref = sharedPref.getString(ctx.getString(R.string.preference_email), "");
        if (unamePref.isEmpty()) {
            return null;
        }

        String psw = sharedPref.getString(ctx.getString(R.string.preference_password), "");
        if (psw.isEmpty()) {
            return null;
        }

        String conTopic = sharedPref.getString(ctx.getString(R.string.preference_con_topic), "");
        if (conTopic.isEmpty()) {
            return null;
        }

        String comTopic = sharedPref.getString(ctx.getString(R.string.preference_com_topic), "");
        if (comTopic.isEmpty()) {
            return null;
        }

        return new UserCredentials(
                unamePref, psw, conTopic, comTopic
        );
    }


    public void save(Context ctx) {
        SharedPreferences sharedPref =
                ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(ctx.getString(R.string.preference_email), userName);
        editor.putString(ctx.getString(R.string.preference_password), password);
        editor.putString(ctx.getString(R.string.preference_con_topic), controlTopic);
        editor.putString(ctx.getString(R.string.preference_com_topic), communicationTopic);
        editor.apply();
    }

    public static void clear(Context ctx) {
        SharedPreferences sharedPref =
                ctx.getSharedPreferences(ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        sharedPref.edit().clear().apply();
    }
}
