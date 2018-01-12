package com.veniosg.dir.android.util;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.provider.DocumentFile;

import java.io.File;
import java.io.IOException;

import static android.support.v4.provider.DocumentFile.fromTreeUri;
import static com.veniosg.dir.android.util.FileUtils.getExternalStorageRoot;
import static com.veniosg.dir.android.util.Logger.log;

public abstract class DocumentFileUtils {
    public static DocumentFile getOrCreateTreeDocumentFile(final File file, Context context) {
        if (!file.exists()) {
            throw new RuntimeException("File must exist. Call 3-args version.");
        } else {
            return getOrCreateTreeDocumentFile(file, context, file.isDirectory());
        }
    }

    /**
     * Get a DocumentFile corresponding to the given file. If the file doesn't exist, it is created.
     *
     * @param file        The file to get the DocumentFile representation of.
     * @param isDirectory Whether file represents a file or directory.
     * @return The DocumentFile representing the passed file. Null if the file or its path can't be created.
     */
    @Nullable
    public static DocumentFile getOrCreateTreeDocumentFile(final File file, Context context, boolean isDirectory) {
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

        // Find the file we need down the granted storage tree.
        DocumentFile docFile = fromTreeUri(context, docTreeUri);
        if (fileIsStorageRoot) return docFile;

        String[] filePathSegments = filePathRelativeToRoot.split("/");
        for (int i = 0; i < filePathSegments.length; i++) {

            String segment = filePathSegments[i];
            boolean isLastSegment = i == filePathSegments.length - 1;
            DocumentFile nextDocFile = docFile.findFile(segment);

            if (nextDocFile == null) {
                if (isLastSegment && !isDirectory) {
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
