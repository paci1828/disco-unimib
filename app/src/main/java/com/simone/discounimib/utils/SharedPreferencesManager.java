package com.simone.discounimib.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesManager {
    private static final String PREF_NAME = "DiscoUnimibPrefs";
    private static final String KEY_USER_UID = "user_uid";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public SharedPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveUser(String uid, String role) {
        editor.putString(KEY_USER_UID, uid);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }
    
    public void saveString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public void setRememberMe(boolean remember) {
        editor.putBoolean(KEY_REMEMBER_ME, remember);
        editor.apply();
    }

    public boolean isRememberMe() {
        return sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
    }

    public String getUserUid() {
        return sharedPreferences.getString(KEY_USER_UID, null);
    }

    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, null);
    }

    public void clear() {
        editor.clear();
        editor.apply();
    }
}