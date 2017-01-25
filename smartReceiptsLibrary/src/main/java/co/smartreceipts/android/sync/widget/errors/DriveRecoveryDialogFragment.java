package co.smartreceipts.android.sync.widget.errors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;

import com.google.common.base.Preconditions;

import co.smartreceipts.android.R;
import co.smartreceipts.android.SmartReceiptsApplication;
import co.smartreceipts.android.activities.NavigationHandler;
import co.smartreceipts.android.sync.BackupProvidersManager;
import co.smartreceipts.android.sync.model.RemoteBackupMetadata;
import co.smartreceipts.android.sync.widget.backups.DeleteRemoteBackupDialogFragment;
import co.smartreceipts.android.sync.widget.backups.DeleteRemoteBackupProgressDialogFragment;

public class DriveRecoveryDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private BackupProvidersManager mBackupProvidersManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBackupProvidersManager = ((SmartReceiptsApplication)getActivity().getApplication()).getBackupProvidersManager();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Test");
        builder.setMessage("Test");
        builder.setCancelable(true);
        builder.setNegativeButton(android.R.string.cancel, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        dismiss();
    }
}