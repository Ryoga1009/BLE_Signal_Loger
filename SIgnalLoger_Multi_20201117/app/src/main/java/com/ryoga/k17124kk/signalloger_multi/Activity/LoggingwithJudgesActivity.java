package com.ryoga.k17124kk.signalloger_multi.Activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.ryoga.k17124kk.signalloger_multi.R;
import com.ryoga.k17124kk.signalloger_multi.Util.DataController;
import com.ryoga.k17124kk.signalloger_multi.Util.DataSet;
import com.ryoga.k17124kk.signalloger_multi.Util.File_ReadWriter;
import com.ryoga.k17124kk.signalloger_multi.Util.GraphView;
import com.ryoga.k17124kk.signalloger_multi.Util.SoundController;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class LoggingwithJudgesActivity extends AppCompatActivity implements BeaconConsumer {


    private static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";


    //    private Identifier _uuid;
//    private Identifier _major;
//    private Identifier _minor;
//ListViewのAdapter クラス内クラスで定義
    private ListAdapter_main listAdapter_main;

    private ListView listView;
    private ArrayList<DataController> dataControllers;
    private BeaconManager beaconManager;
    private Region mRegion;
    private int rssi;

    private File_ReadWriter file_readWriter;


    private long startTime;
    private String nowTime;

    private String fileName = "BleStrengthData";
    private String fileName_Filtered = "BleStrengthData_Lowpath";

    private String filter_Mode;
    private final String FILTER_MODE[] = {"中央値", "移動平均"};


    private int sample = 0;
    private int count = 0;


    private SoundController soundController;


    private int timeMode = 1;//相対時間(1)か絶対時間(2)か


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filtering);


        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        Log.d("MYE_St", "ONCREATE()");
        soundController = new SoundController(getApplicationContext());
        soundController.setCurrentSoundID(2);

        Intent intent = getIntent();


        Log.d("MYE", "FilterActivity");

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        file_readWriter = new File_ReadWriter();


        dataControllers = new ArrayList<>();
        for (DataSet d : file_readWriter.readFile_config()) {
            dataControllers.add(new DataController(d, "中央値"));
        }


        listAdapter_main = new ListAdapter_main(getApplicationContext(), dataControllers);

        listView = findViewById(R.id.listview_main_F);
        listView.setAdapter(listAdapter_main);


        Log.d("MYE", "onCreate");


        mRegion = new Region("iBeacon", null, null, null);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));


        Button button_start = findViewById(R.id.button_start_F);
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startTime = getFirstDate();

                RadioGroup radioGroup = findViewById(R.id.radiogoup);
                if (radioGroup.getCheckedRadioButtonId() == R.id.radioButton1) {
                    timeMode = 1;
                    Log.d("MYE_D", timeMode + "");
                } else if (radioGroup.getCheckedRadioButtonId() == R.id.radioButton2) {
                    timeMode = 2;
                    Log.d("MYE_D", timeMode + "");
                }


                EditText editText_sample = findViewById(R.id.editText_sample_F);
                sample = Integer.valueOf(editText_sample.getText().toString());


                Spinner spinner_FilterMode = findViewById(R.id.spinner_filterMode);
                filter_Mode = (String) spinner_FilterMode.getSelectedItem();
                for (DataController dc : dataControllers) {
                    dc.setFilterMode(filter_Mode);
                }

                Log.d("MYE_F_M", filter_Mode + "------");
//
                //レンジングの開始
                try {

                    for (DataController dc : dataControllers) {
                        dc.setFILTER_SIZE(sample);
                    }
                    EditText editText_scan = findViewById(R.id.editText_scan_F);
                    beaconManager.setForegroundScanPeriod(Integer.parseInt(editText_scan.getText().toString()));
                    beaconManager.startRangingBeaconsInRegion(mRegion);
                    Log.d("MYE_F", "Ranging開始  ControlloerSize : " + dataControllers.size());
                } catch (RemoteException re) {
                    Log.d("MYE", re.getMessage());
                }

            }
        });


        Button button_stop = findViewById(R.id.button_stop_F);
        button_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    //レンジングの停止
                    beaconManager.stopRangingBeaconsInRegion(mRegion);
                    Log.d("MYE", "Ranging停止");
                } catch (RemoteException re) {

                }

                String memos = "ms";
                for (DataController d : dataControllers) {
                    Log.d("MYE", d.toString());
                    memos += "," + d.getDataSet().getMemo();

                }
                file_readWriter.writeFile_LoggingData(fileName, memos);
                file_readWriter.writeFile_LoggingData(fileName_Filtered, memos);


                for (DataController DataController : dataControllers) {
                    DataController.getDataSet().setExist(false);
                    //DataCOntroller内の配列リセット
                    DataController.resetDatasetArrayList();
                }
            }
        });

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

                Log.d("MYE", "Enter");
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


                //音を鳴らす感覚開ける
                count++;

                boolean flag = false;

                //ListAdapter内のグラフ描画用のリスト
                ArrayList<Integer> arrayList = new ArrayList<>();

                String rssi = "";
                String rssi_Filtered = "";

                if (timeMode == 1) {//相対時間
                    nowTime = String.valueOf(getDate());
                } else if (timeMode == 2) {//絶対時間
                    Date date = new Date();
//                    Log.d("MYE_DDD", date.toString());
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
//                    Log.d("MYE_DD", sdf.format(date));

                    nowTime = sdf.format(date);
                }


                rssi += nowTime;
                rssi_Filtered += nowTime;


                for (DataController dataController : dataControllers) {
                    Log.d("MYE_F", dataController.toString() + "======================");
                    for (Beacon beacon : beacons) {
                        if (dataController.getDataSet().getUuid().equals(beacon.getId1().toString()) && dataController.getDataSet().getMajor().equals(beacon.getId2().toString()) && dataController.getDataSet().getMinor().equals(beacon.getId3().toString())) {

                            dataController.setDataSetRssi(nowTime, beacon.getRssi());
                            dataController.getDataSet().setExist(true);

                            rssi += "," + dataController.getDataSet().getRssi();

                            rssi_Filtered += "," + dataController.getFilterd_rssi();

                            //グラフ描画ようにListAdapter用の配列についか
                            arrayList.add(beacon.getRssi());

                            flag = true;

                            break;
                        } else {

                        }
                    }

                    if (!dataController.getDataSet().isExist()) {
                        rssi += ",0";
                        rssi_Filtered += ",0";
                    }

                    if (flag) {
                        file_readWriter.writeFile_LoggingData(dataController.getDataSet().getMemo(), nowTime + "," + dataController.getDataSet().getRssi());

                        if (filter_Mode.equals(FILTER_MODE[0])) {
                            file_readWriter.writeFile_LoggingData(dataController.getDataSet().getMemo() + "_Lowpath_Median", nowTime + "," + dataController.getFilterd_rssi());
                        } else if (filter_Mode.equals(FILTER_MODE[1])) {
                            file_readWriter.writeFile_LoggingData(dataController.getDataSet().getMemo() + "_Lowpath_MoveAverage", nowTime + "," + dataController.getFilterd_rssi());

                        }

                    }
                }


                updateList(dataControllers, arrayList);


                Log.d("MYE", rssi + "");

                if (flag) {
                    file_readWriter.writeFile_LoggingData(fileName, rssi);
                    if (filter_Mode.equals(FILTER_MODE[0])) {
                        file_readWriter.writeFile_LoggingData(fileName_Filtered + "_Median", rssi_Filtered);
                    } else if (filter_Mode.equals(FILTER_MODE[1])) {
                        file_readWriter.writeFile_LoggingData(fileName_Filtered + "_MoveAverage", rssi_Filtered);

                    }
                }


                if (count >= 3) {
                    soundController.callSound(soundController.getCurrentSoundID());
                    count = 0;
                }


            }
        });
    }


    public long getStartTime() {
        return this.startTime;
    }

    public long getFirstDate() {
        return System.currentTimeMillis();
    }

    public long getNowDate() {
        return System.currentTimeMillis();
    }

    public long getDate() {
        return getNowDate() - getStartTime();
    }


    //ListをListAdapterにセットし更新を通知する
    private void updateList(ArrayList<DataController> DataControllerList, ArrayList<Integer> arrayList) {
        listAdapter_main.setDataSetList(DataControllerList, arrayList);

        listAdapter_main.notifyDataSetChanged();
    }


    //ListViewにセットするためのAdapter
    private class ListAdapter_main extends BaseAdapter {

        Context context;
        LayoutInflater layoutInflater;
        ArrayList<DataController> dataControllerList;
        ArrayList<Integer> arrayList_point;
        ArrayList<Integer> arrayList_point_forGraph;


        public ListAdapter_main(Context context, ArrayList<DataController> DataControllerList) {
            this.context = context;
            this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.dataControllerList = DataControllerList;
            this.arrayList_point = new ArrayList<>();
            this.arrayList_point_forGraph = new ArrayList<>();
            arrayList_point.add(new Integer(90));
        }


        public void setDataSetList(ArrayList<DataController> DataControllerList, ArrayList<Integer> arrayList) {
            this.dataControllerList = DataControllerList;
            this.arrayList_point = arrayList;
        }


        public ArrayList<DataController> getDataControllerList() {
            return this.dataControllerList;
        }


        @Override
        public int getCount() {
            return dataControllerList.size();
        }

        @Override
        public DataController getItem(int position) {
            return dataControllerList.get(position);
        }

        public Integer getItem_point(int position) {
            return getItem(position).getDataSet().getRssi();

        }

        public void setItem_Point(Integer point) {
            arrayList_point.add(point);
        }


        @Override
        public long getItemId(int position) {
            return dataControllerList.indexOf(position);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {


            //ListViewで表示されるレイアウト
            convertView = layoutInflater.inflate(R.layout.loging_data, parent, false);

            TextView textView_memo = convertView.findViewById(R.id.textView_memo);
            textView_memo.setTextColor(Color.BLACK);
            TextView textView_rssi = convertView.findViewById(R.id.textView_rssi);
            textView_rssi.setTextColor(Color.WHITE);
            TextView textView_stability = convertView.findViewById(R.id.textView_Stability);
            textView_stability.setTextColor(Color.BLACK);
            TextView textView_negaposi = convertView.findViewById(R.id.textView_state);
            textView_negaposi.setTextColor(Color.WHITE);


            final GraphView graphView = convertView.findViewById(R.id.Graph);
            graphView.setPoint(arrayList_point_forGraph);


            Button button = convertView.findViewById(R.id.Button_reset_F);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    graphView.clearData();
                }
            });


            LinearLayout linearLayout = convertView.findViewById(R.id.BackGround);


            textView_memo.setText(getItem(position).getDataSet().getMemo() + "");
            textView_rssi.setText(getItem(position).getFilterd_rssi() + "");


            arrayList_point_forGraph.add(new Integer(getItem(position).getFilterd_rssi()));


            Log.d("MYE_G", "==================================");


            if (getItem(position).getStabilityOfSensingSignal().isStability()) {
                textView_stability.setText("安定");
                textView_stability.setBackgroundColor(getColor(R.color.colorPositive_Dark));
            } else {
                textView_stability.setText("不安定");
                textView_stability.setBackgroundColor(getColor(R.color.colorNegative_Dark));
            }


            //一つ前の状態を見て反転し現在の状態にする
            if (getItem(position).getStabilityOfSensingSignal().getCurrentStabilityData().getNegaPosi() == 0) {
                textView_negaposi.setText("ポジティブ");
                textView_negaposi.setBackgroundColor(getColor(R.color.colorPositive_Dark));

                graphView.setColor(0);

                //背景を全体的に赤に
                linearLayout.setBackgroundColor(getColor(R.color.colorPositive_Dark));


//                textView_rssi.setBackgroundColor(getColor(R.color.colorPositive));

                //各安定区間が持つ状態変化情報をもとに音を鳴らす
                if (getItem(position).getStabilityOfSensingSignal().getCallSound() == 1) {
                    soundController.callSound(1);
                    soundController.setCurrentSoundID(1);
                    getItem(position).getStabilityOfSensingSignal().setCallSound(0);
                }

            } else {
                textView_negaposi.setText("ネガティブ");
                textView_negaposi.setBackgroundColor(getColor(R.color.colorNegative_Dark));

                //背景を全体的に青に
                linearLayout.setBackgroundColor(getColor(R.color.colorNegative_Dark));


                graphView.setColor(1);

//                textView_rssi.setBackgroundColor(getColor(R.color.colorNegative));

                //各安定区間が持つ状態変化情報をもとに音を鳴らす
                if (getItem(position).getStabilityOfSensingSignal().getCallSound() == 2) {
                    soundController.callSound(2);
                    soundController.setCurrentSoundID(2);
                    getItem(position).getStabilityOfSensingSignal().setCallSound(0);
                }
            }


            graphView.update();


            return convertView;
        }


    }


}
