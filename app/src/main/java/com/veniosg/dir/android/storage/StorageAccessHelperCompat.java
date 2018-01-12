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
import android.support.annotation.NonNull;

import com.veniosg.dir.mvvm.model.storage.StorageAccessHelper;

import java.io.File;

public class StorageAccessHelperCompat implements StorageAccessHelper {
    private final StorageAccessHelper delegate;

    public StorageAccessHelperCompat(Context context) {
        delegate = new SafStorageAccessHelper(context);
    }

    @Override
    public boolean hasWriteAccess(@NonNull File fileInStorage) {
        return delegate.hasWriteAccess(fileInStorage);
    }

    @Override
    public void requestWriteAccess(@NonNull File fileInStorage, @NonNull AccessPermissionListener listener) {
        delegate.requestWriteAccess(fileInStorage, listener);
    }

    @Override
    public boolean isSafBased() {
        return delegate.isSafBased();
    }
}
