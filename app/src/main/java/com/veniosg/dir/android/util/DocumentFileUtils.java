package com.veniosg.dir.android.util;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;

import java.io.File;
import java.io.IOException;

import static android.support.v4.provider.DocumentFile.fromTreeUri;
import static com.veniosg.dir.android.util.FileUtils.delete;
import static com.veniosg.dir.android.util.FileUtils.getExternalStorageRoot;
import static com.veniosg.dir.android.util.Logger.log;

public abstract class DocumentFileUtils {
    /**
     * Delete a file. May be even on external SD card.
     *
     * @param file    the file to be deleted.
     * @return True if successfully deleted.
     */
    public static boolean safAwareDelete(@NonNull Context context, @NonNull final File file) {
        boolean deleteSucceeded = delete(file);

        if (!deleteSucceeded) {
            DocumentFile safFile = findFile(context, file);
            if (safFile != null) deleteSucceeded = safFile.delete();
        }

        return deleteSucceeded && !file.exists();
    }

    @Nullable
    public static DocumentFile findFile(Context context, final File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException(
                    "File must exist. Use createFile() or createDirectory() instead.");
        } else {
            return seekOrCreateTreeDocumentFile(context, file, null, false);
        }
    }

    @Nullable
    public static DocumentFile createFile(Context context, final File file, String mimeType) {
        if (file.exists()) {
            throw new IllegalArgumentException(
                    "File must not exist. Use findFile() instead.");
        } else {
            return seekOrCreateTreeDocumentFile(context, file, mimeType, true);
        }
    }

    @Nullable
    public static DocumentFile createDirectory(Context context, final File directory) {
        if (directory.exists()) {
            throw new IllegalArgumentException("Directory must not exist. Use findFile() instead.");
        } else {
            return seekOrCreateTreeDocumentFile(context, directory, null, true);
        }
    }

    /**
     * Get a DocumentFile corresponding to the given file. If the file doesn't exist, it is created.
     *
     * @param file     The file to get the DocumentFile representation of.
     * @param mimeType Only applies if shouldCreate is true. The mimeType of the file to create.
     *                 Null creates directory.
     * @return The DocumentFile representing the passed file. Null if the file or its path can't
     * be created, or found - depending on shouldCreate's value.
     */
    @Nullable
    private static DocumentFile seekOrCreateTreeDocumentFile(@NonNull Context context,
                                                             @NonNull final File file,
                                                             @Nullable String mimeType,
                                                             boolean shouldCreate) {
        String storageRoot = getExternalStorageRoot(file, context);
        if (storageRoot == null) return null;   // File is not on external storage

        boolean fileIsStorageRoot = false;
        String filePathRelativeToRoot = null;
        try {
            String filePath = file.getCanonicalPath();
            if (!storageRoot.equals(filePath)) {
                filePathRelativeToRoot = filePath.substring(storageRoot.length() + 1);
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
        // TODO SDCARD uncomment:
//        String storageRootUri = storageTreeUriProvider().getTreeUri(storageRoot);
        String storageRootUri = "content://com.android.externalstorage.documents/tree/6338-3934%3A";
        Uri docTreeUri = Uri.parse(storageRootUri);

        // Walk the granted storage tree
        DocumentFile docFile = fromTreeUri(context, docTreeUri);
        if (fileIsStorageRoot) return docFile;

        String[] filePathSegments = filePathRelativeToRoot.split("/");
        for (int i = 0; i < filePathSegments.length; i++) {
            String segment = filePathSegments[i];
            boolean isLastSegment = i == filePathSegments.length - 1;
            DocumentFile nextDocFile = docFile.findFile(segment);

            if (nextDocFile == null && shouldCreate) {
                boolean shouldCreateFile = isLastSegment && mimeType != null;
                nextDocFile = shouldCreateFile ? docFile.createFile(mimeType, segment)
                        : docFile.createDirectory(segment);
            }

            if (nextDocFile == null) {
                // If shouldCreate = true, it means that current segment is not writable
                // Otherwise we couldn't find the file we were looking for
                return null;
            } else {
                docFile = nextDocFile;
            }
        }

        return docFile;
    }
}
