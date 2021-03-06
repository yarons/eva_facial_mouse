/*
 * Copyright (C) 2015 Cesar Mauri Loba (CREA Software Systems)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.crea_si.eviacam.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;

/**
 * eviacam slave mode
 * 
 * TODO: improve service security
 */
public class SlaveMode implements ServiceConnection {
    /**
     * In slave mode there several possibilities when started
     */
    public static final int MOUSE= 0;
    public static final int GAMEPAD_ABSOLUTE= 1;
    public static final int GAMEPAD_RELATIVE= 2;

    private static final String TAG= "eviacam_api";
    
    private static final String REMOTE_PACKAGE= "com.crea_si.eviacam.service";
    private static final String REMOTE_SERVICE= REMOTE_PACKAGE + ".SlaveModeService";
    private static final String REMOTE_PREFERENCE_ACTIVITY= 
            REMOTE_PACKAGE + ".SlaveModePreferencesActivity";
    private static final String REMOTE_GAMEPAD_PREFERENCE_ACTIVITY= 
            REMOTE_PACKAGE + ".GamepadPreferencesActivity";
    private static final String REMOTE_MOUSE_PREFERENCE_ACTIVITY= 
            REMOTE_PACKAGE + ".MousePreferencesActivity";
    
    private final Context mContext;
    private final SlaveModeConnection mSlaveModeConnection;
    
    // binder (proxy) with the remote input method service
    private ISlaveMode mSlaveMode;

    /**
     * Connect to the remote eviacam slave mode service
     * 
     * @param c context
     * @param callback which will receive the instance of a SlaveMode class
     */
    public static void connect(Context c, SlaveModeConnection callback) {
        Log.d(TAG, "Attempt to bind to EViacam API");
        Intent intent= new Intent(REMOTE_SERVICE);
        intent.setPackage(REMOTE_PACKAGE);
        try {
            if (!c.bindService(intent, new SlaveMode(c, callback), Context.BIND_AUTO_CREATE)) {
                Log.d(TAG, "Cannot bind remote API");
            }
        }
        catch(SecurityException e) {
            Log.d(TAG, "Cannot bind remote API. Security exception.");
        }
    }
    
    /**
     * Disconnect from eviacam slave mode service
     */
    public void disconnect() {
        mContext.unbindService(this);
        mSlaveMode = null;
    }

    private SlaveMode (Context c, SlaveModeConnection callback) {
        mContext= c;
        mSlaveModeConnection= callback;
    }
    
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // This is called when the connection with the service has been
        // established, giving us the object we can use to
        // interact with the service.
        Log.d(TAG, "EViacam API:onServiceConnected: " + name.toString());
        mSlaveMode = ISlaveMode.Stub.asInterface(service);
        mSlaveModeConnection.onConnected(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        Log.d(TAG, "EViacam API:onServiceDisconnected");
        mContext.unbindService(this);
        mSlaveModeConnection.onDisconnected();
        mSlaveMode = null;
    }
    
    /**
     * Starts eviacam in slave mode
     */
    public boolean start() {
        if (mSlaveMode== null) return false;
        try {
            return mSlaveMode.start();
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.start: exception: " + e.getMessage()); 
        }
        return false;
    }
    
    /**
     * Stops eviacam slave mode
     */
    public void stop() {
        try {
            mSlaveMode.stop();
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.stop: exception: " + e.getMessage());
        }
    }
    
    /**
     * Set operation mode
     * 
     * @param mode
     *  MOUSE
     *  ABSOLUTE_PAD
     *  RELATIVE_PAD
     */
    public void setOperationMode(int mode) {
        try {
            mSlaveMode.setOperationMode(mode);
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.setOperationMode: exception: " + e.getMessage());
        }
    }
    
    /**
     * Register the listener for gamepad events
     * 
     * @param listener the listener
     * @return true if registration succeeded, false otherwise
     */
    public boolean registerGamepadListener(IGamepadEventListener listener) {
        if (mSlaveMode== null) return false;
        try {
            return mSlaveMode.registerGamepadListener(new IGamepadEventListenerWrapper(listener));
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.registerGamepadListener: exception: " + e.getMessage());
        }
        return false;
    }

    /**
     * Unregister the listener for gamepad events (if any)
     */
    public void unregisterGamepadListener() {
        if (mSlaveMode== null) return;
        try {
            mSlaveMode.unregisterGamepadListener();
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.unregisterGamepadListener: exception: " + e.getMessage());
        }
    }
    
    /**
     * Register the listener for mouse events
     * 
     * @param listener the listener
     * @return true if registration succeeded, false otherwise
     */
    public boolean registerMouseListener(IMouseEventListener listener) {
        if (mSlaveMode== null) return false;
        try {
            return mSlaveMode.registerMouseListener(new IMouseEventListenerWrapper(listener));
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.registerMouseListener: exception: " + e.getMessage());
        }
        return false;
    }

    /**
     * Unregister the listener for mouse events (if any)
     */
    public void unregisterMouseListener() {
        if (mSlaveMode== null) return;
        try {
            mSlaveMode.unregisterMouseListener();
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.unregisterMouseListener: exception: " + e.getMessage());
        }
    }
    
    /**
     * Open the root preferences activity for the slave mode
     */
    public static void openSettingsActivity(Context c) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(REMOTE_PACKAGE, REMOTE_PREFERENCE_ACTIVITY));
        c.startActivity(intent);
    }
    
    /**
     * Open gamepad preferences activity
     */
    public static void openGamepadSettingsActivity(Context c) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(REMOTE_PACKAGE, REMOTE_GAMEPAD_PREFERENCE_ACTIVITY));
        c.startActivity(intent);
    }

    /**
     * Open mouse preferences activity
     */
    public static void openMouseSettingsActivity(Context c) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(REMOTE_PACKAGE, REMOTE_MOUSE_PREFERENCE_ACTIVITY));
        intent.putExtra("slave_mode", true);
        c.startActivity(intent);
    }

    /*
     * Get configuration parameters for gamepad
     */
    /*
    public GamepadParams getGamepadParams() {
        if (mSlaveMode== null) return null;
        try {
            return mSlaveMode.getGamepadParams();
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.getGamepadParams: exception: " + e.getMessage());
        }
        return null;
    }*/

    /*
     * Set configuration parameters for gamepad
     */
    /*
    public void setGamepadParams(GamepadParams p) {
        if (mSlaveMode== null) return;
        try {
            mSlaveMode.setGamepadParams(p);
        } catch (RemoteException e) {
            Log.d(TAG, "SlaveMode.getGamepadParams: exception: " + e.getMessage());
        }
    }*/

    /*
     * Stub implementation for gamepad event listener
     */
    private class IGamepadEventListenerWrapper extends IGamepadEventListener.Stub {
        private final IGamepadEventListener mListener;
        public IGamepadEventListenerWrapper(IGamepadEventListener l) {
            mListener= l;
        }
        
        @Override
        public void buttonPressed(int button) throws RemoteException {
            mListener.buttonPressed(button);
        }

        @Override
        public void buttonReleased(int button) throws RemoteException {
            mListener.buttonReleased(button);    
        }        
    }

    /*
     * Stub implementation for mouse event listener
     */
    private class IMouseEventListenerWrapper extends IMouseEventListener.Stub {
        private final IMouseEventListener mListener;
        public IMouseEventListenerWrapper(IMouseEventListener l) {
            mListener= l;
        }

        @Override
        public void onMouseEvent(MotionEvent e) throws RemoteException {
            mListener.onMouseEvent(e);
        }
    }
}
