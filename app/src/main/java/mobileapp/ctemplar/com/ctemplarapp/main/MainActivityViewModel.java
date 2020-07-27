package mobileapp.ctemplar.com.ctemplarapp.main;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import mobileapp.ctemplar.com.ctemplarapp.CTemplarApp;
import mobileapp.ctemplar.com.ctemplarapp.DialogState;
import mobileapp.ctemplar.com.ctemplarapp.SingleLiveEvent;
import mobileapp.ctemplar.com.ctemplarapp.net.ResponseStatus;
import mobileapp.ctemplar.com.ctemplarapp.net.request.EmptyFolderRequest;
import mobileapp.ctemplar.com.ctemplarapp.net.request.SignInRequest;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Contacts.ContactData;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Contacts.ContactsResponse;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Folders.FoldersResponse;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Mailboxes.MailboxesResponse;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Messages.EmptyFolderResponse;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Messages.MessagesResponse;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Messages.MessagesResult;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Myself.MyselfResponse;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Myself.MyselfResult;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Myself.SettingsEntity;
import mobileapp.ctemplar.com.ctemplarapp.net.response.SignInResponse;
import mobileapp.ctemplar.com.ctemplarapp.repository.ContactsRepository;
import mobileapp.ctemplar.com.ctemplarapp.repository.ManageFoldersRepository;
import mobileapp.ctemplar.com.ctemplarapp.repository.MessagesRepository;
import mobileapp.ctemplar.com.ctemplarapp.repository.UserRepository;
import mobileapp.ctemplar.com.ctemplarapp.repository.constant.MainFolderNames;
import mobileapp.ctemplar.com.ctemplarapp.repository.entity.Contact;
import mobileapp.ctemplar.com.ctemplarapp.repository.entity.ContactEntity;
import mobileapp.ctemplar.com.ctemplarapp.repository.entity.MessageEntity;
import mobileapp.ctemplar.com.ctemplarapp.repository.provider.MessageProvider;
import mobileapp.ctemplar.com.ctemplarapp.utils.EncodeUtils;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;
import timber.log.Timber;

public class MainActivityViewModel extends AndroidViewModel {
    public static final String ANDROID = "android";
    public static final String EXIT_BROADCAST_ACTION = "ctemplar.action.exit";

    private class ExitBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            if (action.equals(EXIT_BROADCAST_ACTION)) {
                exit();
            }
        }

        public void register(Application application) {
            IntentFilter intentFilter = new IntentFilter(EXIT_BROADCAST_ACTION);
            application.registerReceiver(this, intentFilter);
        }

        public void unregister(Application application) {
            application.unregisterReceiver(this);
        }
    }

    private UserRepository userRepository;
    private ContactsRepository contactsRepository;
    private MessagesRepository messagesRepository;
    private ManageFoldersRepository manageFoldersRepository;
    private MutableLiveData<MainActivityActions> actions = new SingleLiveEvent<>();
    private MutableLiveData<DialogState> dialogState = new SingleLiveEvent<>();
    private MutableLiveData<ResponseStatus> responseStatus = new MutableLiveData<>();
    private MutableLiveData<ResponseMessagesData> messagesResponse = new MutableLiveData<>();
    private MutableLiveData<List<Contact>> contactsResponse = new MutableLiveData<>();
    private MutableLiveData<ResponseStatus> toFolderStatus = new MutableLiveData<>();
    private MutableLiveData<ResponseStatus> deleteMessagesStatus = new MutableLiveData<>();
    private MutableLiveData<ResponseStatus> emptyFolderStatus = new MutableLiveData<>();
    private MutableLiveData<FoldersResponse> foldersResponse = new MutableLiveData<>();
    private MutableLiveData<ResponseBody> unreadFoldersBody = new MutableLiveData<>();
    private MutableLiveData<MyselfResponse> myselfResponse = new MutableLiveData<>();
    private MutableLiveData<String> currentFolder = new MutableLiveData<>();

    private ExitBroadcastReceiver exitBroadcastReceiver;

    public MainActivityViewModel(@NonNull Application application) {
        super(application);
        userRepository = CTemplarApp.getUserRepository();
        contactsRepository = CTemplarApp.getContactsRepository();
        messagesRepository = CTemplarApp.getMessagesRepository();
        manageFoldersRepository = CTemplarApp.getManageFoldersRepository();
        exitBroadcastReceiver = new ExitBroadcastReceiver();
        exitBroadcastReceiver.register(application);
    }

    @Override
    protected void onCleared() {
        exitBroadcastReceiver.unregister(getApplication());
    }

    public LiveData<MainActivityActions> getActionsStatus() {
        return actions;
    }

    public void changeAction(MainActivityActions action) {
        actions.postValue(action);
    }

    public LiveData<DialogState> getDialogStatus() {
        return dialogState;
    }

    public void showProgressDialog() {
        dialogState.postValue(DialogState.SHOW_PROGRESS_DIALOG);
    }

    public void hideProgressDialog() {
        dialogState.postValue(DialogState.HIDE_PROGRESS_DIALOG);
    }

    public LiveData<ResponseStatus> getDeleteMessagesStatus() {
        return deleteMessagesStatus;
    }

    public LiveData<ResponseStatus> getEmptyFolderStatus() {
        return emptyFolderStatus;
    }

    public LiveData<ResponseStatus> getResponseStatus() {
        return responseStatus;
    }

    public LiveData<ResponseStatus> getToFolderStatus() {
        return toFolderStatus;
    }

    MutableLiveData<FoldersResponse> getFoldersResponse() {
        return foldersResponse;
    }

    MutableLiveData<ResponseBody> getUnreadFoldersBody() {
        return unreadFoldersBody;
    }

    public void setCurrentFolder(String currentFolder) {
        this.currentFolder.postValue(currentFolder);
    }

    public LiveData<String> getCurrentFolder() {
        return currentFolder;
    }

    public LiveData<ResponseMessagesData> getMessagesResponse() {
        return messagesResponse;
    }

    public LiveData<List<Contact>> getContactsResponse() {
        return contactsResponse;
    }

    public MutableLiveData<MyselfResponse> getMyselfResponse() {
        return myselfResponse;
    }

    public void logout() {
        if (userRepository != null) {
            signOut();
        }
    }

    public void signOut() {
        String token = userRepository.getFirebaseToken();
        userRepository.signOut(ANDROID, token)
                .subscribe(new Observer<Response<Void>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Response<Void> voidResponse) {
                        exit();
                    }

                    @Override
                    public void onError(Throwable e) {
                        exit();
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void exit() {
        userRepository.clearData();
        actions.postValue(MainActivityActions.ACTION_LOGOUT);
    }

    public void checkUserToken() {
        if (TextUtils.isEmpty(userRepository.getUserToken())) {
            exit();
        }
    }

    public void getMessages(int limit, int offset, String folder) {
        if (TextUtils.isEmpty(folder)) {
            return;
        }
        List<MessageEntity> messageEntities;
        switch (folder) {
            case MainFolderNames.STARRED:
                messageEntities = messagesRepository.getStarredMessages();
                break;
            case MainFolderNames.ALL_MAILS:
                messageEntities = messagesRepository.getAllMailsMessages();
                break;
            case MainFolderNames.UNREAD:
                messageEntities = messagesRepository.getUnreadMessages();
                break;
            default:
                messageEntities = messagesRepository.getMessagesByFolder(folder);
                break;
        }
        List<MessageProvider> messageProviders = MessageProvider.fromMessageEntities(messageEntities);

        if (offset == 0) {
            ResponseMessagesData localMessagesData = new ResponseMessagesData(messageProviders,
                    folder, offset);
            if (!localMessagesData.messages.isEmpty()) {
                messagesResponse.postValue(localMessagesData);
            }
        }

        Observable<MessagesResponse> messagesResponseObservable;
        if (MainFolderNames.STARRED.equals(folder)) {
            messagesResponseObservable = userRepository.getStarredMessagesList(limit, offset);
        } else {
            messagesResponseObservable = userRepository.getMessagesList(limit, offset, folder);
        }
        messagesResponseObservable.observeOn(Schedulers.computation())
                .subscribe(new Observer<MessagesResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(MessagesResponse response) {
                        List<MessagesResult> messages = response.getMessagesList();
                        List<MessageEntity> messageEntities
                                = MessageProvider.fromMessagesResultsToEntities(messages, folder);

                        List<MessageProvider> messageProviders;
                        if (offset == 0) {
                            List<MessageEntity> localEntities;
                            switch (folder) {
                                case MainFolderNames.STARRED:
                                    messagesRepository.deleteStarred();
                                    messagesRepository.saveAllMessagesWithIgnore(messageEntities);
                                    localEntities = messagesRepository.getStarredMessages();
                                    break;
                                case MainFolderNames.UNREAD:
                                    messagesRepository.deleteUnread();
                                    messagesRepository.saveAllMessagesWithIgnore(messageEntities);
                                    localEntities = messagesRepository.getUnreadMessages();
                                    break;
                                case MainFolderNames.ALL_MAILS:
                                    messagesRepository.deleteAllMails();
                                    messagesRepository.saveAllMessagesWithIgnore(messageEntities);
                                    localEntities = messagesRepository.getAllMailsMessages();
                                    break;
                                default:
                                    messagesRepository.deleteMessagesByFolderName(folder);
                                    messagesRepository.saveAllMessages(messageEntities);
                                    localEntities = messagesRepository.getMessagesByFolder(folder);
                                    break;
                            }
                            messageProviders = MessageProvider.fromMessageEntities(localEntities);
                        } else {
                            messageProviders = MessageProvider.fromMessageEntities(messageEntities);
                        }

                        messagesResponse.postValue(new ResponseMessagesData(messageProviders,
                                folder, offset));
                        responseStatus.postValue(ResponseStatus.RESPONSE_NEXT_MESSAGES);
                    }

                    @Override
                    public void onError(Throwable e) {
                        responseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void getContacts(int limit, int offset) {
        List<ContactEntity> contactEntities = contactsRepository.getLocalContacts();
        List<Contact> contactList = Contact.fromEntities(contactEntities);
        contactsResponse.postValue(contactList.isEmpty() ? null : contactList);

        contactsRepository.getContactsList(limit, offset)
                .subscribe(new Observer<ContactsResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(final ContactsResponse response) {
                        ContactData[] contacts = response.getResults();
                        ContactData[] decryptedContacts = Contact.decryptContactData(contacts);

                        contactsRepository.saveContacts(decryptedContacts);
                        List<Contact> contactList = Contact.fromResponseResults(decryptedContacts);

                        contactsResponse.postValue(contactList);
                        responseStatus.postValue(ResponseStatus.RESPONSE_NEXT_CONTACTS);
                    }

                    @Override
                    public void onError(Throwable e) {
                        responseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void getMailboxes(int limit, int offset) {
        userRepository.getMailboxesList(limit, offset)
                .subscribe(new Observer<MailboxesResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(MailboxesResponse mailboxesResponse) {
                        if (mailboxesResponse.getTotalCount() > 0) {
                            userRepository.saveMailboxes(mailboxesResponse.getMailboxesList());
                        }
                        responseStatus.postValue(ResponseStatus.RESPONSE_NEXT_MAILBOXES);
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof HttpException) {
                            int code = ((HttpException) e).code();
                            switch (code) {
                                case 401:
                                case 403:
                                    signIn();
                                    break;
                            }
                        }
                        responseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e.getCause());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void signIn() {
        String username = userRepository.getUsername();
        String password = userRepository.getUserPassword();
        SignInRequest signInRequest = new SignInRequest(
                username,
                EncodeUtils.generateHash(username, password)
        );

        userRepository.signIn(signInRequest)
                .subscribe(new Observer<SignInResponse>() {
                    @Override
                    public void onError(Throwable e) {
                        if (e instanceof HttpException) {
                            HttpException exception = (HttpException) e;
                            switch (exception.code()) {
                                case 400:
                                    logout();
                                    break;
                            }
                        }
                    }

                    @Override
                    public void onComplete() {

                    }

                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(SignInResponse signInResponse) {
                        userRepository.saveUserToken(signInResponse.getToken());
                    }
                });
    }

    public void deleteContact(final Contact contact) {
        contactsRepository.deleteLocalContact(contact.getId());
        contactsRepository.deleteContact(contact.getId())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {

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

    public void deleteMessages(Long[] messageIds) {
        String messageIdsString = TextUtils.join(",", messageIds);
        userRepository.deleteMessages(messageIdsString)
                .subscribe(new Observer<Response<Void>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Response<Void> response) {
                        for (long messageId : messageIds) {
                            messagesRepository.deleteMessageById(messageId);
                        }
                        deleteMessagesStatus.postValue(ResponseStatus.RESPONSE_COMPLETE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        deleteMessagesStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void emptyFolder(String folder) {
        userRepository.emptyFolder(new EmptyFolderRequest(folder))
                .subscribe(new Observer<EmptyFolderResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(EmptyFolderResponse emptyFolderResponse) {
                        messagesRepository.deleteMessagesByFolderName(folder);
                        emptyFolderStatus.postValue(ResponseStatus.RESPONSE_COMPLETE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        emptyFolderStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void toFolder(final long messageId, final String folder) {
        userRepository.toFolder(messageId, folder)
                .subscribe(new Observer<Response<Void>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Response<Void> voidResponse) {
                        messagesRepository.updateMessageFolderName(messageId, folder);
                        toFolderStatus.postValue(ResponseStatus.RESPONSE_COMPLETE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        toFolderStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void markMessageIsStarred(final long id, final boolean starred) {
        userRepository.markMessageIsStarred(id, starred)
                .subscribe(new Observer<Response<Void>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Response<Void> messageResponse) {
                        int resultCode = messageResponse.code();
                        if (resultCode == 204) {
                            CTemplarApp.getAppDatabase().messageDao().updateIsStarred(id, starred);
                        }
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

    public void getFolders(int limit, int offset) {
        manageFoldersRepository.getFoldersList(limit, offset)
                .subscribe(new Observer<FoldersResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(FoldersResponse response) {
                        foldersResponse.postValue(response);
                    }

                    @Override
                    public void onError(Throwable e) {
                        responseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e.getCause());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void getUnreadFoldersList() {
        manageFoldersRepository.getUnreadFoldersList()
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        unreadFoldersBody.postValue(responseBody);
                    }

                    @Override
                    public void onError(Throwable e) {
                        responseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e.getCause());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void getUserMyselfInfo() {
        userRepository.getMyselfInfo()
                .subscribe(new Observer<MyselfResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(MyselfResponse myselfResponse) {
                        if (myselfResponse != null) {
                            MyselfResult myselfResult = myselfResponse.getResult()[0];
                            SettingsEntity settingsEntity = myselfResult.getSettings();

                            String timezone = settingsEntity.getTimezone();
                            boolean isContactsEncrypted = settingsEntity.isContactsEncrypted();

                            userRepository.saveTimeZone(timezone);
                            userRepository.setContactsEncryptionEnabled(isContactsEncrypted);
                        }
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
}
