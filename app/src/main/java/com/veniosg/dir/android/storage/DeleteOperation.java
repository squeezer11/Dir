/*
 * Copyright (C) 2018 George Venios
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.veniosg.dir.android.storage;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.veniosg.dir.R;
import com.veniosg.dir.android.fragment.FileListFragment;
import com.veniosg.dir.android.util.MediaScannerUtils;
import com.veniosg.dir.mvvm.model.FileHolder;
import com.veniosg.dir.mvvm.model.storage.FileOperation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.makeText;
import static com.veniosg.dir.android.util.FileUtils.delete;

public class DeleteOperation extends FileOperation<DeleteArguments> {
    private final Context context;
    private final Handler mainThreadHandler;
    private ProgressDialog dialog;

    public DeleteOperation(Context context) {
        super(new StorageAccessHelperCompat(context));
        this.mainThreadHandler = new Handler(context.getMainLooper());
        this.context = context.getApplicationContext();

        runOnUi(() -> dialog = new ProgressDialog(context));
    }

    @Override
    protected boolean operate(DeleteArguments args) {
        boolean allSucceeded = true;

        for (FileHolder fh : args.getVictims()) {
            File tbd = fh.getFile();
            boolean isDir = tbd.isDirectory();
            List<String> paths = new ArrayList<>();

            if (isDir) {
                MediaScannerUtils.getPathsOfFolder(paths, tbd);
            }

            allSucceeded &= delete(tbd);

            if (isDir) {
                MediaScannerUtils.informPathsDeleted(context, paths);
            } else {
                MediaScannerUtils.informFileDeleted(context, tbd);
            }
        }
        return allSucceeded;
    }

    @Override
    protected void onStartOperation(DeleteArguments args) {
        runOnUi(() -> {
            dialog.setCanceledOnTouchOutside(false);
            dialog.setCancelable(false);
            dialog.setMessage(context.getString(R.string.deleting));
            dialog.setIndeterminate(true);
            dialog.show();
        });
    }

    @Override
    protected void onResult(boolean success, DeleteArguments args) {
        runOnUi(() -> {
            makeText(dialog.getContext(), success ?
                    R.string.delete_success : R.string.delete_failure, LENGTH_LONG)
                    .show();
            FileListFragment.refresh(context, args.getTarget());
            dialog.dismiss();
        });
    }

    @Override
    protected void onAccessDenied() {
        runOnUi(() -> dialog.dismiss());
    }

    @Override
    protected void onRequestingAccess() {
        runOnUi(() -> dialog.cancel());
    }

    @Override
    protected boolean needsWriteAccess() {
        return true;
    }

    private void runOnUi(Runnable runnable) {
        if (Looper.myLooper() != context.getMainLooper()) {
            mainThreadHandler.post(runnable);
        } else {
            runnable.run();
        }
    }
}
