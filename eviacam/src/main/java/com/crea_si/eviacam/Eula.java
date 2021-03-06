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
package com.crea_si.eviacam;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Eula {

    public interface Listener {
        void onAcceptEula();
        void onCancelEula();
    }
    private static String EULA_PREFIX = "eula_";

    static
    public void acceptEula (final Activity a, final Listener l) {
        // the eulaKey changes every time you increment the version number
        final String eulaKey = EULA_PREFIX + BuildConfig.VERSION_CODE;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(a);
        if (prefs.getBoolean(eulaKey, false)) {
            l.onAcceptEula();
            return;
        }

        // Show the Eula
        String title = a.getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME;
        String message = a.getString(R.string.eula);

        AlertDialog.Builder builder = new AlertDialog.Builder(a)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Mark this version as read.
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean(eulaKey, true);
                    editor.commit();
                    dialogInterface.dismiss();
                    l.onAcceptEula();
                }
            })
            .setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    l.onCancelEula();
                }
            });
        builder.create().show();
    }
}
