package com.dylan_wang.capturescreen;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Service1 extends Service
{
    private LinearLayout mFloatLayout = null;
    private WindowManager.LayoutParams wmParams = null;
    private WindowManager mWindowManager = null;
    private LayoutInflater inflater = null;
    private ImageButton mFloatView = null;

    private static final String TAG = "MainActivity";

    private String pathImage = null;
    private String nameImage = null;

    private MediaProjection mMediaProjection = null;
    private VirtualDisplay mVirtualDisplay = null;

    public static int mResultCode = 0;
    public static Intent mResultData = null;
    public static MediaProjectionManager mMediaProjectionManager1 = null;

    private WindowManager mWindowManager1 = null;
    private int windowWidth = 0;
    private int windowHeight = 0;
    private ImageReader mImageReader = null;
    private DisplayMetrics metrics = null;
    private int mScreenDensity = 0;

    private Bitmap bitmapSource;
    private ArrayList<Bitmap> templateMap = new ArrayList<Bitmap>();
    List<String> painame = Arrays.asList( "白板", "北风", "东风","南风", "西风", "发财", "红中",
            "一万","二万","三万","四万","五万","六万","七万","八万","九万",
            "一条","二条","三条","四条","五条","六条","七条","八条","九条",
            "一筒", "二筒", "三筒","四筒","五筒", "六筒","七筒","八筒", "九筒"
    );

    @Override
    public void onCreate()
    {
        // TODO Auto-generated method stub
        super.onCreate();
        createFloatView();
        createVirtualEnvironment();
    }


    @Override
    public IBinder onBind(Intent intent)
    {
        // TODO Auto-generated method stub
        return null;
    }

    private void createFloatView()
    {
        wmParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager)getApplication().getSystemService(getApplication().WINDOW_SERVICE);
        wmParams.type = LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;
        wmParams.gravity = Gravity.LEFT | Gravity.TOP;
        wmParams.x = 0;
        wmParams.y = 0;
        wmParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        inflater = LayoutInflater.from(getApplication());
        mFloatLayout = (LinearLayout) inflater.inflate(R.layout.float_layout, null);
        mWindowManager.addView(mFloatLayout, wmParams);
        mFloatView = (ImageButton)mFloatLayout.findViewById(R.id.float_id);

        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        mFloatView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                wmParams.x = (int) event.getRawX() - mFloatView.getMeasuredWidth() / 2;
                wmParams.y = (int) event.getRawY() - mFloatView.getMeasuredHeight() / 2 - 25;
                mWindowManager.updateViewLayout(mFloatLayout, wmParams);
                return false;
            }
        });

        mFloatView.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // hide the button
                mFloatView.setVisibility(View.INVISIBLE);
                Handler handler1 = new Handler();
                handler1.postDelayed(new Runnable() {
                    public void run() {
                        //start virtual
                        startVirtual();
                        Handler handler2 = new Handler();
                        handler2.postDelayed(new Runnable() {
                            public void run() {
                                //capture the screen
                                startCapture();

                                Handler handler3 = new Handler();
                                handler3.postDelayed(new Runnable() {
                                    public void run() {
                                        matchCards();
                                        Handler handler4 = new Handler();
                                        handler4.postDelayed(new Runnable() {
                                            public void run() {
                                                mFloatView.setVisibility(View.VISIBLE);
                                                //stopVirtual();
                                            }
                                        },50);
                                    }
                                }, 50);
                            }
                        }, 50);
                    }
                }, 50);
            }
        });

        Log.i(TAG, "created the float sphere view");
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void createVirtualEnvironment(){
        pathImage = Environment.getExternalStorageDirectory().getPath()+"/Pictures/";
        nameImage = pathImage+"pic.png";
        mMediaProjectionManager1 = (MediaProjectionManager)getApplication().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mWindowManager1 = (WindowManager)getApplication().getSystemService(Context.WINDOW_SERVICE);
        windowWidth = mWindowManager1.getDefaultDisplay().getWidth();
        windowHeight = mWindowManager1.getDefaultDisplay().getHeight();
        metrics = new DisplayMetrics();
        mWindowManager1.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mImageReader = ImageReader.newInstance(windowWidth, windowHeight, 0x1, 2); //ImageFormat.RGB_565

        Log.i(TAG, "prepared the virtual environment");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void startVirtual(){
        if (mMediaProjection != null) {
            Log.i(TAG, "want to display virtual");
            virtualDisplay();
        } else {
            Log.i(TAG, "start screen capture intent");
            Log.i(TAG, "want to build mediaprojection and display virtual");
            setUpMediaProjection();
            virtualDisplay();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setUpMediaProjection(){
        mResultData = ((ShotApplication)getApplication()).getIntent();
        mResultCode = ((ShotApplication)getApplication()).getResult();
        mMediaProjectionManager1 = ((ShotApplication)getApplication()).getMediaProjectionManager();
        mMediaProjection = mMediaProjectionManager1.getMediaProjection(mResultCode, mResultData);
        Log.i(TAG, "mMediaProjection defined");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void virtualDisplay(){
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                windowWidth, windowHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
        Log.i(TAG, "virtual displayed");
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startCapture(){
        nameImage = pathImage+"pic.png";
        Image image = mImageReader.acquireLatestImage();
        int width = image.getWidth();
        int height = image.getHeight();
        final Image.Plane[] planes = image.getPlanes();
        final ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap bitmap = Bitmap.createBitmap(width+rowPadding/pixelStride, height, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);
        bitmap = Bitmap.createBitmap(bitmap, 0, 0,width, height);
        Log.i(TAG, "width=" + bitmap.getWidth() + "  height=" +bitmap.getHeight());
        image.close();
        Log.i(TAG, "image data captured");

        if(bitmap != null) {
            bitmapSource  = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        }

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
        Log.i(TAG,"mMediaProjection undefined");
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        Log.i(TAG,"virtual display stopped");
    }

    @Override
    public void onDestroy()
    {
        // to remove mFloatLayout from windowManager
        super.onDestroy();
        if(mFloatLayout != null)
        {
            mWindowManager.removeView(mFloatLayout);
        }
        tearDownMediaProjection();
        Log.i(TAG, "application destroy");
    }


    public void matchCards() {
        ArrayList<Card> pai = new ArrayList<Card>();
        Mat source = new Mat();
        bitmapSource = Intercept(bitmapSource, 0,1160, 1080, 100);
        bitmapSource = Scale(bitmapSource);
        templateInit();
        int seque = 1;
        int lastseque = 0;
        int len = templateMap.size();
        for(int n=0 ; n < len ; ){
            Utils.bitmapToMat(bitmapSource, source);
            Mat template = new Mat();
            Bitmap bitmapTemplate = templateMap.get(n);
            Utils.bitmapToMat(bitmapTemplate, template);
            Mat result = Mat.zeros(source.rows() - template.rows() + 1, source.cols() - template.cols() + 1, CvType.CV_32FC1);
            Imgproc.matchTemplate(source, template, result, Imgproc.TM_SQDIFF);
            Core.MinMaxLocResult mlr = Core.minMaxLoc(result);
            org.opencv.core.Point matchLoc = mlr.minLoc;
            double minVal = mlr.minVal;
           // Log.i(TAG, "minVal=" + minVal + "  n=" +n );
            if (minVal <= 5000000) {
                bitmapSource = ChangeColor(bitmapSource, matchLoc);
                if(seque != lastseque){
                    Card card = new Card(seque, matchLoc);
                    card.count +=1;
                    card.loc = matchLoc;
                    pai.add(card);
                    lastseque = seque;
                }
                else{
                    int length = pai.size();
                    int index = length -1;
                    Card card = new Card(pai.get(index));
                    card.count += 1;
                    pai.set(index, card);
                }
                seque -=1;
                n -=1;
            }

            seque +=1;
            n += 1;
        }


        int length = pai.size();
        int count1 = 0;
        int count2 = 0;
        int count3 = 0;
        int count4 = 0;
        for(int i=0;i<length;i++){
            if(pai.get(i).id <=7){
                count1 += 1;
            }
            else if(pai.get(i).id>7 && pai.get(i).id<=16){
                count2 +=1;
            }
            else if(pai.get(i).id>16&&pai.get(i).id<=25){
                count3 +=1;
            }
            else if(pai.get(i).id>25 && pai.get(i).id <=34){
                count4 +=1;
            }
        }

        if(count1 != 0){
            int amount1[] = new int[count1];
            for(int i = 0;i<count1;i++){
                amount1[i] = pai.get(i).count;
            }
            int pri[] = el_getpriority(amount1);
            for(int i = 0;i<count1;i++){
                Card card = new Card(pai.get(i));
                card.priority = pri[i];
                pai.set(i, card);
            }
        }

        if(count2 !=0){
            int per = count1;
            ArrayList<Integer> gloup2 = new ArrayList<Integer>();
            int num = 1;
            if(count2 == 1){
                gloup2.add(num);
            }
            else {
                for (int i = 1; i < count2; i++) {
                    if (pai.get(per + i).id == pai.get(per + i - 1).id + 1) {
                        if (i == count2 - 1) {
                            num += 1;
                            gloup2.add(num);
                        } else
                            num += 1;
                    } else {
                        if(i == count2 - 1){
                            gloup2.add(num);
                            gloup2.add(1);

                        }
                        else{
                            gloup2.add(num);
                            num = 1;
                        }
                    }
                }
            }
            int size = gloup2.size();
            int per1 = 0;
            for(int i = 0;i<size;i++){
                int cou = gloup2.get(i);
                int amount[] = new int[cou];
                for(int j = 0;j<cou;j++){
                    amount[j] = pai.get(per + per1 + j).count;
                }
                int pri[] = getpriority(amount);
                for(int j = 0;j<cou;j++){
                    Card card = new Card(pai.get(per + per1 + j));
                    card.priority = pri[j];
                    pai.set(per + per1 + j, card);
                }
                per1 +=cou;
            }
        }

        if(count3 != 0){
            int per = count1 + count2;
            ArrayList<Integer> gloup3 = new ArrayList<Integer>();
            int num = 1;
            if(count3 == 1){
                gloup3.add(num);
            }
            else {
                for (int i = 1; i < count3; i++) {
                    if (pai.get(per + i).id == pai.get(per + i - 1).id + 1) {
                        if (i == count3 - 1) {
                            num += 1;
                            gloup3.add(num);
                        } else
                            num += 1;
                    } else {
                        if(i == count3 - 1){
                            gloup3.add(num);
                            gloup3.add(1);
                        }
                        else{
                            gloup3.add(num);
                            num = 1;
                        }
                    }
                }
            }

            int size = gloup3.size();
            int per1 = 0;
            for(int i = 0;i<size;i++){
                int cou = gloup3.get(i);
                int amount[] = new int[cou];
                for(int j = 0;j<cou;j++){
                    amount[j] = pai.get(per + per1 + j).count;
                }
                int pri[] = getpriority(amount);
                for(int j = 0;j<cou;j++){
                    Card card = new Card(pai.get(per + per1 + j));
                    card.priority = pri[j];
                    pai.set(per + per1 + j, card);
                }
                per1 +=cou;
            }

        }

        if(count4 != 0) {
            int per = count1 + count2 + count3;
            ArrayList<Integer> gloup4 = new ArrayList<Integer>();
            int num = 1;
            if (count4 == 1) {
                gloup4.add(num);
            }
            else {
                for (int i = 1; i < count4; i++) {
                    if (pai.get(per + i).id == pai.get(per + i - 1).id + 1) {
                        if (i == count4 - 1) {
                            num += 1;
                            gloup4.add(num);
                        } else
                            num += 1;
                    } else {
                        if(i == count4 - 1){
                            gloup4.add(num);
                            gloup4.add(1);
                        }
                        else{
                            gloup4.add(num);
                            num = 1;
                        }
                    }
                }
            }
            int size = gloup4.size();
            int per1 = 0;
            for (int i = 0; i < size; i++) {
                int cou = gloup4.get(i);
                int amount[] = new int[cou];
                for (int j = 0; j < cou; j++) {
                    amount[j] = pai.get(per + per1 + j).count;
                }
                int pri[] = getpriority(amount);
                for (int j = 0; j < cou; j++) {
                    Card card = new Card(pai.get(per + per1 + j));
                    card.priority = pri[j];
                    pai.set(per + per1 + j, card);
                }
                per1 += cou;
            }
        }
        if(length == 0){
            Toast.makeText(getApplicationContext(), "截图有误", Toast.LENGTH_SHORT).show();

        }
        else{
            int priority = pai.get(0).priority;
            int id = pai.get(0).id;
            for(int i = 0;i<length;i++){
                if(pai.get(i).priority<priority){
                    id = pai.get(i).id;
                    priority = pai.get(i).priority;
                }
            }
            id -=1;
            if(id >=0){
                String str = painame.get(id);
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void templateInit() {
        for(int idx = 1;idx<35;idx++){
            String imgname = "m" + idx;
            int resId = getResources().getIdentifier(imgname, "drawable", getClass().getPackage().getName());
            Bitmap bitmapTemplate = BitmapFactory.decodeStream(getResources().openRawResource(resId));
            Bitmap scaled = Scale(bitmapTemplate);
            templateMap.add(scaled);
        }
    }

    //图像伸缩
    public Bitmap Scale(Bitmap bitmap) {
        int rawHeight = bitmap.getHeight();
        int rawWidth = bitmap.getWidth();
        Matrix matrix = new Matrix();
        matrix.setScale(0.5f, 0.5f);
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, rawWidth, rawHeight, matrix, true);
        return newBitmap;
    }

    //图像截取
    public Bitmap Intercept(Bitmap bitmap, int x, int y, int width, int height){
        Bitmap newBitmap = Bitmap.createBitmap(bitmap, x, y, width, height);
        return newBitmap;
    }
    //改变颜色
    public Bitmap ChangeColor(Bitmap bitmap, org.opencv.core.Point loc ){
        int x = (int)loc.x;
        int y = (int)loc.y;
        for (int i = y; i < y+30; i++) {
            for (int j = x; j < x+20; j++) {
                bitmap.setPixel(j, i, Color.BLACK);
            }
        }
        return bitmap;
    }
    //中发白等计算优先级
    public static int[] el_getpriority(int count[]){
        int length = count.length;
        int pri[] = new int[length];
        for(int i = 0;i<length;i++){
            if(count[i] == 1){
                pri[i]  = 1;
            }
            else if(count[i] == 2 ){
                pri[i] = 3;
            }
            else if(count[i] == 3 || count[i] == 4){
                pri[i] = 5;
            }
        }
        return pri;
    }
    //万条饼等计算优先级
    public static int[] getpriority(int count[]){
        int length = count.length;
        int pri[] = new int[length];
        switch(length){
            case 1:
                if(count[0] == 1)
                    pri[0] = 1;
                else if(count[0] == 2)
                    pri[0] = 3;
                else if(count[0] ==3||count[0] ==4)
                    pri[0] = 5;
                break;

            case 2:
                pri = count_two(count);
                break;

            case 3:
                pri = count_three(count);
                break;

            case 4:
                pri = count_four(count);
                break;
            case 5:
                for(int i =0;i<5;i++){
                    if(count[i] == 1)
                        pri[i] = 2;
                    else if(count[i] == 2)
                        pri[i] = 3;
                    else if(count[i] == 3||count[i]==4)
                        pri[i] = 5;
                }
                break;
            case 6:
                for(int i =0;i<6;i++){
                    if(count[i] == 1)
                        pri[i] = 2;
                    else if(count[i] == 2)
                        pri[i] = 3;
                    else if(count[i] == 3||count[i]==4)
                        pri[i] = 5;
                }
                break;
            case 7:
                for(int i =0;i<7;i++){
                    if(count[i] == 1)
                        pri[i] = 2;
                    else if(count[i] == 2)
                        pri[i] = 3;
                    else if(count[i] == 3||count[i]==4)
                        pri[i] = 5;
                }
                break;
            case 8:
                for(int i =0;i<8;i++){
                    if(count[i] == 1)
                        pri[i] = 2;
                    else if(count[i] == 2)
                        pri[i] = 3;
                    else if(count[i] == 3||count[i]==4)
                        pri[i] = 5;
                }
                break;
            case 9:
                for(int i =0;i<9;i++){
                    if(count[i] == 1)
                        pri[i] = 2;
                    else if(count[i] == 2)
                        pri[i] = 3;
                    else if(count[i] == 3||count[i]==4)
                        pri[i] = 5;
                }
                break;

        }
        return pri;
    }

    public static int[] count_two(int count[]){
        int pri[] = new int[2];
        String str = "";
        for(int i=0;i<2;i++){
            str += String.valueOf(count[i]);
        }
        switch(str){
            case "11":
                pri[0] = 2;
                pri[1] = 2;
                break;
            case "12":
                pri[0] = 2;
                pri[1] = 3;
                break;
            case "21":
                pri[0] = 3;
                pri[1] = 2;
                break;
            case "22":
                pri[0] = 3;
                pri[1] = 3;
                break;
            case "13":
                pri[0] = 2;
                pri[1] = 5;
                break;
            case "31":
                pri[0] = 5;
                pri[1] = 2;
                break;
            case "23":
                pri[0] = 3;
                pri[1] = 5;
                break;
            case "32":
                pri[0] = 5;
                pri[1] = 3;
                break;
            case "33":
                pri[0] = 5;
                pri[1] = 5;
                break;
        }
        return pri;
    }

    public static int[] count_three(int count[]) {
        int pri[] = new int[3];
        String str = "";
        for (int i = 0; i < 3; i++) {
            str += String.valueOf(count[i]);
        }
        switch(str){
            case "111":
                pri[0] = 4;
                pri[1] = 4;
                pri[2] = 4;
                break;
            case "211":
                pri[0] = 2;
                pri[1] = 4;
                pri[2] = 4;
                break;
            case "121":
                pri[0] = 4;
                pri[1] = 2;
                pri[2] = 4;
                break;
            case "112":
                pri[0] = 4;
                pri[1] = 4;
                pri[2] = 2;
                break;
            case "311":
                pri[0] = 5;
                pri[1] = 4;
                pri[2] = 4;
                break;
            case "131":
                pri[0] = 4;
                pri[1] = 5;
                pri[2] = 4;
                break;
            case "113":
                pri[0] = 4;
                pri[1] = 4;
                pri[2] = 5;
                break;
            case "221":
                pri[0] = 3;
                pri[1] = 3;
                pri[2] = 4;
                break;
            case "212":
                pri[0] = 3;
                pri[1] = 4;
                pri[2] = 3;
                break;
            case "122":
                pri[0] = 4;
                pri[1] = 3;
                pri[2] = 3;
                break;
            case "222":
                pri[0] = 5;
                pri[1] = 5;
                pri[2] = 5;
                break;
            case "321":
                pri[0] = 5;
                pri[1] = 4;
                pri[2] = 2;
                break;
            case "312":
                pri[0] = 5;
                pri[1] = 2;
                pri[2] = 4;
                break;
            case "123":
                pri[0] = 2;
                pri[1] = 4;
                pri[2] = 5;
                break;
            case "132":
                pri[0] = 2;
                pri[1] = 5;
                pri[2] = 4;
                break;
            case "213":
                pri[0] = 4;
                pri[1] = 2;
                pri[2] = 5;
                break;
            case "231":
                pri[0] = 4;
                pri[1] = 5;
                pri[2] = 2;
                break;
            case "322":
                pri[0] = 2;
                pri[1] = 5;
                pri[2] = 5;
                break;
            case "232":
                pri[0] = 5;
                pri[1] = 2;
                pri[2] = 5;
                break;
            case "223":
                pri[0] = 5;
                pri[1] = 5;
                pri[2] = 2;
                break;
            case "323":
                pri[0] = 5;
                pri[1] = 4;
                pri[2] = 5;
                break;
            case "332":
                pri[0] = 5;
                pri[1] = 5;
                pri[2] = 4;
                break;
            case "233":
                pri[0] = 4;
                pri[1] = 5;
                pri[2] = 5;
                break;
            case "133":
                pri[0] = 2;
                pri[1] = 5;
                pri[2] = 5;
                break;
            case "313":
                pri[0] = 5;
                pri[1] = 2;
                pri[2] = 5;
                break;
            case "331":
                pri[0] = 5;
                pri[1] = 5;
                pri[2] = 2;
                break;
            case "333":
                pri[0] = 5;
                pri[1] = 5;
                pri[2] = 5;
                break;
        }
        return pri;
    }

    public static int[] count_four(int count[]){
        int pri[] = new int[4];
        String str = "";
        for (int i = 0; i < 4; i++) {
            str += String.valueOf(count[i]);
        }
        switch(str){
            case "1111":
                pri[0] = 2;
                pri[1] = 4;
                pri[2] = 4;
                pri[3] = 4;
                break;
            case "1112":
                pri[0] = 4;
                pri[1] = 4;
                pri[2] = 4;
                pri[3] = 3;
                break;
            case "1121":
                pri[0] = 4;
                pri[1] = 4;
                pri[2] = 3;
                pri[3] = 4;
                break;
            case "1211":
                pri[0] = 4;
                pri[1] = 3;
                pri[2] = 4;
                pri[3] = 4;
                break;
            case "2111":
                pri[0] = 3;
                pri[1] = 4;
                pri[2] = 4;
                pri[3] = 4;
                break;
            case "1113":
                pri[0] = 4;
                pri[1] = 4;
                pri[2] = 4;
                pri[3] = 5;
                break;
            case "1131":
                pri[0] = 4;
                pri[1] = 4;
                pri[2] = 5;
                pri[3] = 2;
                break;
            case "1311":
                pri[0] = 2;
                pri[1] = 5;
                pri[2] = 4;
                pri[3] = 4;
                break;
            case "3111":
                pri[0] = 5;
                pri[1] = 4;
                pri[2] = 4;
                pri[3] = 4;
                break;
            case "1123":
                pri[0] = 4;
                pri[1] = 4;
                pri[2] = 2;
                pri[3] = 5;
                break;
            case "1132":
                pri[0] = 2;
                pri[1] = 2;
                pri[2] = 5;
                pri[3] = 3;
                break;
            case "1213":
                pri[0] = 4;
                pri[1] = 3;
                pri[2] = 4;
                pri[3] = 5;
                break;
            case "1231":
                pri[0] = 2;
                pri[1] = 3;
                pri[2] = 5;
                pri[3] = 2;
                break;
            case "2113":
                pri[0] = 2;
                pri[1] = 4;
                pri[2] = 4;
                pri[3] = 5;
                break;
            case "2131":
                pri[0] = 3;
                pri[1] = 4;
                pri[2] = 5;
                pri[3] = 2;
                break;
            case "2311":
                pri[0] = 3;
                pri[1] = 5;
                pri[2] = 2;
                pri[3] = 2;
                break;
            case "3112":
                pri[0] = 5;
                pri[1] = 4;
                pri[2] = 4;
                pri[3] = 2;
                break;
            case "3121":
                pri[0] = 5;
                pri[1] = 4;
                pri[2] = 2;
                pri[3] = 4;
                break;
            case "3211":
                pri[0] = 5;
                pri[1] = 2;
                pri[2] = 4;
                pri[3] = 4;
                break;
            case "1312":
                pri[0] = 2;
                pri[1] = 5;
                pri[2] = 4;
                pri[3] = 3;
                break;
            case "1321":
                pri[0] = 2;
                pri[1] = 5;
                pri[2] = 3;
                pri[3] = 4;
                break;
            case "1223":
                pri[0] = 2;
                pri[1] = 3;
                pri[2] = 3;
                pri[3] = 5;
                break;
            case "3221":
                pri[0] = 5;
                pri[1] = 3;
                pri[2] = 3;
                pri[3] = 2;
                break;
            case "1232":
                pri[0] = 2;
                pri[1] = 3;
                pri[2] = 5;
                pri[3] = 3;
                break;
            case "3212":
                pri[0] = 5;
                pri[1] = 3;
                pri[2] = 2;
                pri[3] = 3;
                break;
            case "1322":
                pri[0] = 2;
                pri[1] = 5;
                pri[2] = 3;
                pri[3] = 3;
                break;
            case "3122":
                pri[0] = 5;
                pri[1] = 2;
                pri[2] = 3;
                pri[3] = 3;
                break;
            default:
                for(int i =0;i<4;i++){
                    if(count[i] == 1)
                        pri[i] = 2;
                    else if(count[i] == 2)
                        pri[i] = 3;
                    else if(count[i] == 3)
                        pri[i] = 5;
                }
                break;
        }
        return pri;
    }

}