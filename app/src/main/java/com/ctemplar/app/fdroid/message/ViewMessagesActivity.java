package com.ctemplar.app.fdroid.message;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;

import com.ctemplar.app.fdroid.BaseActivity;
import com.ctemplar.app.fdroid.R;
import com.ctemplar.app.fdroid.main.MainActivity;

import static com.ctemplar.app.fdroid.services.NotificationService.FROM_NOTIFICATION;

public class ViewMessagesActivity extends BaseActivity {
    public static final String PARENT_ID = "parent_id";
    private ViewMessagesFragment fragment;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_view_messages;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragment = ViewMessagesFragment.newInstance(getIntent().getExtras());

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.activity_view_messages_content_frame, fragment)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (!fragment.onBackPressed()) {
            super.onBackPressed();
            Intent intent = getIntent();
            boolean fromNotification = intent != null && intent.getBooleanExtra(FROM_NOTIFICATION,
                    false);
            if (fromNotification) {
                startActivity(new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                );
            }
        }
    }
}
