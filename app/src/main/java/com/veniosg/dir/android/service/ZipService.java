/*
 * Copyright (C) 2014 George Venios
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

package com.veniosg.dir.android.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.veniosg.dir.android.fragment.FileListFragment;
import com.veniosg.dir.android.storage.CompressOperation;
import com.veniosg.dir.android.util.Logger;
import com.veniosg.dir.android.util.MediaScannerUtils;
import com.veniosg.dir.android.util.Notifier;
import com.veniosg.dir.android.util.Utils;
import com.veniosg.dir.mvvm.model.FileHolder;

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

import static com.veniosg.dir.android.storage.CompressArguments.compressArgs;
import static com.veniosg.dir.android.util.FileUtils.delete;
import static java.util.Collections.singletonList;

/**
 * @author George Venios
 */
public class ZipService extends IntentService {
    private static final int BUFFER_SIZE = 1024;

    private static final String ACTION_COMPRESS = "com.veniosg.dir.action.COMPRESS";
    private static final String ACTION_EXTRACT = "com.veniosg.dir.action.EXTRACT";
    private static final String EXTRA_FILES = "com.veniosg.dir.action.FILES";

    /**
     * Creates an IntentService. Invoked by your subclass's constructor.
     */
    public ZipService() {
        super(ZipService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        List<FileHolder> files = intent.getParcelableArrayListExtra(EXTRA_FILES);
        File to = new File(intent.getData().getPath());

        if (ACTION_COMPRESS.equals(intent.getAction())) {
            new CompressOperation(this).invoke(compressArgs(to, files));
        } else if (ACTION_EXTRACT.equals(intent.getAction())) {
            try {
                extract(files, to);
            } catch (Exception e) {
                // Cleanup
                delete(to);

                Logger.log(e);
                Notifier.showExtractDoneNotification(false, files.hashCode(), to, this);
            }
        }
    }

    private void extract(List<FileHolder> files, File to) throws IOException {
        List<ZipFile> zipFiles = fileHoldersToZipFiles(files);
        int fileCount = countFilesInZip(zipFiles);
        int extractedCount = 0;

        for (ZipFile zipFile : zipFiles) {
            for (Enumeration e = zipFile.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = (ZipEntry) e.nextElement();

                Notifier.showExtractProgressNotification(extractedCount, fileCount,
                        Utils.getLastPathSegment(entry.getName()),
                        Utils.getLastPathSegment(zipFile.getName()),
                        files.hashCode(), this);

                extractCore(zipFile, entry, to);
                extractedCount++;
            }
        }

        MediaScannerUtils.informFolderAdded(this, to);
        Notifier.showExtractDoneNotification(true, files.hashCode(), to, this);
        FileListFragment.refresh(this, to.getParentFile());
    }

    private void extractCore(ZipFile zipfile, ZipEntry entry,
                             File outputDir) throws IOException {
        if (entry.isDirectory()) {
            createDir(new File(outputDir, entry.getName()));
            return;
        }
        File outputFile = new File(outputDir, entry.getName());
        if (!outputFile.getParentFile().exists()) {
            createDir(outputFile.getParentFile());
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
        }
        outputFile.setLastModified(entry.getTime());
    }

    private int countFilesInZip(List<ZipFile> zipFiles) {
        int count = 0;

        for (ZipFile z : zipFiles) {
            count += z.size();
        }

        return count;
    }

    private void createDir(File dir) {
        if (dir.exists()) {
            return;
        }
        if (!dir.mkdirs()) {
            throw new RuntimeException("Can not create dir " + dir);
        }
    }

    private static List<ZipFile> fileHoldersToZipFiles(List<FileHolder> files) throws IOException {
        List<ZipFile> zips = new ArrayList<ZipFile>(files.size());

        for (FileHolder fh : files) {
            zips.add(new ZipFile(fh.getFile()));
        }

        return zips;
    }

    public static void extractTo(Context c, final FileHolder tbe, File extractTo) {
        extractTo(c, singletonList(tbe), extractTo);
    }

    public static void compressTo(Context c, final FileHolder tbc, File compressTo) {
        compressTo(c, singletonList(tbc), compressTo);
    }

    public static void extractTo(Context c, List<FileHolder> tbe, File extractTo) {
        Intent i = new Intent(ACTION_EXTRACT);
        i.setClassName(c, ZipService.class.getName());
        i.setData(Uri.fromFile(extractTo));
        i.putParcelableArrayListExtra(EXTRA_FILES, tbe instanceof ArrayList
                ? (ArrayList<FileHolder>) tbe
                : new ArrayList<>(tbe));
        c.startService(i);
    }

    public static void compressTo(Context c, List<FileHolder> tbc, File compressTo) {
        Intent i = new Intent(ACTION_COMPRESS);
        i.setClassName(c, ZipService.class.getName());
        i.setData(Uri.fromFile(compressTo));
        i.putParcelableArrayListExtra(EXTRA_FILES, tbc instanceof ArrayList
                ? (ArrayList<FileHolder>) tbc
                : new ArrayList<>(tbc));
        c.startService(i);
    }
}
