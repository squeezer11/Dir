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

import com.veniosg.dir.R;
import com.veniosg.dir.android.fragment.FileListFragment;
import com.veniosg.dir.mvvm.model.storage.FileOperation;

import java.io.File;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;
import static com.veniosg.dir.android.util.DocumentFileUtils.getOrCreateTreeDocumentFile;

public class CreateDirectoryOperation extends FileOperation<CreateDirectoryArguments> {
    private Context context;

    public CreateDirectoryOperation(Context context) {
        super(new StorageAccessManagerCompat(context));
        this.context = context;
    }

    @Override
    protected boolean operate(CreateDirectoryArguments args) {
        File dest = args.getTarget();

        return dest.exists() || dest.mkdirs();
    }

    @Override
    protected boolean operateSaf(CreateDirectoryArguments args) {
        File dest = args.getTarget();

        return dest.exists() || getOrCreateTreeDocumentFile(dest, context, true) != null;
    }

    @Override
    protected void onStartOperation(CreateDirectoryArguments args) {
    }

    @Override
    protected void onResult(boolean success, CreateDirectoryArguments args) {
        makeText(context,
                success ? R.string.create_dir_success : R.string.create_dir_failure,
                LENGTH_SHORT
        ).show();

        if (success) FileListFragment.refresh(context, args.getTarget().getParentFile());
    }

    @Override
    protected void onAccessDenied() {
        // TODO SDCARD show some toast
    }

    @Override
    protected void onRequestingAccess() {
    }

    @Override
    protected boolean needsWriteAccess() {
        return true;
    }
}
