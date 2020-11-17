package com.ryoga.k17124kk.signalloger_multi.Activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.ryoga.k17124kk.signalloger_multi.R;
import com.ryoga.k17124kk.signalloger_multi.Util.DataController;
import com.ryoga.k17124kk.signalloger_multi.Util.DataSet;
import com.ryoga.k17124kk.signalloger_multi.Util.File_ReadWriter;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class LoggingWithFilterActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";


    //    private Identifier _uuid;
//    private Identifier _major;
//    private Identifier _minor;
//ListViewのAdapter クラス内クラスで定義
    private LoggingWithFilterActivity.ListAdapter_main listAdapter_main;

    private ListView listView;
    private ArrayList<DataSet> datas;
    private BeaconManager beaconManager;
    private ArrayList<DataController> dataControllers;
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

    private int timeMode = 1;//相対時間(1)か絶対時間(2)か


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Intent intent = getIntent();


        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        file_readWriter = new File_ReadWriter();


        datas = file_readWriter.readFile_config();

        dataControllers = new ArrayList<>();
        for (DataSet d : file_readWriter.readFile_config()) {
            dataControllers.add(new DataController(d, "中央値"));
        }


        //minorでソート
        Collections.sort(datas, new Comparator<DataSet>() {
            @Override
            public int compare(DataSet o1, DataSet o2) {
                // 今度は昇順にしたいので、o1 と o2の位置を変更します。
                return Integer.parseInt(o1.getMinor()) - Integer.parseInt(o2.getMinor());
            }
        });

        Log.d("MYE", datas.toString());


        listAdapter_main = new ListAdapter_main(getApplicationContext(), dataControllers);

        listView = findViewById(R.id.listview_main);
        listView.setAdapter(listAdapter_main);


        Log.d("MYE", "onCreate");


        mRegion = new Region("iBeacon", null, null, null);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));


        Button button_start = findViewById(R.id.button_start);
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                RadioGroup radioGroup = findViewById(R.id.radiogoup);
                if (radioGroup.getCheckedRadioButtonId() == R.id.radioButton1) {
                    timeMode = 1;
                    Log.d("MYE_D", timeMode + "");
                } else if (radioGroup.getCheckedRadioButtonId() == R.id.radioButton2) {
                    timeMode = 2;
                    Log.d("MYE_D", timeMode + "");
                }


                startTime = getFirstDate();

                EditText editText_sample = findViewById(R.id.editText_sample_F);
                sample = Integer.valueOf(editText_sample.getText().toString());

                Spinner spinner_FilterMode = findViewById(R.id.spinner_filterMode);
                filter_Mode = (String) spinner_FilterMode.getSelectedItem();

                for (DataController dc : dataControllers) {
                    dc.setFilterMode(filter_Mode);
                }
//
                //レンジングの開始
                try {
                    for (DataController dc : dataControllers) {
                        dc.setFILTER_SIZE(sample);
                    }

                    EditText editText_scan = findViewById(R.id.editText_scan);
                    beaconManager.setForegroundScanPeriod(Integer.parseInt(editText_scan.getText().toString()));
                    beaconManager.startRangingBeaconsInRegion(mRegion);
                    Log.d("MYE", "Ranging開始");
                } catch (RemoteException re) {
                    Log.d("MYE", re.getMessage());
                }

            }
        });


        Button button_stop = findViewById(R.id.button_stop);
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
//                for (DataSet d : datas) {
//                    Log.d("MYE", d.toString());
//                    memos += "," + d.getMemo();
//
//                }
                file_readWriter.writeFile_LoggingData(fileName, memos);
                file_readWriter.writeFile_LoggingData(fileName_Filtered, memos);


                for (DataController DataController : dataControllers) {
//                    dataSet.setExist(false);
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

                boolean flag = false;


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
                    for (Beacon beacon : beacons) {
                        if (dataController.getDataSet().getUuid().equals(beacon.getId1().toString()) && dataController.getDataSet().getMajor().equals(beacon.getId2().toString()) && dataController.getDataSet().getMinor().equals(beacon.getId3().toString())) {

                            dataController.setDataSetRssi(nowTime, beacon.getRssi());
                            dataController.getDataSet().setExist(true);


                            rssi += "," + dataController.getDataSet().getRssi();

                            rssi_Filtered += "," + dataController.getFilterd_rssi();
//                            rssi += "," + dataSet.getRssi();
                            flag = true;


                            break;

                        }
                    }

                    if (!dataController.getDataSet().isExist()) {
                        rssi += ",0";
                        rssi_Filtered += ",0";
                    }

                }


                String rs = "";
                String rs_F = "";

                rs += nowTime + ",";
                rs_F += nowTime + ",";
                for (DataController d : dataControllers) {
                    rs += "," + d.getDataSet().getRssi();
                    rs_F += "," + d.getFilterd_rssi();
                }


                file_readWriter.writeFile_LoggingData(fileName, rssi);

                if (filter_Mode.equals(FILTER_MODE[0])) {
                    file_readWriter.writeFile_LoggingData(fileName_Filtered + "_Median", rssi_Filtered);
                } else if (filter_Mode.equals(FILTER_MODE[1])) {
                    file_readWriter.writeFile_LoggingData(fileName_Filtered + "_MoveAverage", rssi_Filtered);
                }


                updateList(dataControllers);


            }
        });
    }


    public static long getFirstDate() {
        return System.currentTimeMillis();
    }

    public static long getNowDate() {
        return System.currentTimeMillis();
    }

    public static long getDate() {
        return getNowDate() - getFirstDate();
    }


    //ListをListAdapterにセットし更新を通知する
    private void updateList(ArrayList<DataController> DataControllerList) {
        listAdapter_main.setDataSetList(DataControllerList);

        listAdapter_main.notifyDataSetChanged();
    }


    //ListViewにセットするためのAdapter
    private class ListAdapter_main extends BaseAdapter {

        Context context;
        LayoutInflater layoutInflater;
        ArrayList<DataController> dataControllerList;


        public ListAdapter_main(Context context, ArrayList<DataController> DataControllerList) {
            this.context = context;
            this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.dataControllerList = DataControllerList;


        }


        public void setDataSetList(ArrayList<DataController> DataControllerList) {
            this.dataControllerList = DataControllerList;
        }


        public Integer getItem_point(int position) {
            return getItem(position).getDataSet().getRssi();

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


        @Override
        public long getItemId(int position) {
            return dataControllerList.indexOf(position);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {


            //ListViewで表示されるレイアウト
            convertView = layoutInflater.inflate(R.layout.logging_nomal, parent, false);

            TextView textView_memo = convertView.findViewById(R.id.textView_memo);
            TextView textView_rssi = convertView.findViewById(R.id.textView_rssi);

            textView_memo.setText(getItem(position).getDataSet().getMemo() + "");
            textView_rssi.setText(getItem(position).getFilterd_rssi() + "");


            return convertView;
        }


    }


}
