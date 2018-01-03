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

import com.veniosg.dir.android.util.MediaScannerUtils;
import com.veniosg.dir.mvvm.model.FileHolder;
import com.veniosg.dir.mvvm.model.storage.FileOperation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.veniosg.dir.android.util.Notifier.clearNotification;
import static com.veniosg.dir.android.util.Notifier.showMoveDoneNotification;
import static com.veniosg.dir.android.util.Notifier.showMoveProgressNotification;

public class MoveOperation extends FileOperation<MoveArguments> {
    private final Context context;

    public MoveOperation(Context context) {
        super(new StorageAccessHelperCompat(context));
        this.context = context.getApplicationContext();
    }

    @Override
    protected boolean operate(MoveArguments args) {
        boolean res = true;
        int fileIndex = 0;
        boolean fileMoved;

        File from;
        File toFile;
        File target = args.getTarget();
        List<FileHolder> files = args.getFilesToMove();
        for (FileHolder fh : files) {
            showMoveProgressNotification(fileIndex++, files.size(), getId(),
                    fh.getFile(), target.getPath(), context);

            from = fh.getFile().getAbsoluteFile();
            toFile = new File(target, fh.getName());

            List<String> paths = new ArrayList<>();
            if (from.isDirectory()) {
                MediaScannerUtils.getPathsOfFolder(paths, from);
            }

            // Move
            fileMoved = fh.getFile().renameTo(toFile);

            // Inform media scanner
            if (fileMoved) {
                if (toFile.isDirectory()) {
                    MediaScannerUtils.informPathsDeleted(context, paths);
                    MediaScannerUtils.informFolderAdded(context, toFile);
                } else {
                    MediaScannerUtils.informFileDeleted(context, from);
                    MediaScannerUtils.informFileAdded(context, toFile);
                }
            }

            res &= fileMoved;
        }

        return res;
    }

    @Override
    protected void onStartOperation(MoveArguments args) {
    }

    @Override
    protected void onResult(boolean success, MoveArguments args) {
        showMoveDoneNotification(success, getId(), args.getTarget().getPath(), context);
    }

    @Override
    protected void onAccessDenied() {
        // TODO SDCARD show some toast
    }

    @Override
    protected void onRequestingAccess() {
        clearNotification(getId(), context);
    }

    @Override
    protected boolean needsWriteAccess() {
        return true;
    }
}
