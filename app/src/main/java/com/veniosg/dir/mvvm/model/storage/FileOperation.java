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

package com.veniosg.dir.mvvm.model.storage;

import android.support.annotation.NonNull;

import com.veniosg.dir.android.storage.StorageAccessHelperCompat;
import com.veniosg.dir.mvvm.model.storage.StorageAccessHelper.AccessPermissionListener;

import java.io.File;

import static java.util.UUID.randomUUID;

/**
 * @param <A> Container type for operation arguments.
 */
public abstract class FileOperation<A extends FileOperation.Arguments> {
    private StorageAccessHelperCompat storageAccessHelperCompat;
    private int id = randomUUID().hashCode();

    protected FileOperation(StorageAccessHelperCompat storageAccessHelperCompat) {
        this.storageAccessHelperCompat = storageAccessHelperCompat;
    }

    public final void invoke(A args) {
        onStartOperation(args);
        boolean normalSucceeded = doOperation(args);
        boolean failedBecauseOfNoAccess = !normalSucceeded &&
                needsWriteAccess() &&
                !storageAccessHelperCompat.hasWriteAccess(args.getTarget());
        if (failedBecauseOfNoAccess) {
            onRequestingAccess();
            storageAccessHelperCompat.requestWriteAccess(args.getTarget(), new AccessPermissionListener() {
                @Override
                public void granted() {
                    invoke(args);
                }

                @Override
                public void denied() {
                    onAccessDenied();
                }

                @Override
                public void error() {
                    invoke(args);
                }
            });
        } else {
            onResult(normalSucceeded, args);
        }
    }

    protected int getId() {
        return id;
    }

    /**
     * @return Whether the operation was successful.
     */
    protected abstract boolean doOperation(A args);

    /**
     * Good place to show initial UI, or prepare any dialogs etc.
     * Called right before running the operation. Can be called multiple times.
     *
     * @param args Original arguments for the invocation that is getting started.
     */
    protected abstract void onStartOperation(A args);

    /**
     * Good place to show final result (success/failure) UI.
     * No other callbacks will happen after this.
     *
     * @param success Whether the invocation was successful.
     * @param args    Original arguments for the invocation that just finished.
     */
    protected abstract void onResult(boolean success, A args);

    /**
     * Called if the user has denied storage write permissions on the parent volume of {@link Arguments#target}.
     * No other callbacks will happen after this.
     */
    protected abstract void onAccessDenied();

    /**
     * Good place to hide any progress UI. You may still get calls to onResult()/onAccessDenied() after this.
     */
    protected abstract void onRequestingAccess();

    /**
     * @return Whether this type of operation can fail because of lack of storage write permissions.
     */
    protected abstract boolean needsWriteAccess();

    public static abstract class Arguments {
        @NonNull
        private final File target;

        protected Arguments(@NonNull File target) {
            this.target = target;
        }

        @NonNull
        public File getTarget() {
            return target;
        }
    }
}
