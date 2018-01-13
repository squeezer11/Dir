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

import android.support.annotation.NonNull;

import com.veniosg.dir.android.ui.toast.ToastFactory;
import com.veniosg.dir.mvvm.model.storage.access.ExternalStorageAccessManager;
import com.veniosg.dir.mvvm.model.storage.access.StorageAccessManager.AccessPermissionListener;

import java.io.File;

import static java.util.UUID.randomUUID;

/**
 * @param <A> Container type for operation arguments.
 */
public abstract class FileOperation<A extends FileOperation.Arguments> {
    private final ExternalStorageAccessManager storageAccessHelperCompat;
    private final ToastFactory toastFactory;
    private final int id = randomUUID().hashCode();

    protected FileOperation(ExternalStorageAccessManager storageAccessHelperCompat, ToastFactory toastFactory) {
        this.storageAccessHelperCompat = storageAccessHelperCompat;
        this.toastFactory = toastFactory;
    }

    public final void invoke(A args) {
        onStartOperation(args);
        boolean success = operate(args);
        boolean failedButNeedsAccess = !success && needsWriteAccess();
        if (failedButNeedsAccess) {
            if (storageAccessHelperCompat.hasWriteAccess(args.getTarget())) {
                if (storageAccessHelperCompat.isSafBased()) {
                    success = operateSaf(args);
                }
                onResult(success, args);
            } else {
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
                        toastFactory.grantAccessWrongDirectory().show();
                    }
                });
            }
        } else {
            onResult(success, args);
        }
    }

    protected int getId() {
        return id;
    }

    ToastFactory getToastFactory() {
        return toastFactory;
    }

    /**
     * @return Whether the operation was successful.
     */
    protected abstract boolean operate(A args);

    /**
     * Try the operation using SAF facilities
     * Triggered if {@link #operate(Arguments)} returns false and we have write permissions.
     *
     * @return Whether the operation was successful.
     */
    protected abstract boolean operateSaf(A args);

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
