package com.ryoga.k17124kk.signalloger_multi.Util;

import android.app.Application;
import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import com.ryoga.k17124kk.signalloger_multi.R;

public class SoundController extends Application {


    private Context context;
    private MediaPlayer mediaPlayer_1;
    private MediaPlayer mediaPlayer_2;
    private int currentSoundID;


    public SoundController(Context context) {
        Log.d("MYE_Sd", "Soundcontroller");

        this.context = context;

        mediaPlayer_1 = MediaPlayer.create(context, R.raw.pi);
        mediaPlayer_2 = MediaPlayer.create(context, R.raw.cursor2);


        Log.d("MYE_Sd", "Load完了");


    }

    public void setCurrentSoundID(int i) {
        this.currentSoundID = i;
    }

    public int getCurrentSoundID() {
        return this.currentSoundID;
    }


    public void callSound(int soundID) {

        if (soundID == 1) {
            mediaPlayer_1.start();
            Log.d("MYE_Sd", "sound1");
            mediaPlayer_1.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer_1.stop();
                    mediaPlayer_1.reset();
                    mediaPlayer_1.release();
                    mediaPlayer_1 = MediaPlayer.create(context, R.raw.pi);
                    Log.d("MYE_Sd", "sound1 release");
                }
            });

        } else if (soundID == 2) {
            mediaPlayer_2.start();
            Log.d("MYE_Sd", "sound2");
            mediaPlayer_2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer_2.stop();
                    mediaPlayer_2.reset();
                    mediaPlayer_2.release();
                    mediaPlayer_2 = MediaPlayer.create(context, R.raw.cursor2);
                    Log.d("MYE_Sd", "sound2 release");
                }
            });
        }


    }


}
