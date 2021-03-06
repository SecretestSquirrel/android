package mobileapp.ctemplar.com.ctemplarapp.login;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import org.jetbrains.annotations.NotNull;

import butterknife.BindView;
import mobileapp.ctemplar.com.ctemplarapp.BaseFragment;
import mobileapp.ctemplar.com.ctemplarapp.BaseFragmentActivity;
import mobileapp.ctemplar.com.ctemplarapp.DialogState;
import mobileapp.ctemplar.com.ctemplarapp.LoginActivityActions;
import mobileapp.ctemplar.com.ctemplarapp.R;
import mobileapp.ctemplar.com.ctemplarapp.main.MainActivity;
import timber.log.Timber;

public class LoginActivity extends BaseFragmentActivity {
    @BindView(R.id.progress_bar)
    public ProgressBar progress;

    @BindView(R.id.progress_background)
    public View progressBackground;

    @BindView(R.id.content_frame)
    FrameLayout mContentFrame;

    private LoginActivityViewModel loginViewModel;

    @NonNull
    @Override
    protected BaseFragment getStartFragment() {
        return new SignInFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        loginViewModel = new ViewModelProvider(this).get(LoginActivityViewModel.class);
        loginViewModel.getActionStatus().observe(this, this::handleActions);
        loginViewModel.getDialogState().observe(this, this::handleDialogState);
    }

    public void blockUI() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    public void unlockUI() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }

    private void goToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void handleDialogState(DialogState state) {
        if (state != null) {
            switch (state) {
                case SHOW_PROGRESS_DIALOG:
                    progress.setVisibility(View.VISIBLE);
                    progressBackground.setVisibility(View.VISIBLE);
                    blockUI();
                    break;
                case HIDE_PROGRESS_DIALOG:
                    progress.setVisibility(View.GONE);
                    progressBackground.setVisibility(View.GONE);
                    unlockUI();
                    break;
            }
        }
    }

    private void handleActions(LoginActivityActions action) {
        if (action != null) {
            switch (action) {
                case CHANGE_FRAGMENT_SIGN_IN:
                    break;
                case CHANGE_FRAGMENT_FORGOT_USERNAME:
                    pushFragment(new ForgotUsernameFragment());
                    break;
                case CHANGE_FRAGMENT_FORGOT_PASSWORD:
                    pushFragment(new ForgotPasswordFragment());
                    break;
                case CHANGE_FRAGMENT_CONFIRM_PASWORD:
                    pushFragment(new ConfirmResetPasswordFragment());
                    break;
                case CHANGE_FRAGMENT_RESET_CODE:
                    pushFragment(new ResetCodeFragment());
                    break;
                case CHANGE_FRAGMENT_NEW_PASSWORD:
                    pushFragment(new NewPasswordFragment());
                    break;
                case CHANGE_FRAGMENT_CREATE_ACCOUNT:
                    pushFragment(new SignUpFragment());
                    break;
                case CHANGE_ACTIVITY_MAIN:
                    goToMainActivity();
                    break;

            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        Timber.d("onSaveInstanceState");
        if (isPortrait2Landscape()) {
            removeFragments();
        }
        super.onSaveInstanceState(outState);
    }

    private void removeFragments() {
        Timber.d("removeFragments");
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(mContentFrame.getId());
        if (fragment == null) {
            Timber.e("fragment is null");
            return;
        }
        supportFragmentManager
                .beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss();
    }

    private boolean isPortrait2Landscape() {
        return isDevicePortrait() && (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
    }

    private boolean isDevicePortrait() {
        return (findViewById(mContentFrame.getId()) != null);
    }
}
