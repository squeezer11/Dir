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
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import com.veniosg.dir.R;
import com.veniosg.dir.android.fragment.FileListFragment;
import com.veniosg.dir.android.util.MediaScannerUtils;
import com.veniosg.dir.android.view.toast.ToastFactory;
import com.veniosg.dir.mvvm.model.FileHolder;
import com.veniosg.dir.mvvm.model.storage.FileOperation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.veniosg.dir.android.util.DocumentFileUtils.findFile;
import static com.veniosg.dir.android.util.FileUtils.delete;

public class DeleteOperation extends FileOperation<DeleteArguments> {
    private final Context context;
    private final ToastFactory toastFactory;
    private final Handler mainThreadHandler;
    private ProgressDialog dialog;

    public DeleteOperation(Context context) {
        super(new StorageAccessManagerCompat(context));
        this.mainThreadHandler = new Handler(context.getMainLooper());
        this.toastFactory = new ToastFactory(context);
        this.context = context.getApplicationContext();

        runOnUi(() -> dialog = new ProgressDialog(context));
    }

    @Override
    protected boolean operate(DeleteArguments args) {
        boolean allSucceeded = true;

        for (FileHolder fh : args.getVictims()) {
            File tbd = fh.getFile();
            List<String> paths = getPathsUnder(tbd);

            boolean deleted = delete(tbd);
            allSucceeded &= deleted;

            if (deleted) MediaScannerUtils.informPathsDeleted(context, paths);
        }
        return allSucceeded;
    }

    @Override
    protected boolean operateSaf(DeleteArguments args) {
        boolean allSucceeded = true;

        for (FileHolder fh : args.getVictims()) {
            DocumentFile tbd = findFile(context, fh.getFile());
            List<String> paths = getPathsUnder(fh.getFile());

            boolean deleted = tbd != null && tbd.delete();
            allSucceeded &= deleted;

            if (deleted) MediaScannerUtils.informPathsDeleted(context, paths);
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
            if (success) {
                toastFactory.deleteSuccess().show();
                FileListFragment.refresh(context, args.getTarget());
            } else {
                toastFactory.deleteFailure().show();
            }

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

    @NonNull
    private List<String> getPathsUnder(File file) {
        List<String> paths = new ArrayList<>();
        if (file.isDirectory()) {
            MediaScannerUtils.getPathsOfFolder(paths, file);
        } else {
            paths.add(file.getAbsolutePath());
        }
        return paths;
    }
}
