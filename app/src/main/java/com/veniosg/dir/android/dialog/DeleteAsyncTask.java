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

package com.veniosg.dir.android.dialog;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.veniosg.dir.android.storage.DeleteOperation;
import com.veniosg.dir.mvvm.model.FileHolder;

import static com.veniosg.dir.android.storage.DeleteArguments.deleteArgs;

class DeleteAsyncTask extends AsyncTask<FileHolder, Void, Void> {
    private final Context context;

    DeleteAsyncTask(@NonNull Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected Void doInBackground(FileHolder... params) {
        new DeleteOperation(context)
                .invoke(deleteArgs(params[0].getFile().getParentFile(), params));
        return null;
    }

    @Override
    protected void onPostExecute(Void ignored) {
    }
}
