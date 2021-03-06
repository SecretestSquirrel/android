package mobileapp.ctemplar.com.ctemplarapp.mailboxes;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import mobileapp.ctemplar.com.ctemplarapp.CTemplarApp;
import mobileapp.ctemplar.com.ctemplarapp.net.ResponseStatus;
import mobileapp.ctemplar.com.ctemplarapp.net.entity.PGPKeyEntity;
import mobileapp.ctemplar.com.ctemplarapp.net.request.CheckUsernameRequest;
import mobileapp.ctemplar.com.ctemplarapp.net.request.CreateMailboxRequest;
import mobileapp.ctemplar.com.ctemplarapp.net.request.DefaultMailboxRequest;
import mobileapp.ctemplar.com.ctemplarapp.net.request.EnabledMailboxRequest;
import mobileapp.ctemplar.com.ctemplarapp.net.response.CheckUsernameResponse;
import mobileapp.ctemplar.com.ctemplarapp.net.response.domains.DomainsResponse;
import mobileapp.ctemplar.com.ctemplarapp.net.response.mailboxes.MailboxesResult;
import mobileapp.ctemplar.com.ctemplarapp.repository.MailboxDao;
import mobileapp.ctemplar.com.ctemplarapp.repository.UserRepository;
import mobileapp.ctemplar.com.ctemplarapp.repository.entity.MailboxEntity;
import mobileapp.ctemplar.com.ctemplarapp.utils.EncodeUtils;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

public class MailboxesViewModel extends ViewModel {
    private UserRepository userRepository;
    private MailboxDao mailboxDao;
    private MutableLiveData<ResponseStatus> defaultMailboxResponseStatus = new MutableLiveData<>();
    private MutableLiveData<ResponseStatus> enabledMailboxResponseStatus = new MutableLiveData<>();
    private MutableLiveData<DomainsResponse> domainsResponse = new MutableLiveData<>();
    private MutableLiveData<CheckUsernameResponse> checkUsernameResponse = new MutableLiveData<>();
    private MutableLiveData<ResponseStatus> createMailboxResponseStatus = new MutableLiveData<>();
    private MutableLiveData<ResponseStatus> checkUsernameStatus = new MutableLiveData<>();

    public MailboxesViewModel() {
        userRepository = CTemplarApp.getUserRepository();
        mailboxDao = CTemplarApp.getAppDatabase().mailboxDao();
    }

    public List<MailboxEntity> getMailboxes() {
        return mailboxDao.getAll();
    }

    public LiveData<ResponseStatus> defaultMailboxResponseStatus() {
        return defaultMailboxResponseStatus;
    }

    public LiveData<ResponseStatus> enabledMailboxResponseStatus() {
        return enabledMailboxResponseStatus;
    }

    public LiveData<ResponseStatus> createMailboxResponseStatus() {
        return createMailboxResponseStatus;
    }

    public MutableLiveData<CheckUsernameResponse> getCheckUsernameResponse() {
        return checkUsernameResponse;
    }

    public LiveData<ResponseStatus> getCheckUsernameStatus() {
        return checkUsernameStatus;
    }

    MutableLiveData<DomainsResponse> getDomainsResponse() {
        return domainsResponse;
    }

    void updateDefaultMailbox(final long lastSelectedMailboxId, final long mailboxId) {
        userRepository.updateDefaultMailbox(mailboxId, new DefaultMailboxRequest())
                .subscribe(new Observer<MailboxesResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(MailboxesResult mailboxesResult) {
                        defaultMailboxResponseStatus.postValue(ResponseStatus.RESPONSE_COMPLETE);
                        mailboxDao.setDefault(lastSelectedMailboxId, false);
                        mailboxDao.setDefault(mailboxId, true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    void updateEnabledMailbox(final long mailboxId, final boolean isEnabled) {
        userRepository.updateEnabledMailbox(mailboxId, new EnabledMailboxRequest(isEnabled))
                .subscribe(new Observer<MailboxesResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(MailboxesResult mailboxesResult) {
                        enabledMailboxResponseStatus.postValue(ResponseStatus.RESPONSE_COMPLETE);
                        mailboxDao.setEnabled(mailboxId, isEnabled);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    void getDomains() {
        userRepository.getDomains()
                .subscribe(new Observer<DomainsResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(DomainsResponse response) {
                        domainsResponse.postValue(response);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    void checkUsername(String username) {
        userRepository.checkUsername(new CheckUsernameRequest(username))
                .subscribe(new Observer<CheckUsernameResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(CheckUsernameResponse response) {
                        checkUsernameResponse.postValue(response);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof HttpException) {
                            HttpException exception = (HttpException) e;
                            if (exception.code() == 429) {
                                checkUsernameStatus.postValue(ResponseStatus.RESPONSE_ERROR_TOO_MANY_REQUESTS);
                            } else {
                                checkUsernameStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                            }
                        } else {
                            checkUsernameStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        }
                        Timber.w(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    void createMailbox(final String mailboxEmail) {
        final String userPassword = userRepository.getUserPassword();
        final CreateMailboxRequest createMailboxRequest = new CreateMailboxRequest();
        createMailboxRequest.setEmail(mailboxEmail);

        EncodeUtils.generateAdditionalMailbox(mailboxEmail, userPassword)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap((Function<PGPKeyEntity, Observable<Response<MailboxesResult>>>) pgpKeyEntity -> {
                    createMailboxRequest.setPrivateKey(pgpKeyEntity.getPrivateKey());
                    createMailboxRequest.setPublicKey(pgpKeyEntity.getPublicKey());
                    createMailboxRequest.setFingerprint(pgpKeyEntity.getFingerprint());
                    return userRepository.createMailbox(createMailboxRequest);
                }).subscribe(new Observer<Response<MailboxesResult>>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(Response<MailboxesResult> mailboxesResultResponse) {
                if (mailboxesResultResponse.code() == 201) {
                    createMailboxResponseStatus.postValue(ResponseStatus.RESPONSE_COMPLETE);
                    final MailboxesResult mailboxResponse = mailboxesResultResponse.body();
                    List<MailboxesResult> mailboxEntityList = Collections.singletonList(mailboxResponse);
                    userRepository.saveMailboxes(mailboxEntityList);
                } else if (mailboxesResultResponse.code() == 400) {
                    createMailboxResponseStatus.postValue(ResponseStatus.RESPONSE_ERROR_PAID);
                } else {
                    createMailboxResponseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                }
            }

            @Override
            public void onError(Throwable e) {
                createMailboxResponseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                Timber.e(e);
            }

            @Override
            public void onComplete() {

            }
        });
    }
}
