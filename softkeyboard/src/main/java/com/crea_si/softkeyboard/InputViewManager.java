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
package com.crea_si.softkeyboard;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.os.Handler;
import android.os.SystemClock;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

public class InputViewManager {
    /*
     * Layout types
     */
    public static final int NONE_LAYOUT = 0;
    public static final int QWERTY_LAYOUT = 1;
    public static final int SYMBOLS_LAYOUT = 2;
    public static final int SYMBOLS_SHIFT_LAYOUT = 3;
    public static final int NAVIGATION_LAYOUT = 4;
    
    /*
     * Qwerty layout subtypes
     */
    private static final int QWERTY_NONE = 0;
    private static final int QWERTY_EN = 1;
    private static final int QWERTY_ES = 2;
    private static final int QWERTY_CA = 3;

    final private InputMethodService mIMEService;
    final private InputMethodManager mInputMethodManager;

    private final Handler mHandler= new Handler();

    private LatinKeyboardView mInputView;

    private int mCurrentLayout= NONE_LAYOUT;
    private int mCurrentQwertySubtype = QWERTY_NONE;

    private LatinKeyboard mQwertyKeyboard;
    private LatinKeyboard mSymbolsKeyboard;
    private LatinKeyboard mSymbolsShiftedKeyboard;    
    private LatinKeyboard mNavigationKeyboard;

    private boolean mCapsLock;
    private long mLastShiftTime= 0;

    public InputViewManager(InputMethodService ime) {
        mIMEService= ime;
        mInputMethodManager = (InputMethodManager) ime.getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    /**
     * Select the layout that will be enabled when required calling 
     * enableSelectedLayout
     * 
     * @param type type of layout
     */
    public void selectLayout(int type) {
        if (type == mCurrentLayout) return;

        switch (type) {
        case QWERTY_LAYOUT:
            if (mQwertyKeyboard== null) {
                selectSubtype (mInputMethodManager.getCurrentInputMethodSubtype());
            }
            mCurrentLayout= QWERTY_LAYOUT;
            break;
        case SYMBOLS_LAYOUT:
            if (mSymbolsKeyboard== null) {
                mSymbolsKeyboard = new LatinKeyboard(mIMEService, R.xml.symbols);
            }
            mSymbolsKeyboard.setShifted(false);
            mCurrentLayout= SYMBOLS_LAYOUT;
            break;
        case SYMBOLS_SHIFT_LAYOUT:
            if (mSymbolsShiftedKeyboard== null) {
                mSymbolsShiftedKeyboard = new LatinKeyboard(mIMEService, R.xml.symbols_shift);
            }
            mSymbolsShiftedKeyboard.setShifted(true);
            mCurrentLayout= SYMBOLS_SHIFT_LAYOUT;
            break;
        case NAVIGATION_LAYOUT:
            if (mNavigationKeyboard== null) {
                mNavigationKeyboard = new LatinKeyboard(mIMEService, R.xml.navigation);
            }
            mCurrentLayout= NAVIGATION_LAYOUT;
            break;
        default:
            throw new IllegalArgumentException();
        }
    }
    
    public int getSelectedLayout() {
        return mCurrentLayout;
    }
    
    /**
     * Select qwerty keyboard according of a specific subtype (language)
     *
     * TODO: there is probably a better way to choose between different
     * keyboard layouts when changing the subtype (e.g. when changing language)
     */
    public void selectSubtype(InputMethodSubtype subtype) {
        final String locale= subtype.getLocale();
        if (locale.compareTo("es")== 0) {
            if (mCurrentQwertySubtype != QWERTY_ES) {
                mQwertyKeyboard = new LatinKeyboard(mIMEService, R.xml.qwerty_es);
                mCurrentQwertySubtype= QWERTY_ES;
            }
        }
        else if (locale.compareTo("ca")== 0) {
            if (mCurrentQwertySubtype != QWERTY_CA) {
                mQwertyKeyboard = new LatinKeyboard(mIMEService, R.xml.qwerty_ca);
                mCurrentQwertySubtype= QWERTY_CA;
            }
        }
        else if (mCurrentQwertySubtype != QWERTY_EN) {
            mQwertyKeyboard = new LatinKeyboard(mIMEService, R.xml.qwerty);
            mCurrentQwertySubtype= QWERTY_EN;
        }
    }

    /**
     * Create the KeyboardView
     * 
     * @param listener the listener
     * @return the view
     */
    public View createView(KeyboardView.OnKeyboardActionListener listener) {
        mInputView= (LatinKeyboardView)
                mIMEService.getLayoutInflater().inflate(R.layout.input, null);
        mInputView.setOnKeyboardActionListener(listener);
        return mInputView;
    }

    /*
     * Helper to get the currently selected keyboard 
     */
    private Keyboard getSelectedKeyboard() {
        switch (mCurrentLayout) {
        case QWERTY_LAYOUT:         return mQwertyKeyboard;
        case SYMBOLS_LAYOUT:        return mSymbolsKeyboard;
        case SYMBOLS_SHIFT_LAYOUT:  return mSymbolsShiftedKeyboard;
        case NAVIGATION_LAYOUT:     return mNavigationKeyboard;
        default:                    return null;
        }
    }
    
    /**
     * Enable the selected keyboard layout & subtype
     * 
     * @param subtype (can be null)
     */
    public void enableSelected(InputMethodSubtype subtype) {
        if (mInputView== null) return;
        mInputView.setKeyboard(getSelectedKeyboard());
        mInputView.closing();
        if (subtype == null) {
            subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        }
        mInputView.setSubtype(subtype);
    }

    /*
     * Cleanup when closing the keyboard
     */
    public void closing() {
        if (mInputView== null) return;
        mInputView.closing();
    }
    
    /*
     * Handle backspace key
     */
    public boolean handleBack() {
        return mInputView != null && mInputView.handleBack();
    }
    
    /*
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    public void updateShiftKeyState(EditorInfo attr) {
        if (mInputView == null) return;

        // Applicable only to the qwerty keyboard
        if (mCurrentLayout != QWERTY_LAYOUT) return;
        if (attr == null) attr = mIMEService.getCurrentInputEditorInfo();
        
        int caps = 0;
        if (attr.inputType != InputType.TYPE_NULL) {
            caps = mIMEService.getCurrentInputConnection().getCursorCapsMode(attr.inputType);
        }
        mInputView.setShifted(mCapsLock || caps != 0);
    }
    
    /*
     * Update label for the enter key according to what editor says
     */
    public void updateEnterLabel(EditorInfo attr) {
        if (mInputView == null) return;
        LatinKeyboard current= (LatinKeyboard) mInputView.getKeyboard();
        current.setImeOptions(mIMEService.getResources(), attr.imeOptions);
    }
    
    /*
     * Handle when user press shift key
     */
    public void handleShift() {
        if (mInputView == null) return;
        
        if (mCurrentLayout == QWERTY_LAYOUT) {
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !isShifted());
        } else if (mCurrentLayout == SYMBOLS_LAYOUT) {
            selectLayout(SYMBOLS_SHIFT_LAYOUT);
            enableSelected(null);
        } else if (mCurrentLayout == SYMBOLS_SHIFT_LAYOUT) {
            selectLayout(SYMBOLS_LAYOUT);
            enableSelected(null);
        }
    }
    
    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }
    
    public int[] getKeyboardLocationOnScreen() {
        if (mInputView == null) return null;
        int coord[]= new int[2];
        mInputView.getLocationOnScreen(coord);
        
        return coord;
    }
    
    public Key getKeyBelow (int x, int y) {
        // has clicked inside the keyboard?
        if (mInputView == null) return null;

        Keyboard kbd= mInputView.getKeyboard();
        if (kbd == null) return null;

        // keys near the given point
        int[] keys= kbd.getNearestKeys ((int) x, (int) y);

        for (int i : keys) {
            Keyboard.Key k= kbd.getKeys().get(i);
            if (k.isInside(x, y)) return k;
        }

        return null;
    }

    /**
     * Perform a click on the keyboard
     * @param x - abscissa coordinate relative to the view of the keyboard
     * @param y - ordinate coordinate relative to the view of the keyboard
     * @return - true if click performed
     */
    public boolean performClick (int x, int y) {
        // has clicked inside the keyboard?
        if (mInputView == null) return false;

        long time= SystemClock.uptimeMillis();
        MotionEvent down= MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, y, 0);

        // dispatch down event
        mInputView.dispatchTouchEvent(down);

        // program up event after some ms
        mDelayedX= x;
        mDelayedY= y;
        mHandler.postDelayed(mDelayedEvent, 150);

        return true;
    }

    /* Runnable and parameters to send the UP event */
    private int mDelayedX, mDelayedY;
    private Runnable mDelayedEvent= new Runnable() {
        @Override
        public void run() {
            long time= SystemClock.uptimeMillis();
            MotionEvent up=
                    MotionEvent.obtain(time, time, MotionEvent.ACTION_UP, mDelayedX, mDelayedY, 0);
            if (mInputView!= null) mInputView.dispatchTouchEvent(up);
        }
    };
    
    /*
     * Return whether the qwerty layout is shifted
     */
    public boolean isShifted() {
        if (mCurrentLayout != QWERTY_LAYOUT) return false;
        return mInputView != null && mInputView.isShifted();
    }
    
    /*
     * Handle layout change
     */
    public void handleModeChange() {
        if (mInputView == null) return;

        if (mCurrentLayout == QWERTY_LAYOUT) {
            selectLayout(SYMBOLS_LAYOUT);
        } else {
            selectLayout(QWERTY_LAYOUT);
        }
        enableSelected(null);
    }
    
    /*
     * Enable navigation keyboard
     */
    public void setNavigationKeyboard() {
        if (mInputView == null) return;
        if (mCurrentLayout == NAVIGATION_LAYOUT) return;

        selectLayout(NAVIGATION_LAYOUT);
        enableSelected(null);
    }
}
