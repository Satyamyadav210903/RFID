package com.ecartes.rfid_demo.common;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Common {
    public static String timeStamp = "";
    public static String activitytype = "LoginActivity";
    public static String deviceId = "";
    public static String appPreference = "AssetTracking";
    public static String appPackage = "com.ecartes.actrec";
    public static String userName = "";
    public static String userloginId = "";
    public static Integer loginID = null;
    public static int uplinkInterval = 60;

    public static String exception = "exception";
    public static String mqttErrorLog = "mqttErrorLog";
    public static boolean isOnlogin = false;
    public static String password = "";
    public static String usernameurl = "";
    public static String passwordurl = "";
    public static int timeout = 120;
    public static Integer shelfregistrationpower = 10;
    public static Integer region = 8;
    public static Integer findMyTagPower = 30;
    public static boolean isItOnFailedRecordsPage = false;

    public static boolean isScanRecords = false;

    public static String padLeftZeros(String str, int n) {
        return String.format("%1$" + n + "s", str).replace(' ', '0');
    }

    public static String errorToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    public static void sleep() {
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sleep250() {
        try {
            Thread.sleep(250);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sleep2Second() {
        try {
            Thread.sleep(200);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sleep1Second() {
        try {
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}