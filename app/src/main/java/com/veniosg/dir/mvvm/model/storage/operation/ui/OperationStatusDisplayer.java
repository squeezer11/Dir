package com.veniosg.dir.mvvm.model.storage.operation.ui;

import java.io.File;

public interface OperationStatusDisplayer {
    void showCopySuccess(int operationId, File destDir);
    void showCopyFailure(int operationId, File destDir);
    void showCopyProgress(int operationId, File destDir, File copying, int progress, int max);
}
