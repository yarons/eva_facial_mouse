package com.crea_si.softkeyboard;

import android.graphics.Point;
import android.view.View;
import android.view.ViewGroup;

class ViewUtils {
    /**
     * Determines if given point is inside view
     * @param p - coordinates of point 
     * @param view - view object to compare
     * @return true if the point is within view bounds, false otherwise
     */
    public static boolean isPointInsideView(Point p, View view) {
        if (view == null) return false;
        
        int[] location= new int[2];
        
        view.getLocationOnScreen(location);
        
        if (p.x< location[0] || p.y< location[1]) return false;

        if (location[0] + view.getWidth() < p.x || 
            location[1] + view.getHeight() < p.y) return false;
   
        return true;
    }

    
    /**
     * Given a view, finds recursively a view with the point inside and which have ID
     * @param p - coordinates of point 
     * @param v - view object to start search
     * @return the view which meets these conditions, null otherwise
     */
    public static View findViewWithIdBelowPoint(Point p, View v) {
        if (v.getVisibility() != View.VISIBLE) return null;
        if (!isPointInsideView(p, v)) return null;
        if (!(v instanceof ViewGroup)) {
            if (v.getId() != View.NO_ID) return v;
            return null; 
        }
        
        // is a ViewGroup, iterate children
        ViewGroup vg= (ViewGroup) v;
        
        int childCount= vg.getChildCount();
        
        for (int i= 0; i< childCount; i++) {            
            View result = findViewWithIdBelowPoint(p, vg.getChildAt(i));
            if (result != null) return result;
        }

        return null;
    }
    
    /*
     * for debugging
     */
    public static void dumpViewGroupHierarchy (View v) {
        if (v == null) return;
        
        
        if (v.getId() == View.NO_ID) {
            EVIACAMSOFTKBD.debug("Processing NO_ID View: " + v.toString());
        }
        else {
            EVIACAMSOFTKBD.debug("Processing (id:" + v.getId() + ") " + v.toString());
        }
        
        if (!(v instanceof ViewGroup)) return; 
        
        ViewGroup vg= (ViewGroup) v;
        
        // iterate children
        
        int childCount= vg.getChildCount();
                
        for (int i= 0; i< childCount; i++) {
            dumpViewGroupHierarchy(vg.getChildAt(i));
        }
    }
}
