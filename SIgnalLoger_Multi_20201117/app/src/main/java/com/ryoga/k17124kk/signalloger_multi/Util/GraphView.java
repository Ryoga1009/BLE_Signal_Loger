package com.ryoga.k17124kk.signalloger_multi.Util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

public class GraphView extends View {

    int start = -30;
    int width = 100;//幅制御用
    int hight = 500;//高さの幅
    int size = 30;//何分割するか

    private int color = 0;


    private ArrayList<Integer> arrayList;
    private Paint paint;

    public GraphView(Context context) {
        super(context);
        paint = new Paint();
        arrayList = new ArrayList<>();
        Log.d("MYE_G", "graphView1");

    }

    public GraphView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        paint = new Paint();

        arrayList = new ArrayList<>();

        Log.d("MYE_G", "graphView2");
    }


    public void setColor(int color) {
        this.color = color;
    }

    public void addPoint(Integer num) {
        arrayList.add(num);
        Log.d("MYE_G", "ADD");
    }

    public void setPoint(ArrayList<Integer> dataSetArrayList) {
        arrayList = dataSetArrayList;
    }

    public void update() {
        Log.d("MYE_G", "update");
        invalidate();
    }


    public void clearData() {
        arrayList.clear();
    }


    @Override
    protected void onDraw(Canvas canvas) {


        canvas.drawColor(0, PorterDuff.Mode.CLEAR);


        if (color == 0) {
            canvas.drawColor(Color.argb(255, 194, 24, 91));
        } else {
            canvas.drawColor(Color.argb(255, 48, 63, 159));
        }


        // 線
        paint.setStrokeWidth(15);
        paint.setColor(Color.argb(255, 0, 255, 120));
        // (x1,y1,x2,y2,paint) 始点の座標(x1,y1), 終点の座標(x2,y2)
//        canvas.drawLine(this.getLeft(), this.getTop(), this.getRight(), this.getBottom(), paint);


        Log.d("MYE_G", "更新--" + arrayList.toString());


        if (arrayList.size() >= 2) {
//            for (int i = 0; i < arrayList.size(); i++) {
//                canvas.drawCircle(i * (this.getRight() / 100), this.getTop() + (-1 * (arrayList.get(i) - 30) * (150 / 70)), 8, paint);
//
//            }
            for (int i = 1; i < arrayList.size() - 1; i++) {


                if (arrayList.get(i - 1) == -100 || arrayList.get(i) == -100) {

                } else {
                    canvas.drawLine((i - 1) * (this.getRight() / 100), this.getTop() + (-1 * (arrayList.get(i - 1) - -30) * (500 / 30)), i * (this.getRight() / 100), this.getTop() + (-1 * (arrayList.get(i) - -30) * (500 / 30)), paint);

                }


                //    canvas.drawLine((i - 1) * (this.getRight() / width), this.getTop() + (-1 * (arrayList.get(i - 1) - start) * (hight / size)), i * (this.getRight() / width), this.getTop() + (-1 * (arrayList.get(i) - start) * (hight / size)), paint);
                Log.d("GMYE", arrayList.get(i) + "");


                if (i * (this.getRight() / 100) >= this.getRight()) {
                    clearData();
                }
            }
        }


    }


    @Override
    public String toString() {
        return "GraphView{" +
                "arrayList=" + arrayList +
                ", paint=" + paint +
                '}';
    }
}
