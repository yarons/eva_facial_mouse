/*
 * Enable Viacam for Android, a camera based mouse emulator
 *
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.crea_si.eviacam.service;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;

import com.crea_si.eviacam.R;

import org.opencv.android.MyBaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.MyCameraBridgeViewBase;
import org.opencv.android.MyJavaCameraView;
import org.opencv.android.MyOpenCVLoader;

/**
 * Helper class to detect and launch the installation of the OpenCV manager
 */
public class OpenCVInstallHelper {

    interface Listener {
        void onOpenCVInstallSuccess();
        void onOpenCVInstallCancel();
    }

    private final Context mContext;
    private final Listener mListener;

    /* Callback for openCV initialization */
    private final MyBaseLoaderCallback mLoaderCallback;

    /* openCV capture & view facility */
    private MyCameraBridgeViewBase mCameraView;

    /* Constructor **/
    public OpenCVInstallHelper(Context c, Listener l) {
        mContext= c;
        mListener= l;
        mLoaderCallback= new MyBaseLoaderCallback(c) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        manageOpenCVInstallSuccess();
                        break;
                    case LoaderCallbackInterface.INSTALL_CANCELED:
                        manageOpenCVInstallCancel();
                        break;
                    default:
                        super.onManagerConnected(status);
                }
            }
        };

        mCameraView = new MyJavaCameraView(mContext, MyCameraBridgeViewBase.CAMERA_ID_FRONT);
        MyOpenCVLoader.initAsync(MyOpenCVLoader.OPENCV_VERSION_2_4_9, mContext, mLoaderCallback);
    }

    public void cleanup() {
        if (mCameraView != null) {
            mCameraView.disableView();
            mCameraView= null;
        }
    }
    
    /* Handles the case when openCV has been properly installed */
    private void manageOpenCVInstallSuccess() {
        mListener.onOpenCVInstallSuccess();
    }
    
    /* Handles the case when openCV installation has been cancelled */
    private void manageOpenCVInstallCancel() {
        Resources res= mContext.getResources();
        AlertDialog installCancelDlg = new AlertDialog.Builder(mContext).create();
        installCancelDlg.setTitle(res.getText(R.string.installation_cancelled));
        installCancelDlg.setMessage(res.getString(R.string.app_name) + " " +
                res.getText(R.string.needs_opencv_retry));
        installCancelDlg.setCancelable(false); // This blocks the 'BACK' button
        installCancelDlg.setButton(AlertDialog.BUTTON_POSITIVE,
                res.getText(R.string.retry), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                MyOpenCVLoader.initAsync(MyOpenCVLoader.OPENCV_VERSION_2_4_9, 
                                         OpenCVInstallHelper.this.mContext, mLoaderCallback);
            }
        });
        installCancelDlg.setButton(AlertDialog.BUTTON_NEGATIVE,
                res.getText(android.R.string.cancel), new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                OpenCVInstallHelper.this.mListener.onOpenCVInstallCancel();
            }
        });
        
        installCancelDlg.show();
    }
}