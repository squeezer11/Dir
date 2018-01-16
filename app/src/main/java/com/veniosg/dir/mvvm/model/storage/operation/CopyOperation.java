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

package com.veniosg.dir.mvvm.model.storage.operation;

import android.content.Context;

import com.veniosg.dir.android.ui.toast.ToastFactory;
import com.veniosg.dir.mvvm.model.storage.access.ExternalStorageAccessManager;
import com.veniosg.dir.mvvm.model.storage.operation.argument.CopyArguments;
import com.veniosg.dir.mvvm.model.storage.operation.ui.OperationStatusDisplayer;

import static com.veniosg.dir.android.fragment.FileListFragment.refresh;
import static com.veniosg.dir.android.util.Notifier.clearNotification;

public class CopyOperation extends FileOperation<CopyArguments> {
    private final Context context;
    private final OperationStatusDisplayer statusDisplayer;

    public CopyOperation(Context context, OperationStatusDisplayer statusDisplayer) {
        super(new ExternalStorageAccessManager(context), new ToastFactory(context));
        this.context = context.getApplicationContext();
        this.statusDisplayer = statusDisplayer;
    }

    @Override
    protected boolean operate(CopyArguments args) {
        return new Copier.FileCopier(context, statusDisplayer, getId()).copy(args);
    }

    @Override
    protected boolean operateSaf(CopyArguments args) {
        return new Copier.DocumentFileCopier(context, statusDisplayer, getId()).copy(args);
    }

    @Override
    protected void onStartOperation(CopyArguments args) {
    }

    @Override
    protected void onResult(boolean success, CopyArguments args) {
        if (success) {
            statusDisplayer.showCopySuccess(getId(), args.getTarget());
            refresh(context, args.getTarget());
        } else {
            statusDisplayer.showCopyFailure(getId(), args.getTarget());
        }
    }

    @Override
    protected void onAccessDenied() {
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
