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

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PointF;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.crea_si.eviacam.EVIACAM;

class OrientationManager {

    private final Context mContext;

    // orientation sensors listener. keeps updated the current orientation
    // of the device (independently of the screen orientation)
    private PhysicalOrientation mPhysicalOrientation;
    
    // orientation of the screen
    private int mScreenOrientation= 0;

    // the orientation of the camera
    private final int mCameraOrientation;

    // default (natural) device orientation
    private final int mDeviceNaturalOrientation;

    private static OrientationManager sInstance= null;

    // constructor
    private OrientationManager(Context c, int cameraOrientation) {
        mContext= c; 

        // create physical orientation manager
        mPhysicalOrientation= new PhysicalOrientation(mContext);
        
        // enable sensor listener
        mPhysicalOrientation.enable();
        
        mScreenOrientation= getScreenOrientation(mContext);
        
        mCameraOrientation= cameraOrientation;

        mDeviceNaturalOrientation= doGetDeviceDefaultOrientation(mContext);
    }

    /**
     * Get the "natural" orientation of the device
     *
     * @return Configuration.ORIENTATION_LANDSCAPE or Configuration.ORIENTATION_PORTRAIT
     */
    static public int doGetDeviceDefaultOrientation(Context c) {
        WindowManager windowManager = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);

        Configuration config = c.getResources().getConfiguration();

        int rotation = windowManager.getDefaultDisplay().getRotation();

        if ( ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) &&
                config.orientation == Configuration.ORIENTATION_LANDSCAPE)
                || ((rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) &&
                config.orientation == Configuration.ORIENTATION_PORTRAIT)) {
            return Configuration.ORIENTATION_LANDSCAPE;
        } else {
            return Configuration.ORIENTATION_PORTRAIT;
        }
    }

    /**
     * Initialize orientation manager singleton instance
     *
     * @param c
     * @param cameraOrientation
     *  The orientation of the camera image. The value is the angle that the camera image needs
     *  to be rotated clockwise so it shows correctly on the display in its natural orientation.
     *  It should be 0, 90, 180, or 270.
     */
    public static void init (Context c, int cameraOrientation) {
        if (sInstance != null) {
            throw new RuntimeException("OrientationManager already created");
        }
        sInstance= new OrientationManager(c, cameraOrientation);
    }

    /**
     * Get instance if already available
     *
     * @return instance of null if has not been previously initialized
     */
    public static OrientationManager get() {
        return sInstance;
    }

    public void cleanup() {
        mPhysicalOrientation.disable();
        sInstance= null;
    }

    /**
     * Returns the rotation of the screen from its "natural" orientation. 
     * 
     * The returned value may be Surface.ROTATION_0 (no rotation), Surface.ROTATION_90, 
     * Surface.ROTATION_180, or Surface.ROTATION_270. For example, if a device has a 
     * naturally tall screen, and the user has turned it on its side to go into a 
     * landscape orientation, the value returned here may be either Surface.ROTATION_90 
     * or Surface.ROTATION_270 depending on the direction it was turned. 
     * 
     * The angle is the rotation of the drawn graphics on the screen, which is the opposite 
     * direction of the physical rotation of the device. For example, if the device is 
     * rotated 90 degrees counter-clockwise, to compensate rendering will be rotated by 
     * 90 degrees clockwise and thus the returned value here will be Surface.ROTATION_90.
     * 
     */
    static private int getScreenOrientation(Context c) {
        WindowManager wm= (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display d= wm.getDefaultDisplay();
        switch (d.getRotation()) {
        case Surface.ROTATION_0: return 0;
        case Surface.ROTATION_90: return 90;
        case Surface.ROTATION_180: return 180;
        case Surface.ROTATION_270: return 270;
        default:
            throw new RuntimeException("wrong screen orientation");
        }
    }

    /**
     * Get the "natural" device orientation
     * @return Configuration.ORIENTATION_LANDSCAPE or Configuration.ORIENTATION_PORTRAIT
     */
    public int getDeviceNaturalOrientation() {
        return mDeviceNaturalOrientation;
    }

    /**
     * Get the current (physical) device orientation
     *
     * @return 0 degrees when the device is oriented in its natural position, 90 degrees when
     * its left side is at the top, 180 degrees when it is upside down, and 270 degrees when
     * its right side is to the top.
     */
    public int getDeviceCurrentOrientation () {
        return mPhysicalOrientation.getCurrentOrientation();
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        mScreenOrientation= getScreenOrientation(mContext);
        EVIACAM.debug("Screen rotation changed: " + mScreenOrientation);
    }

    /**
     * Given the physical orientation of the device and the mounting orientation of
     * the camera returns the rotation (clockwise) in degrees that needs to be applied
     * to the image so that the subject appears upright
     *
     * @return
     */
    public int getPictureRotation() {
        int phyRotation = mCameraOrientation - mPhysicalOrientation.getCurrentOrientation();
        if (phyRotation< 0) phyRotation+= 360;
        
        return phyRotation;
    }

    /**
     * Given the screen orientation and the physical orientation of the device,
     * return the rotation that needs to be applied to a motion vector so that the
     * physical motion of the subject matches the motion of the pointer on the screen
     */
    private int getDiffRotation () {
        // calculate equivalent physical device rotation for the current screen orientation
        int equivPhyRotation= 360 - mScreenOrientation;
        if (equivPhyRotation== 360) equivPhyRotation= 0;

        // when is a mismatch between physical rotation and screen orientation
        // need to cancel it out (e.g. activity that forces specific screen orientation
        // but the device has not been rotated)
        int diffRotation= equivPhyRotation -  mPhysicalOrientation.getCurrentOrientation();
        if (diffRotation< 0) diffRotation+= 360;

        return diffRotation;
    }

    /**
     * Given the screen orientation and the physical orientation of the device
     * modifies a given a motion vector so that the physical motion of the subject
     * matches the motion of the pointer on the screen
     */
    public void fixVectorOrientation(PointF motion) {
        int diffRotation= getDiffRotation();
        switch (diffRotation) {
        case 0: 
            // Nothing to be done
            break;
        case 90: {
            float tmp= motion.x;
            motion.x= -motion.y;
            motion.y= tmp;
            break;
        }
        case 180:
            motion.x= -motion.x;
            motion.y= -motion.y;
            break;
        case 270: {
            float tmp= motion.x;
            motion.x= motion.y;
            motion.y= -tmp;
            break;
        }
        default:
            throw new RuntimeException("wrong diffRotation");
        }
    }
}
