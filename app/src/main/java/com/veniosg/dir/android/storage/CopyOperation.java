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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;

import static com.veniosg.dir.android.util.FileUtils.createUniqueCopyName;
import static com.veniosg.dir.android.util.FileUtils.countFilesUnder;
import static com.veniosg.dir.android.util.Notifier.clearNotification;
import static com.veniosg.dir.android.util.Notifier.showCopyDoneNotification;
import static com.veniosg.dir.android.util.Notifier.showCopyProgressNotification;

public class CopyOperation extends FileOperation<CopyArguments> {
    private static final int COPY_BUFFER_SIZE = 32 * 1024;

    private final Context context;

    public CopyOperation(Context context) {
        super(new StorageAccessHelperCompat(context));
        this.context = context.getApplicationContext();
    }

    @Override
    protected boolean operate(CopyArguments args) {
        List<FileHolder> files = args.getFilesToCopy();
        File to = args.getTarget();

        int fileCount = countFilesUnder(files);
        int filesCopied = 0;

        // Try copying
        for (FileHolder origin : files) {
            File dest = createUniqueCopyName(context, to, origin.getName());
            filesCopied = copyCore(filesCopied, fileCount, origin.getFile(), dest, files.hashCode());

            if (origin.getFile().isDirectory()) {
                MediaScannerUtils.informFolderAdded(context, dest);
            } else {
                MediaScannerUtils.informFileAdded(context, dest);
            }
        }

        return filesCopied == fileCount;
    }

    @Override
    protected void onStartOperation(CopyArguments args) {
    }

    @Override
    protected void onResult(boolean success, CopyArguments args) {
        showCopyDoneNotification(success, args.getFilesToCopy().hashCode(), args.getTarget().getPath(), context);
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

    /**
     * Copy a file.
     *
     * @param filesCopied Initial value of how many files have been copied.
     * @param oldFile     File to copy.
     * @param newFile     The file to be created.
     * @param notId       The id for the progress notification.
     * @return The new filesCopied count.
     */
    private int internalCopyFile(int filesCopied, int fileCount, File oldFile, File newFile, int notId) {
        showCopyProgressNotification(filesCopied, fileCount, notId, oldFile, newFile.getParent(), context);

        try {
            FileInputStream input = new FileInputStream(oldFile);
            FileOutputStream output = new FileOutputStream(newFile);

            byte[] buffer = new byte[COPY_BUFFER_SIZE];

            while (true) {
                int bytes = input.read(buffer);

                if (bytes <= 0) {
                    break;
                }

                output.write(buffer, 0, bytes);
            }

            output.close();
            input.close();

        } catch (Exception e) {
            return filesCopied;
        }
        return filesCopied + 1;
    }

    /**
     * Recursively copy a folder.
     *
     * @param filesCopied Initial value of how many files have been copied.
     * @param oldFile     Folder to copy.
     * @param newFile     The dir to be created.
     * @param notId       The id for the progress notification.
     * @return The new filesCopied count.
     */
    private int copyCore(int filesCopied, int fileCount, File oldFile, File newFile, int notId) {
        if (oldFile.isDirectory()) {
            // Create directory if it doesn't exist
            if (!newFile.exists()) newFile.mkdir();

            // list all the directory contents
            String files[] = oldFile.list();

            for (String file : files) {
                // construct the src and dest file structure
                File srcFile = new File(oldFile, file);
                File destFile = new File(newFile, file);
                // recursive copy
                filesCopied = copyCore(filesCopied, fileCount, srcFile, destFile, notId);
            }
        } else {
            filesCopied = internalCopyFile(filesCopied, fileCount, oldFile, newFile, notId);
        }

        return filesCopied;
    }
}
