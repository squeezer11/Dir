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
import com.veniosg.dir.android.view.toast.ToastFactory;
import com.veniosg.dir.mvvm.model.storage.FileOperation;

import java.io.File;

import static com.veniosg.dir.android.util.DocumentFileUtils.createDirectory;

public class CreateDirectoryOperation extends FileOperation<CreateDirectoryArguments> {
    private final Context context;
    private final ToastFactory toastFactory;

    public CreateDirectoryOperation(Context context) {
        super(new StorageAccessManagerCompat(context));
        this.context = context;
        this.toastFactory = new ToastFactory(context);
    }

    @Override
    protected boolean operate(CreateDirectoryArguments args) {
        File dest = args.getTarget();

        return dest.exists() || dest.mkdirs();
    }

    @Override
    protected boolean operateSaf(CreateDirectoryArguments args) {
        File dest = args.getTarget();

        return dest.exists() || createDirectory(context, dest) != null;
    }

    @Override
    protected void onStartOperation(CreateDirectoryArguments args) {
    }

    @Override
    protected void onResult(boolean success, CreateDirectoryArguments args) {
        if (success) {
            toastFactory.createDirectorySuccess().show();
            FileListFragment.refresh(context, args.getTarget().getParentFile());
        } else {
            toastFactory.createDirectoryFailure().show();
        }
    }

    @Override
    protected void onAccessDenied() {
    }

    @Override
    protected void onRequestingAccess() {
    }

    @Override
    protected boolean needsWriteAccess() {
        return true;
    }
}
