package com.ryoga.k17124kk.signalloger_multi.Activity;

import android.content.Context;
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
import android.widget.TextView;

import com.ryoga.k17124kk.signalloger_multi.R;
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

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    private static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";


    //    private Identifier _uuid;
//    private Identifier _major;
//    private Identifier _minor;
//ListViewのAdapter クラス内クラスで定義
    private ListAdapter_main listAdapter_main;

    private ListView listView;
    private ArrayList<DataSet> datas;
    private BeaconManager beaconManager;
    private Region mRegion;
    private int rssi;

    private File_ReadWriter file_readWriter;

    private long startTime;
    private String nowTime = "";
    private String fileName = "BleStrengthData";
    private String fileName_format = "MM_dd_HH:mm:ss";


    private int timeMode = 1;//相対時間(1)か絶対時間(2)か

    private EditText editText_filename;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        file_readWriter = new File_ReadWriter();


        datas = file_readWriter.readFile_config();

        //minorでソート
        Collections.sort(datas, new Comparator<DataSet>() {
            @Override
            public int compare(DataSet o1, DataSet o2) {
                // 今度は昇順にしたいので、o1 と o2の位置を変更します。
                return Integer.parseInt(o1.getMinor()) - Integer.parseInt(o2.getMinor());
            }
        });


        listAdapter_main = new ListAdapter_main(getApplicationContext(), datas);

        listView = findViewById(R.id.listview_main);
        listView.setAdapter(listAdapter_main);


        mRegion = new Region("iBeacon", null, null, null);

        beaconManager = BeaconManager.getInstanceForApplication(this);
        beaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));


        EditText editText_filename = findViewById(R.id.editText_filename);
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(fileName_format);
        editText_filename.setText(sdf.format(date));


        final Button button_start = findViewById(R.id.button_start);
        final Button button_stop = findViewById(R.id.button_stop);

        //ストップボタンを押せないようにする
        button_start.setEnabled(true);
        button_stop.setEnabled(false);
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                startTime = getFirstDate();


                RadioGroup radioGroup = findViewById(R.id.radiogoup);
                if (radioGroup.getCheckedRadioButtonId() == R.id.radioButton1) {
                    timeMode = 1;
                    Log.d("MYE", timeMode + "::");
                } else if (radioGroup.getCheckedRadioButtonId() == R.id.radioButton2) {
                    timeMode = 2;
                    Log.d("MYE", timeMode + "::");
                }

                // ファイル名任意入力
                EditText editText_filename = findViewById(R.id.editText_filename);
                //空欄でなければ任意の名前に。空欄なら初期設定の名前に
                if (!editText_filename.getText().toString().isEmpty()) {
                    fileName = editText_filename.getText().toString();
                } else {

                    fileName = "BleStrengthData";
                }


//
                //レンジングの開始
                try {

                    EditText editText_scan = findViewById(R.id.editText_scan);
                    beaconManager.setForegroundScanPeriod(Integer.parseInt(editText_scan.getText().toString()));
                    beaconManager.startRangingBeaconsInRegion(mRegion);
//                    beaconManager.startMonitoringBeaconsInRegion(mRegion);
                } catch (RemoteException re) {
                }

                //スタートボタンを押せないようにする
                button_start.setEnabled(false);
                button_stop.setEnabled(true);

            }
        });


        button_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    //レンジングの停止
                    beaconManager.stopRangingBeaconsInRegion(mRegion);
                } catch (RemoteException re) {

                }

//                String memos = "HH:mm:ss.SSS";
//                for (DataSet d : datas) {
//                    memos += "," + d.getMemo();
//
//                }
//                file_readWriter.writeFile_LoggingData(fileName, memos);


                for (DataSet dataSet : datas) {
                    dataSet.setExist(false);
                }

                EditText editText_filename = findViewById(R.id.editText_filename);
                Date date = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat(fileName_format);
                editText_filename.setText(sdf.format(date));


                //ストップボタンを押せないようにする
                button_start.setEnabled(true);
                button_stop.setEnabled(false);

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

                Log.d("MYE", "EXit");
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {
                // 領域に対する状態が変化
                Log.d("MYE", "didDetermineStateForRegion");
            }


        });
        beaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {


                String rssi = "";

                if (timeMode == 1) {//相対時間
                    nowTime = String.valueOf(getDate());
                } else if (timeMode == 2) {//絶対時間
                    Date date = new Date();
//                    Log.d("MYE_DDD", date.toString());
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss:SSS");
                    Log.d("MYE_DD", sdf.format(date));

                    nowTime = sdf.format(date);
                }


//                rssi += nowTime - startTime;


                for (DataSet dataSet : datas) {
                    for (Beacon beacon : beacons) {
                        if (dataSet.getUuid().equals(beacon.getId1().toString()) && dataSet.getMajor().equals(beacon.getId2().toString()) && dataSet.getMinor().equals(beacon.getId3().toString())) {
                            dataSet.setRssi(beacon.getRssi());
                            dataSet.setExist(true);
//                            rssi += "," + dataSet.getRssi();

                            break;

                        }
                    }

                }


                String rs = "";
                for (DataSet d : datas) {
                    rs += "," + d.getRssi();
                }

                updateList(datas);


                file_readWriter.writeFile_LoggingData(fileName, nowTime + rs);

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
    private void updateList(ArrayList<DataSet> datasetList) {
        listAdapter_main.setDataSetList(datasetList);
        listAdapter_main.notifyDataSetChanged();
    }

    //ListViewにセットするためのAdapter
    private class ListAdapter_main extends BaseAdapter {

        Context context;
        LayoutInflater layoutInflater;
        ArrayList<DataSet> datasetList;


        public ListAdapter_main(Context context, ArrayList<DataSet> datasetList) {
            this.context = context;
            this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.datasetList = datasetList;


        }


        public void setDataSetList(ArrayList<DataSet> datasetList) {
            this.datasetList = datasetList;
        }


        public Integer getItem_point(int position) {

            return getItem(position).getRssi();

        }


        public ArrayList<DataSet> getDatasetList() {
            return this.datasetList;
        }


        @Override
        public int getCount() {
            return datasetList.size();
        }

        @Override
        public DataSet getItem(int position) {
            return datasetList.get(position);
        }


        @Override
        public long getItemId(int position) {
            return datasetList.indexOf(position);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {


            //ListViewで表示されるレイアウト
            convertView = layoutInflater.inflate(R.layout.logging_nomal, parent, false);

            TextView textView_memo = convertView.findViewById(R.id.textView_memo);
            TextView textView_rssi = convertView.findViewById(R.id.textView_rssi);

            textView_memo.setText(getItem(position).getMemo() + "");
            textView_rssi.setText(getItem(position).getRssi() + "");


            return convertView;
        }


    }


}
