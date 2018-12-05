package com.example.k17124kk.signalloger_alt;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.security.Permission;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class MainActivity extends Activity implements BeaconConsumer {


    private static final String LOCAL_FILE = "Log.csv";
    private final int REQUEST_PERMISSION = 1000;




    Identifier uuid;
    Identifier major;
    Identifier minor;
//        "8b72ac516e4143da353b1af64f160d26"
//    B9407F30-F5F8-466E-AFF9-25556B57FE6D
//    b9407f30-f5f8-466e-aff9-25556b57fe6d  --大鐘


    private static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";

    private BeaconManager beaconManager;
    private  Region mRegion;

    private Button start_button;
    private Button stop_button;
    private Button resetButton;
    private Button event_button;
    private EditText editText_UUID;
    private EditText editText_major;
    private EditText editText_minor;
    private EditText editText_FileName;

    private TextView t;


    //スレッド管理用Handler
    private Handler handler;

    private int rssi;
    private String fileName;



    private ListView listView;
    private ArrayAdapter<String> adapter;


    private String startTime;


    private boolean eventFlag = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MYE","onCreate");
        t = findViewById(R.id.textView);

        listView = findViewById(R.id.listView);
        adapter =   new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        adapter.add("-----");
        listView.setAdapter(adapter);


        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));


        start_button = findViewById(R.id.start_button);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                startTime = getFirstDate();

                editText_UUID = findViewById(R.id.editText_UUID);
                editText_major = findViewById(R.id.editText_major);
                editText_minor = findViewById(R.id.editText_minor);
                editText_FileName = findViewById(R.id.editText_FileName);
                fileName = editText_FileName.getText().toString()+".csv";

                uuid = Identifier.parse(editText_UUID.getText().toString());
                major = Identifier.parse(editText_major.getText().toString());
                minor = Identifier.parse(editText_minor.getText().toString());

                Log.d("MYE",uuid+":"+major+":"+minor);

                mRegion = new Region("ibeacon", uuid, major, minor);

                //レンジングの開始
                try{
                    beaconManager.startRangingBeaconsInRegion(mRegion);
                    Log.d("MYE","Ranging開始");
                }catch(RemoteException re){

                }
                start_button.setEnabled(false);
                stop_button.setEnabled(true);
                resetButton.setEnabled(false);
            }
        });



        stop_button = findViewById(R.id.stop_button);
        stop_button.setEnabled(false);//初期状態では使えないように
        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    //レンジングの停止
                    beaconManager.stopRangingBeaconsInRegion(mRegion);
                    Log.d("MYE","Ranging停止");
                    sampleFileInput();
                }catch(RemoteException re){

                }

                start_button.setEnabled(true);
                stop_button.setEnabled(false);
                resetButton.setEnabled(true);
            }
        });


        event_button = findViewById(R.id.button_event);
        event_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventFlag = true;
            }
        });


        resetButton = findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeLog("",false);
            }
        });


        //hundlerのコンストラクタ呼び出し
        this.handler = new Handler();


        Log.d("MYE","onCreate end--");
        //タイマーセット
        this.setTimer();

    }









    @Override
    protected void onResume() {
        super.onResume();
        // サービスの開始
        beaconManager.bind(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // サービスの停止
        beaconManager.unbind(this);
    }

    @Override
    public void onBeaconServiceConnect() {




        beaconManager.addMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                // 領域侵入

            }
            @Override
            public void didExitRegion(Region region) {
                // 領域退出

            }
            @Override
            public void didDetermineStateForRegion(int i, Region region) {
                // 領域に対する状態が変化
            }


        });
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                // 検出したビーコンの情報
                Log.d("MYE","ビーコン検出");

                int count = 0;


                for(Beacon b : beacons){

                    Log.d("MYE",String.valueOf(b.getRssi())+"------"+count);
                    writeLog(getNowDate()+","+b.getRssi(),true);

                    t = findViewById(R.id.textView);
                    t.setText(String.valueOf(rssi));

                    rssi = b.getRssi();

                    count++;

                }



            }
        });
    }



    private void sampleFileInput(){
        String text = null;

        // try-with-resources
        try (FileInputStream fileInputStream = openFileInput(LOCAL_FILE);
             BufferedReader reader= new BufferedReader(
                     new InputStreamReader(fileInputStream, "UTF-8"))) {

            String lineBuffer;
            while( (lineBuffer = reader.readLine()) != null ) {
                Log.d("MYE",lineBuffer);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String getFirstDate(){
        final DateFormat df = new SimpleDateFormat("HH:mm:ss");
        final Date date = new Date(System.currentTimeMillis());
        return df.format(date);
    }

    public static String getNowDate(){
        final DateFormat df = new SimpleDateFormat("HH:mm:ss");
        final Date date = new Date(System.currentTimeMillis());


        return df.format(date);
    }





    public void writeLog(String str,boolean flag){


        String path = Environment.getExternalStorageDirectory().getPath() + "/"+fileName;
        try {
            FileOutputStream fos = new FileOutputStream(path,flag);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            BufferedWriter bw = new BufferedWriter(osw);
            if(eventFlag){
                str = str + ",@";
                eventFlag = false;
            }
            bw.write(str+"\n");
            bw.flush();
            bw.close();

            Log.d("MYE",path+" に保存しました.");
        } catch (FileNotFoundException e) {
            Log.d("MainActivity", e.toString());
        } catch (IOException e) {
            Log.d("MainActivity", e.toString());
        }

        adapter.add(str);


        if(!flag){
            adapter.clear();
        }
    }











    private void setTimer() {


        Timer timer = new Timer();
        //遅延０ms  10000msごとに呼び出し　
        timer.scheduleAtFixedRate(new TestTask(), 0, 300);
        Log.d("myError", "setTimer");

    }





    class TestTask extends TimerTask {



        public TestTask() {
            Log.d("myError", "TestTask");

        }

        @Override
        public void run(){
            handler.post(new Runnable() {
                @Override
                public void run() {

//                    t = findViewById(R.id.textView);
//                    t.setText(String.valueOf(rssi));



                }
            });
        }


    }











}
