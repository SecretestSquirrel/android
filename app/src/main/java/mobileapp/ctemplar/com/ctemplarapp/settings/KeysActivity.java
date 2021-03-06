package mobileapp.ctemplar.com.ctemplarapp.settings;

import android.os.Bundle;
import android.os.Environment;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import mobileapp.ctemplar.com.ctemplarapp.BaseActivity;
import mobileapp.ctemplar.com.ctemplarapp.R;
import mobileapp.ctemplar.com.ctemplarapp.repository.entity.MailboxEntity;
import mobileapp.ctemplar.com.ctemplarapp.utils.PermissionUtils;
import timber.log.Timber;

public class KeysActivity extends BaseActivity {
    private static final String PUBLIC_KEY_FORMAT = "public.asc";
    private static final String PRIVATE_KEY_FORMAT = "private.asc";
    private static final String SPLITTER = "_";

    @BindView(R.id.activity_setting_keys_address_spinner)
    Spinner mailboxSpinner;

    @BindView(R.id.activity_setting_keys_fingerprint_text_view)
    TextView fingerprintTextView;

    @BindView(R.id.activity_setting_keys_public_key_text_view)
    TextView downloadPublicKeyTextView;

    @BindView(R.id.activity_setting_keys_private_key_text_view)
    TextView downloadPrivateKeyTextView;

    private SettingsViewModel settingsViewModel;
    private List<MailboxEntity> mailboxEntityList;
    private boolean isPrivate;

    private final ActivityResultLauncher<String[]> downloadKeyPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (result.containsValue(false)) {
                    return;
                }
                downloadKey(isPrivate);
            });

    @Override
    protected int getLayoutId() {
        return R.layout.activity_settings_keys;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        mailboxEntityList = settingsViewModel.getAllMailboxes();
        String[] addresses = new String[mailboxEntityList.size()];
        for (int addressIterator = 0; addressIterator < addresses.length; addressIterator++) {
            addresses[addressIterator] = mailboxEntityList.get(addressIterator).getEmail();
        }
        SpinnerAdapter addressAdapter = new ArrayAdapter<>(
                this,
                R.layout.item_domain_spinner,
                addresses
        );
        mailboxSpinner.setAdapter(addressAdapter);
        setListeners();
    }

    private void setListeners() {
        mailboxSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fingerprintTextView.setText(mailboxEntityList.get(position).getFingerprint());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        downloadPublicKeyTextView.setOnClickListener(v -> downloadKey(false));
        downloadPrivateKeyTextView.setOnClickListener(v -> downloadKey(true));

        SpannableString publicKeyTitleSpannable = new SpannableString(getString(R.string.download_public_key));
        SpannableString privateKeyTitleSpannable = new SpannableString(getString(R.string.download_private_key));
        publicKeyTitleSpannable.setSpan(new UnderlineSpan(), 0, publicKeyTitleSpannable.length(), 0);
        privateKeyTitleSpannable.setSpan(new UnderlineSpan(), 0, privateKeyTitleSpannable.length(), 0);
        downloadPublicKeyTextView.setText(publicKeyTitleSpannable);
        downloadPrivateKeyTextView.setText(privateKeyTitleSpannable);
    }

    private void downloadKey(boolean isPrivate) {
        this.isPrivate = isPrivate;
        if (!PermissionUtils.writeExternalStorage(this)) {
            downloadKeyPermissionLauncher.launch(PermissionUtils.externalStoragePermissions());
            return;
        }
        int selectedItemPosition = mailboxSpinner.getSelectedItemPosition();
        MailboxEntity mailboxEntity = mailboxEntityList.get(selectedItemPosition);

        String keyName;
        byte[] keyContent;
        if (isPrivate) {
            keyName = mailboxEntity.getEmail() + SPLITTER + mailboxEntity.getFingerprint()
                    + SPLITTER + PRIVATE_KEY_FORMAT;
            keyContent = mailboxEntity.getPrivateKey().getBytes();
        } else {
            keyName = mailboxEntity.getEmail() + SPLITTER + mailboxEntity.getFingerprint()
                    + SPLITTER + PUBLIC_KEY_FORMAT;
            keyContent = mailboxEntity.getPublicKey().getBytes();
        }

        File externalStorageFile = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File keyFile = new File(externalStorageFile, keyName);
        try {
            BufferedOutputStream bufferedOutputStream =
                    new BufferedOutputStream(new FileOutputStream(keyFile));
            bufferedOutputStream.write(keyContent);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        } catch (IOException e) {
            Timber.e(e);
            return;
        }

        String savedToast = isPrivate ? getString(R.string.your_private_key_saved)
                : getString(R.string.your_public_key_saved);
        Toast.makeText(this, savedToast, Toast.LENGTH_SHORT).show();
    }
}
