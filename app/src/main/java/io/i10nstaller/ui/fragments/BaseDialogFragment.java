package io.i10nstaller.ui.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import io.i10nstaller.model.InstallStage;
import io.i10nstaller.viewmodel.InstallViewModel;

public class BaseDialogFragment extends DialogFragment {

    public final InstallStage mDialogData;
    InstallViewModel mViewModel;

    public BaseDialogFragment(InstallStage dialogData) {
        mDialogData = dialogData;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity())
            .get(InstallViewModel.class);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof AlertDialog alertDialog) {
            for (int which : new int[] {
                AlertDialog.BUTTON_POSITIVE,
                AlertDialog.BUTTON_NEGATIVE,
                AlertDialog.BUTTON_NEUTRAL
            }) {
                var button = alertDialog.getButton(which);
                if (button != null) {
                    button.setFocusable(true);
                    button.setClickable(true);
                }
            }
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        cleanAndFinish();
    }

    void cleanAndFinish() {
        mViewModel.cleanupInstall();
        requireActivity().finish();
    }
}
