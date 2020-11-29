package com.example.minibluetooth;

import android.app.Application;
import android.os.Handler;

public class MainApplication extends Application 
{
    protected static Handler clientHandler;
    protected static Handler serverHandler;
    protected static ClientThread clientThread;
    protected static ServerThread serverThread;
    protected static ProgressData progressData = new ProgressData();

    protected static final String TEMP_IMAGE_FILE_NAME = "btimage.jpg";
    protected static final int PICTURE_RESULT_CODE = 1234;
    protected static final int IMAGE_QUALITY = 100;

    @Override
    public void onCreate() 
    {
    	super.onCreate();
    }
}
