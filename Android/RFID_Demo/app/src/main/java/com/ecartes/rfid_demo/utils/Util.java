package com.ecartes.rfid_demo.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import java.util.HashMap;
import java.util.Map;

public class Util {

    public static SoundPool sp;
    public static Map<Integer, Integer> suondMap;
    public static Context context;

    //
    public static void initSoundPool(Context context){
        Util.context = context;
        sp = new SoundPool(1, AudioManager.STREAM_MUSIC, 1);
        suondMap = new HashMap<Integer, Integer>();
        // We don't have R.raw.scan resource, so just initialize map without loading sound
        // suondMap.put(1, sp.load(context, R.raw.scan, 1)); // Commented out since resource doesn't exist
        suondMap.put(1, 0); // Placeholder value
    }

    //
    public static  void play(int sound, int number){
        if(sp != null && suondMap.containsKey(sound)) {
            int soundId = suondMap.get(sound);
            if(soundId != 0) { // Only play if sound was loaded
                AudioManager am = (AudioManager)Util.context.getSystemService(Util.context.AUDIO_SERVICE);
                //
                float audioMaxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                //
                float audioCurrentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                float volumnRatio = audioCurrentVolume/audioMaxVolume;
                sp.play(
                        soundId, //
                        audioCurrentVolume, //
                        audioCurrentVolume, //
                        1, //
                        number, //
                        1);//
            }
        }
    }
    
    public static void pasue() {
        if(sp != null) {
            sp.pause(0);
        }
    }
}