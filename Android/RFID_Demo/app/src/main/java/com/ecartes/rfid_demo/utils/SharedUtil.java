package com.ecartes.rfid_demo.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedUtil {

    private final SharedPreferences mSharedPreferences;

    public SharedUtil(Context context) {
        mSharedPreferences = context.getSharedPreferences("UHF", Context.MODE_PRIVATE);
    }

    public void saveWorkFreq(int workFreq) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt("workFreq", workFreq);
        editor.apply();
    }

    // 0 China1 // 1 FCC // 2 EU
    public int getWorkFreq() {
        return mSharedPreferences.getInt("workFreq", 1);
    }

    public void savePower(int power) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt("power", power);
        editor.apply();
    }

    public int getPower() {
        return mSharedPreferences.getInt("power", 30);
    }

    public void saveSession(int session) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt("session", session);
        editor.apply();
    }

    public int getSession() {
        return mSharedPreferences.getInt("session", 0);
    }

    public void saveQvalue(int qvalue) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt("qvalue", qvalue);
        editor.apply();
    }

    public int getQvalue() {
        return mSharedPreferences.getInt("qvalue", 0);
    }

    public void saveTarget(int target) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.putInt("target", target);
        editor.apply();
    }

    public int getTarget() {
        return mSharedPreferences.getInt("target", 0);
    }
}
