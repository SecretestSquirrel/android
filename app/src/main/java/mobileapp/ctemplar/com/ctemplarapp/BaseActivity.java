package mobileapp.ctemplar.com.ctemplarapp;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.app.BaseContextWrapperDelegate;

import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.disposables.CompositeDisposable;
import mobileapp.ctemplar.com.ctemplarapp.utils.DialogUtils;
import mobileapp.ctemplar.com.ctemplarapp.utils.LocaleUtils;
import mobileapp.ctemplar.com.ctemplarapp.utils.ThemeUtils;

public abstract class BaseActivity extends AppCompatActivity {

    protected abstract int getLayoutId();

    private Unbinder mUnbinder;

    protected boolean mRegisterForUserTokenExpiry = true;
    private boolean mExpiredDialogShowing = false;
    private CompositeDisposable mSubscriptions = new CompositeDisposable();
    private AppCompatDelegate baseContextWrappingDelegate;

//    @Override
//    protected void attachBaseContext(Context newBase) {
//        Context context = LocaleUtils.getContextWrapper(newBase);
//        super.attachBaseContext(context);
//    }

    @NonNull
    @Override
    public AppCompatDelegate getDelegate() {
        return baseContextWrappingDelegate != null ?
                baseContextWrappingDelegate :
                (baseContextWrappingDelegate = new BaseContextWrapperDelegate(super.getDelegate()));
    }

    @Override
    public Context createConfigurationContext(Configuration overrideConfiguration) {
        Context context = super.createConfigurationContext(overrideConfiguration);
        return LocaleUtils.getContextWrapper(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeUtils.setTheme(this);
        if (savedInstanceState == null || mUnbinder == null) {
            setContentView(getLayoutId());
            mUnbinder = ButterKnife.bind(this);
        }
    }

    @Override
    protected void onDestroy() {
        mUnbinder.unbind();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mRegisterForUserTokenExpiry) {

//            mSubscriptions.add(UserTokenExpired
//                    .asObservable()
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new Subscriber<String>() {
//                        @Override
//                        public void onCompleted() {
//                            // never happens
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            // never happens
//                        }
//
//                        @Override
//                        public void onNext(String o) {
//                            // ignore the Object
//                            onUserTokenExpired();
//                        }
//                    })
//            );
        }
    }

    @Override
    protected void onPause() {
        mSubscriptions.dispose();
        mSubscriptions.clear();
        super.onPause();
    }

    protected boolean handleBackPress() {
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!handleBackPress()) {
            finish();
        }
    }

    private void onUserTokenExpired() {
        if (!mExpiredDialogShowing) {
            mExpiredDialogShowing = true;
            // Logout and show dialog
            DialogUtils.showAlertDialog(this, R.string.token_expired_title, R.string.token_expired_message, false, dialog -> {
                mExpiredDialogShowing = false;
                // go to LoginActivity for relogin
                // I.startLoginActivity(BaseActivity.this);
                finish();
            });
        }
    }
}
