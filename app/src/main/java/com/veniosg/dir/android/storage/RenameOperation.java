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

import android.content.Context;

import com.veniosg.dir.R;
import com.veniosg.dir.android.util.MediaScannerUtils;
import com.veniosg.dir.mvvm.model.storage.FileOperation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.veniosg.dir.android.util.MediaScannerUtils.getPathsOfFolder;

public class RenameOperation extends FileOperation<RenameArguments> {
    private Context context;
    private List<String> affectedPaths = new ArrayList<>();

    public RenameOperation(Context context) {
        super(new StorageAccessHelperCompat(context));
        this.context = context;
    }

    @Override
    protected boolean operate(RenameArguments args) {
        File from = args.getFileToRename();
        File dest = args.getTarget();

        if (!dest.exists()) {
            boolean renamed = from.renameTo(dest);
            if (!renamed) return false;
        }

        return true;
    }

    @Override
    protected void onStartOperation(RenameArguments args) {
        File from = args.getFileToRename();
        if (from.isDirectory()) {
            getPathsOfFolder(affectedPaths, from);
        } else {
            affectedPaths.add(from.getAbsolutePath());
        }
    }

    @Override
    protected void onResult(boolean success, RenameArguments args) {
        makeText(
                context,
                success ? R.string.rename_success : R.string.rename_failure,
                LENGTH_SHORT
        ).show();

        if (success) {
            File from = args.getFileToRename();
            File dest = args.getTarget();
            MediaScannerUtils.informPathsDeleted(context, affectedPaths);
            if (dest.isFile()) {
                MediaScannerUtils.informFileAdded(context, dest);
            } else {
                MediaScannerUtils.informFolderAdded(context, dest);
            }
        }
    }

    @Override
    protected void onAccessDenied() {
        // TODO SDCARD show some toast
    }

    @Override
    protected void onRequestingAccess() {
    }

    @Override
    protected boolean needsWriteAccess() {
        return true;
    }
}
