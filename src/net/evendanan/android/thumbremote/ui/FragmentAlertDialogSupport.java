/*
 * Copyright (C) 2011 The Android Open Source Project
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

package net.evendanan.android.thumbremote.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.SeekBar;

import net.evendanan.android.thumbremote.R;

public class FragmentAlertDialogSupport extends DialogFragment {

    public static final int DIALOG_NO_PASSWORD = 1;
    public static final int DIALOG_NO_SERVER = 2;
    public static final int DIALOG_MEDIA_TIME_SEEK = 3;
    public static final int DIALOG_DISCOVERYING = 4;

    public static FragmentAlertDialogSupport newInstance(int type) {
        FragmentAlertDialogSupport frag = new FragmentAlertDialogSupport();
        Bundle args = new Bundle();
        args.putInt("type", type);
        frag.setArguments(args);
        return frag;
    }

    private Dialog createCredentialsRequiredDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.need_creds_title)
                .setMessage(R.string.need_creds_message)
                .setCancelable(true)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ((RemoteUiActivity) getActivity()).startSetupActivity();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
            
        AlertDialog alert = builder.create();
        
        return alert;
    }

    private Dialog createMediaTimeSeekDialog() {
        final Dialog seeker = new Dialog(getActivity(), R.style.Popup);
        seeker.setContentView(R.layout.custom_time_selection);
        seeker.findViewById(R.id.seeker_close_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                seeker.dismiss();
            }
        });
        seeker.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                ((RemoteUiActivity) getActivity()).mMediaSeekBar = null;
            }
        });

        SeekBar seekBar = (SeekBar) seeker.findViewById(R.id.time_seek_bar);
        seekBar.setOnSeekBarChangeListener((RemoteUiActivity) getActivity());

        return seeker;
    }

    private Dialog createNoServerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.no_server_found_title)
                .setMessage(R.string.no_server_found_message)
                .setCancelable(true)
                .setPositiveButton(R.string.no_server_found_action_manual,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ((RemoteUiActivity) getActivity()).startSetupActivity();
                                dialog.dismiss();
                            }
                        })
                .setNeutralButton(R.string.no_server_found_action_rescan,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                ((RemoteUiActivity) getActivity()).rescanForServers();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton(R.string.no_server_found_action_neither,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();

        return alert;
    }

    private Dialog creatDiscoveryProgressDialog() {
        ProgressDialog p = new ProgressDialog(getActivity());
        p.setTitle(R.string.discoverying_dialog_title);
        p.setMessage(getString(R.string.discoverying_dialog_message));
        p.setIndeterminate(true);
        p.setCancelable(false);
        
        return p;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int id = getArguments().getInt("type");

        Dialog dialog = null;
        switch (id)
        {
            case DIALOG_NO_PASSWORD:
                dialog = createCredentialsRequiredDialog();
                break;
            case DIALOG_NO_SERVER:
                dialog = createNoServerDialog();
                break;
            case DIALOG_MEDIA_TIME_SEEK:
                dialog = createMediaTimeSeekDialog();
                break;
            case DIALOG_DISCOVERYING:
                dialog = creatDiscoveryProgressDialog();
                break;
        }

        if (dialog != null)
            dialog.getWindow().setWindowAnimations(R.style.BoxeeInOut);
        
        return null;
    }   
}
