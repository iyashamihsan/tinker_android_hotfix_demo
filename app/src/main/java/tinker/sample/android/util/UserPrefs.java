package tinker.sample.android.util;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * Created by Yasham on 10/19/17.
 */

public class UserPrefs {

    private static UserPrefs instance;
    private Context context;
    private SharedPreferences settings;

    private UserPrefs(Context context) {
        this.context = context;
        settings = context.getSharedPreferences(AppConstant.PREFS_FILE_NAME_PARAM, 0);
    }

    public static UserPrefs getInstance(Context context) {
        if (instance == null) {
            instance = new UserPrefs(context);
        }
        return instance;
    }

    public void saveString(String key, String value) {
        settings.edit().putString(key, value).commit();
    }

    public String getString(String key, String defaultValue) {
        return settings.getString(key, defaultValue);
    }
}
