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

import com.veniosg.dir.android.fragment.FileListFragment;
import com.veniosg.dir.android.util.MediaScannerUtils;
import com.veniosg.dir.android.util.Notifier;
import com.veniosg.dir.mvvm.model.FileHolder;
import com.veniosg.dir.mvvm.model.storage.FileOperation;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.veniosg.dir.android.util.FileUtils.countFilesUnder;
import static com.veniosg.dir.android.util.FileUtils.delete;
import static com.veniosg.dir.android.util.Logger.log;
import static com.veniosg.dir.android.util.Notifier.clearNotification;
import static com.veniosg.dir.android.util.Notifier.showCompressProgressNotification;

public class CompressOperation extends FileOperation<CompressArguments> {
    private static final int BUFFER_SIZE = 1024;

    private final Context context;

    public CompressOperation(Context context) {
        super(new StorageAccessHelperCompat(context));
        this.context = context;
    }

    @Override
    protected boolean operate(CompressArguments args) {
        List<FileHolder> toCompress = args.getToCompress();
        File to = args.getTarget();
        int fileCount = countFilesUnder(toCompress);
        int filesCompressed = 0;
        BufferedOutputStream outStream;
        try {
            outStream = new BufferedOutputStream(new FileOutputStream(to));
        } catch (FileNotFoundException e) {
            log(e);
            return false;
        }
        try (ZipOutputStream zipStream = new ZipOutputStream(new BufferedOutputStream(outStream))) {
            for (FileHolder file : toCompress) {
                filesCompressed = compressCore(getId(), zipStream, file.getFile(),
                        null, filesCompressed, fileCount, to);
            }
        } catch (IOException e) {
            log(e);
            return false;
        }

        return true;
    }

    @Override
    protected boolean operateSaf(CompressArguments args) {
        // TODO SDCARD
        return false;
    }

    @Override
    protected void onStartOperation(CompressArguments args) {
    }

    @Override
    protected void onResult(boolean success, CompressArguments args) {
        File target = args.getTarget();
        if (!success) delete(target);

        MediaScannerUtils.informFileAdded(context, target);
        Notifier.showCompressDoneNotification(success, getId(), target, context);
        FileListFragment.refresh(context, target.getParentFile());
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
     * Recursively compress a File.
     *
     * @return How many files where compressed.
     */
    private int compressCore(int notId, ZipOutputStream zipStream, File toCompress, String internalPath,
                             int filesCompressed, final int fileCount, File zipFile) throws IOException {
        if (internalPath == null) internalPath = "";

        showCompressProgressNotification(filesCompressed, fileCount, notId, zipFile, toCompress, context);
        if (toCompress.isFile()) {
            byte[] buf = new byte[BUFFER_SIZE];
            int len;
            FileInputStream in = new FileInputStream(toCompress);

            // Create internal zip file entry.
            ZipEntry entry;
            if (internalPath.length() > 0) {
                entry = new ZipEntry(internalPath + "/" + toCompress.getName());
            } else {
                entry = new ZipEntry(toCompress.getName());
            }
            entry.setTime(toCompress.lastModified());
            zipStream.putNextEntry(entry);

            // Compress
            while ((len = in.read(buf)) > 0) {
                zipStream.write(buf, 0, len);
            }

            filesCompressed++;
            zipStream.closeEntry();
            in.close();
        } else {
            if (toCompress.list().length == 0) {
                zipStream.putNextEntry(new ZipEntry(internalPath + "/" + toCompress.getName() + "/"));
                zipStream.closeEntry();
            } else {
                for (File child : toCompress.listFiles()) {
                    filesCompressed = compressCore(notId, zipStream, child,
                            internalPath + "/" + toCompress.getName(),
                            filesCompressed, fileCount, zipFile);
                }
            }
        }

        return filesCompressed;
    }
}
