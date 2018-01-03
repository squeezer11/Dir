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

package com.veniosg.dir.android.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.veniosg.dir.R;
import com.veniosg.dir.android.fragment.FileListFragment;
import com.veniosg.dir.android.util.MediaScannerUtils;
import com.veniosg.dir.mvvm.model.FileHolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.makeText;
import static com.veniosg.dir.android.util.FileUtils.delete;

class DeleteAsyncTask extends AsyncTask<FileHolder, Void, DeleteAsyncTask.Result> {
    private final Context context;
    private final ProgressDialog dialog;

    DeleteAsyncTask(@NonNull Context context) {
        this.context = context;
        dialog = new ProgressDialog(context);
    }

    @Override
    protected void onPreExecute() {
        dialog.setMessage(context.getString(R.string.deleting));
        dialog.setIndeterminate(true);
        dialog.show();
    }

    @Override
    protected Result doInBackground(FileHolder... params) {
        boolean allSucceeded = true;

        for (FileHolder fh : params) {
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
        return new Result(allSucceeded, params[0].getFile().getParentFile());
    }

    @Override
    protected void onPostExecute(Result result) {
        makeText(dialog.getContext(), result.success ?
                R.string.delete_success : R.string.delete_failure, LENGTH_LONG)
                .show();
        FileListFragment.refresh(context, result.parent);
        dialog.dismiss();
    }

    static class Result {
        private final boolean success;
        private final File parent;

        private Result(boolean success, File parent) {
            this.success = success;
            this.parent = parent;
        }
    }
}
