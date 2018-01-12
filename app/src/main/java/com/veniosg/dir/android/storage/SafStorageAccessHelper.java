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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriPermission;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.provider.DocumentFile;

import com.veniosg.dir.android.activity.SafPromptActivity;
import com.veniosg.dir.mvvm.model.storage.StorageAccessHelper;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.support.v4.provider.DocumentFile.fromTreeUri;
import static com.veniosg.dir.IntentConstants.ACTION_STORAGE_ACCESS_RESULT;
import static com.veniosg.dir.IntentConstants.EXTRA_STORAGE_ACCESS_GRANTED;
import static com.veniosg.dir.android.util.FileUtils.getExternalStorageRoot;
import static com.veniosg.dir.android.util.FileUtils.isWritable;
import static com.veniosg.dir.android.util.Logger.log;
import static java.lang.String.format;
import static java.util.Locale.ROOT;

/**
 * Uses the Storage Access Framework to request and persist access permissions to external storage.
 */
class SafStorageAccessHelper implements StorageAccessHelper {
    private final Context context;

    SafStorageAccessHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean hasWriteAccess(@NonNull File fileInStorage) {
        boolean grantedBefore = permissionGrantedForParentOf(fileInStorage);

        return grantedBefore || checkWriteAccess(fileInStorage);
    }

    @Override
    public void requestWriteAccess(@NonNull final File fileInStorage,
                                   @NonNull final AccessPermissionListener listener) {
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // We're assuming broadcast will happen in onDestroy(), and only once, regardless of SAF result
                localBroadcastManager.unregisterReceiver(this);

                if (hasWriteAccess(fileInStorage)) {
                    listener.granted();
                } else {
                    boolean granted = intent.getBooleanExtra(EXTRA_STORAGE_ACCESS_GRANTED, false);

                    if (!granted) {
                        listener.denied();
                    } else {
                        listener.error();
                    }
                }
            }
        }, new IntentFilter(ACTION_STORAGE_ACCESS_RESULT));
        Intent safPromptIntent = new Intent(context, SafPromptActivity.class);
        safPromptIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(safPromptIntent);
    }

    @Override
    public boolean isSafBased() {
        return true;
    }

    private boolean permissionGrantedForParentOf(@NonNull File fileInStorage) {
        List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();

        for (UriPermission permission : permissions) {
            String storageRoot = getExternalStorageRoot(fileInStorage, context);
            DocumentFile grantedDocFile = fromTreeUri(context, permission.getUri());
            boolean grantedOnAncestor = areSameFile(storageRoot, grantedDocFile);
            if (permission.isWritePermission() && grantedOnAncestor) return true;
        }
        return false;
    }

    private boolean checkWriteAccess(File fileInStorage) {
        File fileParent = fileInStorage.getParentFile();
        // Reached root, can't write
        if (fileParent == null) return false;
        // Recur until we find a parent that exists
        if (!fileParent.exists()) return checkWriteAccess(fileParent);

        File tmpFile = generateDummyFileIn(fileParent);

        boolean writable = false;
        if (isWritable(tmpFile)) writable = true;

        DocumentFile document = null;
        if (!writable) {
            // Java said not writable, confirm with SAF
            document = getOrCreateDocumentFile(tmpFile, context);

            if (document != null) {
                // This should have created the file - otherwise something is wrong with access URL.
                writable = document.canWrite() && tmpFile.exists();
            }
        }

        // Cleanup
        safAwareDelete(tmpFile, context, document);
        return writable;
    }

    @NonNull
    private File generateDummyFileIn(File parent) {
        File dummyFile;
        int i = 0;
        do {
            String fileName = format(ROOT, "WriteAccessCheck%d", i++);
            dummyFile = new File(parent, fileName);
        } while (dummyFile.exists());
        return dummyFile;
    }

    /**
     * Delete a file. May be even on external SD card.
     *
     * @param file    the file to be deleted.
     * @param safFile Document to use if normal deletion fails.
     * @return True if successfully deleted.
     */
    @SuppressWarnings("UnusedReturnValue")
    private boolean safAwareDelete(@NonNull final File file, Context context, @Nullable DocumentFile safFile) {
        boolean deleteSucceeded = file.delete();

        if (!deleteSucceeded && safFile != null) {
            deleteSucceeded = safFile.delete();
        }

        return deleteSucceeded && !file.exists();
    }

    private boolean areSameFile(@Nullable String filePath, DocumentFile documentFile) {
        if (filePath == null) return false;
        File file = new File(filePath);
        return file.lastModified() == documentFile.lastModified()
                && file.getName().equals(documentFile.getName());
    }

    /**
     * Get a DocumentFile corresponding to the given file. If the file doesn't exist, it is created.
     *
     * @param file The file to get the DocumentFile representation of.
     * @return The DocumentFile representing the passed file. Null if the file or its path can't be created.
     */
    @Nullable
    public static DocumentFile getOrCreateDocumentFile(final File file, Context context) {
        String storageRoot = getExternalStorageRoot(file, context);
        if (storageRoot == null) return null;   // File is not on external storage

        boolean fileIsStorageRoot = false;
        String pathRelativeToRoot = null;
        try {
            String path = file.getCanonicalPath();
            if (!storageRoot.equals(path)) {
                pathRelativeToRoot = path.substring(storageRoot.length() + 1);
            } else {
                fileIsStorageRoot = true;
            }
        } catch (IOException e) {
            log("Could not get canonical path of File while getting DocumentFile");
            return null;
        } catch (SecurityException e) {
            fileIsStorageRoot = true;
        }

        // TODO SDCARD it looks like we need to persist (or otherwise get all permitted roots here) and walk their subtrees....
        // TODO SDCARD we should try to make sure that the uris we're persisting ARE storage roots, otherwise we should just drop them


//        String uriString = PreferenceManager.getDefaultSharedPreferences(context).getString("URI", null);
//        if (uriString == null) return null;
//        Uri docTreeUri = parse(uriString);
        // TODO SDCARD this should be reading off a list of uris we have access to
        Uri docTreeUri = Uri.parse("content://com.android.externalstorage.documents/tree/6338-3934%3A");

        // Find the file we need down the granted storage tree.
        DocumentFile docFile = fromTreeUri(context, docTreeUri);
        if (fileIsStorageRoot) return docFile;

        String[] segments = pathRelativeToRoot.split("/");
        for (int i = 0; i < segments.length; i++) {

            String segment = segments[i];
            boolean isLastSegment = i == segments.length - 1;
            DocumentFile nextDocFile = docFile.findFile(segment);

            if (nextDocFile == null) {
                if (isLastSegment) {
                    nextDocFile = docFile.createFile("image/png", segment);
                } else {
                    nextDocFile = docFile.createDirectory(segment);
                }
            }

            if (nextDocFile == null) {
                // Segment of file's path not writable
                return null;
            } else {
                docFile = nextDocFile;
            }
        }

        return docFile;
    }
}
