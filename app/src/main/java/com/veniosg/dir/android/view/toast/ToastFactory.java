package com.veniosg.dir.android.view.toast;

import android.content.Context;
import android.widget.Toast;

import com.veniosg.dir.R;

import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class ToastFactory {
    private Context context;

    public ToastFactory(Context context) {
        this.context = context;
    }

    public Toast renameSuccess() {
        return makeText(context, R.string.rename_success, LENGTH_SHORT);
    }

    public Toast renameFailure() {
        return makeText(context, R.string.rename_failure, LENGTH_SHORT);
    }

    public Toast createDirectorySuccess() {
        return makeText(context, R.string.create_dir_success, LENGTH_SHORT);
    }

    public Toast createDirectoryFailure() {
        return makeText(context, R.string.create_dir_failure, LENGTH_SHORT);
    }

    public Toast deleteSuccess() {
        return makeText(context, R.string.delete_success, LENGTH_SHORT);
    }

    public Toast deleteFailure() {
        return makeText(context, R.string.delete_failure, LENGTH_SHORT);
    }
}
