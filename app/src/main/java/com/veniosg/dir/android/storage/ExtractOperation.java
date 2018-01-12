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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.veniosg.dir.android.util.FileUtils.delete;
import static com.veniosg.dir.android.util.Logger.log;
import static com.veniosg.dir.android.util.Notifier.clearNotification;
import static com.veniosg.dir.android.util.Notifier.showExtractProgressNotification;
import static com.veniosg.dir.android.util.Utils.getLastPathSegment;

public class ExtractOperation extends FileOperation<ExtractArguments> {
    private static final int BUFFER_SIZE = 1024;

    private final Context context;

    public ExtractOperation(Context context) {
        super(new StorageAccessManagerCompat(context));
        this.context = context;
    }

    @Override
    protected boolean operate(ExtractArguments args) {
        List<FileHolder> zipHolders = args.getZipFiles();
        File dstDirectory = args.getTarget();
        List<ZipFile> zipFiles;
        try {
            zipFiles = fileHoldersToZipFiles(zipHolders);
        } catch (IOException e) {
            log(e);
            return false;
        }
        int fileCount = countFilesInZip(zipFiles);
        int extractedCount = 0;

        for (ZipFile zipFile : zipFiles) {
            for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();

                showExtractProgressNotification(extractedCount, fileCount,
                        getLastPathSegment(entry.getName()),
                        getLastPathSegment(zipFile.getName()),
                        getId(), context);

                boolean extractSuccessful = extractCore(zipFile, entry, dstDirectory);
                if (!extractSuccessful) return false;
                extractedCount++;
            }
        }

        return true;
    }

    @Override
    protected boolean operateSaf(ExtractArguments args) {
        // TODO SDCARD
        return false;
    }

    @Override
    protected void onStartOperation(ExtractArguments args) {
    }

    @Override
    protected void onResult(boolean success, ExtractArguments args) {
        File to = args.getTarget();
        if (!success) delete(to);

        MediaScannerUtils.informFileAdded(context, to);
        Notifier.showExtractDoneNotification(success, getId(), to, context);
        FileListFragment.refresh(context, to.getParentFile());
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

    private boolean extractCore(ZipFile zipfile, ZipEntry entry, File outputDir) {
        if (entry.isDirectory()) {
            return tryCreateDir(new File(outputDir, entry.getName()));
        }
        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            boolean parentExists = tryCreateDir(outputFile.getParentFile());
            if (!parentExists) return false;
        }

        try (
                BufferedInputStream inputStream = new BufferedInputStream(zipfile.getInputStream(entry));
                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile))
        ) {
            int len;
            byte buf[] = new byte[BUFFER_SIZE];
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            outputFile.setLastModified(entry.getTime());
        } catch (IOException e) {
            log(e);
            return false;
        }

        return true;
    }

    private static List<ZipFile> fileHoldersToZipFiles(List<FileHolder> files) throws IOException {
        List<ZipFile> zips = new ArrayList<ZipFile>(files.size());

        for (FileHolder fh : files) {
            zips.add(new ZipFile(fh.getFile()));
        }

        return zips;
    }

    private int countFilesInZip(List<ZipFile> zipFiles) {
        int count = 0;

        for (ZipFile z : zipFiles) {
            count += z.size();
        }

        return count;
    }

    /**
     * @param dir Directory to create.
     * @return True if the directory now exists, false if it couldn't be created.
     */
    private boolean tryCreateDir(File dir) {
        return dir.exists() || dir.mkdirs();
    }
}
