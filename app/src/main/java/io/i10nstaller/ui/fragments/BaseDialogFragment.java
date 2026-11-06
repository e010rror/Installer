package io.i10nstaller.ui.fragments;

import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import io.i10nstaller.model.InstallStage;
import io.i10nstaller.viewmodel.InstallViewModel;

public class BaseDialogFragment extends DialogFragment {

    public final InstallStage mDialogData;
    InstallViewModel mViewModel;
    private ViewTreeObserver mDialogViewTreeObserver;
    private ViewTreeObserver.OnWindowFocusChangeListener mWindowFocusChangeListener;
    private ViewTreeObserver.OnTouchModeChangeListener mTouchModeChangeListener;

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
            boolean isTvDevice = isTvDevice();
            for (int which : new int[] {
                AlertDialog.BUTTON_POSITIVE,
                AlertDialog.BUTTON_NEGATIVE,
                AlertDialog.BUTTON_NEUTRAL
            }) {
                var button = alertDialog.getButton(which);
                if (button != null) {
                    button.setFocusable(true);
                    button.setFocusableInTouchMode(
                        isTvDevice && which == AlertDialog.BUTTON_POSITIVE);
                    button.setClickable(true);
                }
            }
            if (shouldHandleConfirmationDpadKeys()) {
                configureConfirmationDialog(alertDialog);
            }
        }
    }

    private void configureConfirmationDialog(AlertDialog dialog) {
        var positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        var negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        var neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (positiveButton == null) {
            return;
        }

        positiveButton.setFocusable(true);
        positiveButton.setFocusableInTouchMode(true);
        positiveButton.post(positiveButton::requestFocus);

        if (negativeButton != null) {
            negativeButton.setFocusableInTouchMode(true);
        }
        if (neutralButton != null) {
            neutralButton.setFocusableInTouchMode(true);
        }

        var decorView = dialog.getWindow().getDecorView();
        mDialogViewTreeObserver = decorView.getViewTreeObserver();
        mWindowFocusChangeListener = hasFocus -> {
            if (hasFocus) {
                focusPositiveUnlessOtherButtonFocused(
                    positiveButton, negativeButton, neutralButton);
            }
        };
        mTouchModeChangeListener = inTouchMode -> {
            if (!inTouchMode) {
                focusPositiveUnlessOtherButtonFocused(
                    positiveButton, negativeButton, neutralButton);
            }
        };
        mDialogViewTreeObserver.addOnWindowFocusChangeListener(mWindowFocusChangeListener);
        mDialogViewTreeObserver.addOnTouchModeChangeListener(mTouchModeChangeListener);

        dialog.setOnKeyListener((ignored, keyCode, event) ->
            handleConfirmationKey(
                positiveButton, negativeButton, neutralButton, keyCode, event));
    }

    private void focusPositiveUnlessOtherButtonFocused(
        View positiveButton, View negativeButton, View neutralButton) {
        if (!hasFocus(negativeButton) && !hasFocus(neutralButton)) {
            positiveButton.requestFocus();
        }
    }

    private boolean hasFocus(View button) {
        return button != null && button.hasFocus();
    }

    private boolean handleConfirmationKey(
        View positiveButton, View negativeButton, View neutralButton,
        int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER
            || keyCode == KeyEvent.KEYCODE_ENTER
            || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
            if (hasFocus(negativeButton) || hasFocus(neutralButton)) {
                return false;
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                positiveButton.setPressed(true);
                return true;
            }
            if (event.getAction() == KeyEvent.ACTION_UP) {
                positiveButton.setPressed(false);
                if (!event.isCanceled() && positiveButton.isEnabled()) {
                    positiveButton.performClick();
                }
                return true;
            }
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT
            && event.getAction() == KeyEvent.ACTION_DOWN
            && negativeButton != null
            && negativeButton.isEnabled()
            && !negativeButton.hasFocus()) {
            negativeButton.requestFocus();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
            && event.getAction() == KeyEvent.ACTION_DOWN
            && positiveButton.isEnabled()
            && !positiveButton.hasFocus()) {
            positiveButton.requestFocus();
            return true;
        }
        return false;
    }

    private boolean isTvDevice() {
        var context = requireContext();
        var uiModeManager =
            (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return (uiModeManager != null
                && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION)
            || !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    protected boolean shouldHandleConfirmationDpadKeys() {
        return false;
    }

    protected void focusPositiveButtonForDpad() {
        if (getDialog() instanceof AlertDialog alertDialog) {
            var positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                boolean isTvDevice = isTvDevice();
                positiveButton.post(() -> {
                    if (isTvDevice || !positiveButton.isInTouchMode()) {
                        positiveButton.requestFocus();
                    }
                });
            }
        }
    }

    @Override
    public void onStop() {
        removeDialogFocusListeners();
        super.onStop();
    }

    private void removeDialogFocusListeners() {
        if (mDialogViewTreeObserver != null && mDialogViewTreeObserver.isAlive()) {
            if (mWindowFocusChangeListener != null) {
                mDialogViewTreeObserver.removeOnWindowFocusChangeListener(
                    mWindowFocusChangeListener);
            }
            if (mTouchModeChangeListener != null) {
                mDialogViewTreeObserver.removeOnTouchModeChangeListener(
                    mTouchModeChangeListener);
            }
        }
        mDialogViewTreeObserver = null;
        mWindowFocusChangeListener = null;
        mTouchModeChangeListener = null;
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
