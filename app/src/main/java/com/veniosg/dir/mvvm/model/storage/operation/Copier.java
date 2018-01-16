package com.veniosg.dir.mvvm.model.storage.operation;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;

import com.veniosg.dir.android.util.MediaScannerUtils;
import com.veniosg.dir.mvvm.model.FileHolder;
import com.veniosg.dir.mvvm.model.storage.operation.argument.CopyArguments;
import com.veniosg.dir.mvvm.model.storage.operation.ui.OperationStatusDisplayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static com.veniosg.dir.android.util.FileUtils.countFilesUnder;
import static com.veniosg.dir.android.util.FileUtils.createUniqueCopyName;
import static com.veniosg.dir.android.util.Logger.log;
import static com.veniosg.dir.mvvm.model.storage.DocumentFileUtils.createDirectory;
import static com.veniosg.dir.mvvm.model.storage.DocumentFileUtils.createFile;
import static com.veniosg.dir.mvvm.model.storage.DocumentFileUtils.outputStreamFor;

/**
 * While worker classes like this are generally only required as a means to keep common logic
 * between normal and saf versions of operations centralized and should therefore reside within the
 * operation, this had to move out because of the inability of SAF to move files.
 */
abstract class Copier {
    private static final int COPY_BUFFER_SIZE = 32 * 1024;

    @NonNull
    private final Context context;
    @NonNull
    private final OperationStatusDisplayer statusDisplayer;
    private final int operationId;

    Copier(@NonNull Context context,
           @NonNull OperationStatusDisplayer statusDisplayer,
           int operationId) {
        this.context = context;
        this.statusDisplayer = statusDisplayer;
        this.operationId = operationId;
    }

    boolean copy(CopyArguments args) {
        List<FileHolder> files = args.getFilesToCopy();
        File destDirectory = args.getTarget();

        int fileCount = countFilesUnder(files);
        int filesCopied = 0;

        for (FileHolder origin : files) {
            File dest = createUniqueCopyName(context, destDirectory, origin.getName());
            if (dest != null) {
                filesCopied = copyFileOrDirectory(
                        filesCopied, fileCount, origin.getFile(), dest);

                if (origin.getFile().isDirectory()) {
                    MediaScannerUtils.informFolderAdded(context, dest);
                } else {
                    MediaScannerUtils.informFileAdded(context, dest);
                }
            }
        }

        return filesCopied == fileCount;
    }

    /**
     * Recursively copy a folder.
     *
     * @param filesCopied Initial value of how many files have been copied.
     * @param oldFile     Folder to copy.
     * @param newFile     The dir to be created.
     * @return The new filesCopied count.
     */
    private int copyFileOrDirectory(int filesCopied, int fileCount, @NonNull File oldFile, @NonNull File newFile) {
        if (oldFile.isDirectory()) {
            filesCopied = copyDirectory(filesCopied, fileCount, oldFile, newFile);
        } else {
            filesCopied = copyFile(filesCopied, fileCount, oldFile, newFile);
        }

        return filesCopied;
    }

    /**
     * Copy a file.
     *
     * @param filesCopied Initial value of how many files have been copied.
     * @param oldFile     File to copy.
     * @param newFile     The file to be created.
     * @return The new filesCopied count.
     */
    private int copyFile(int filesCopied, int fileCount, File oldFile, File newFile) {
        statusDisplayer.showCopyProgress(operationId, newFile.getParentFile(), oldFile,
                filesCopied, fileCount);

        try (
                FileInputStream input = new FileInputStream(oldFile);
                OutputStream output = outputStream(newFile)
        ) {
            int len;
            byte[] buffer = new byte[COPY_BUFFER_SIZE];
            while ((len = input.read(buffer)) > 0) {
                output.write(buffer, 0, len);
            }
        } catch (IOException e) {
            log(e);
            return filesCopied;
        }
        return filesCopied + 1;
    }

    private int copyDirectory(int filesCopied, int fileCount, @NonNull File oldFile,
                              @NonNull File newFile) {
        if (!newFile.exists()) mkDir(newFile);

        // list all the directory contents
        String files[] = oldFile.list();

        for (String file : files) {
            // construct the src and dest file structure
            File srcFile = new File(oldFile, file);
            File destFile = new File(newFile, file);
            // recursive copy
            filesCopied = copyFileOrDirectory(filesCopied, fileCount, srcFile, destFile);
        }
        return filesCopied;
    }

    @NonNull
    protected abstract OutputStream outputStream(File newFile) throws FileNotFoundException;

    @SuppressWarnings("UnusedReturnValue")
    protected abstract boolean mkDir(@NonNull File newFile);

    public static class FileCopier extends Copier {
        public FileCopier(@NonNull Context context,
                          @NonNull OperationStatusDisplayer statusDisplayer,
                          int operationId) {
            super(context, statusDisplayer, operationId);
        }

        @Override
        @NonNull
        protected OutputStream outputStream(File newFile) throws FileNotFoundException {
            return new FileOutputStream(newFile);
        }

        @Override
        protected boolean mkDir(@NonNull File newFile) {
            return newFile.mkdir();
        }
    }

    public static class DocumentFileCopier extends Copier {
        private final Context context;

        public DocumentFileCopier(@NonNull Context context,
                                  @NonNull OperationStatusDisplayer statusDisplayer,
                                  int operationId) {
            super(context, statusDisplayer, operationId);
            this.context = context;
        }

        @NonNull
        @Override
        protected OutputStream outputStream(File newFile) throws FileNotFoundException {
            DocumentFile toSaf = createFile(context, newFile, "*/*");
            return outputStreamFor(toSaf, context);
        }

        @Override
        protected boolean mkDir(@NonNull File newDir) {
            return createDirectory(context, newDir) != null;
        }
    }
}
