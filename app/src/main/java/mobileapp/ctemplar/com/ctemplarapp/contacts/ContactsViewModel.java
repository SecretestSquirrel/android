package mobileapp.ctemplar.com.ctemplarapp.contacts;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import mobileapp.ctemplar.com.ctemplarapp.CTemplarApp;
import mobileapp.ctemplar.com.ctemplarapp.net.ResponseStatus;
import mobileapp.ctemplar.com.ctemplarapp.net.response.contacts.ContactData;
import mobileapp.ctemplar.com.ctemplarapp.net.response.contacts.ContactsResponse;
import mobileapp.ctemplar.com.ctemplarapp.net.response.contacts.EncryptContact;
import mobileapp.ctemplar.com.ctemplarapp.repository.ContactsRepository;
import mobileapp.ctemplar.com.ctemplarapp.repository.UserStore;
import mobileapp.ctemplar.com.ctemplarapp.repository.entity.Contact;
import mobileapp.ctemplar.com.ctemplarapp.repository.entity.ContactEntity;
import mobileapp.ctemplar.com.ctemplarapp.utils.EncryptUtils;
import okhttp3.ResponseBody;
import timber.log.Timber;

import static mobileapp.ctemplar.com.ctemplarapp.utils.DateUtils.GENERAL_GSON;

public class ContactsViewModel extends ViewModel {
    private final ContactsRepository contactsRepository;
    private final UserStore userStore;

    private MutableLiveData<ResponseStatus> responseStatus = new MutableLiveData<>();
    private MutableLiveData<List<Contact>> contactsResponse = new MutableLiveData<>();
    private MutableLiveData<Contact> contactResponse = new MutableLiveData<>();

    public ContactsViewModel() {
        contactsRepository = CTemplarApp.getContactsRepository();
        userStore = CTemplarApp.getUserStore();
    }

    public MutableLiveData<ResponseStatus> getResponseStatus() {
        return responseStatus;
    }

    public MutableLiveData<List<Contact>> getContactsResponse() {
        return contactsResponse;
    }

    public MutableLiveData<Contact> getContactResponse() {
        return contactResponse;
    }

    public void getContacts(int limit, int offset) {
        List<ContactEntity> contactLocalEntities = contactsRepository.getLocalContacts();
        Single.fromCallable(() -> Contact.fromEntities(contactLocalEntities))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(new SingleObserver<List<Contact>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull List<Contact> contacts) {
                        contactsResponse.postValue(contacts.isEmpty() ? null : contacts);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Timber.e(e);
                    }
                });

        contactsRepository.getContactsList(limit, offset)
                .subscribe(new Observer<ContactsResponse>() {
                    @Override
                    public void onSubscribe(@NotNull Disposable d) {

                    }

                    @Override
                    public void onNext(final @NotNull ContactsResponse response) {
                        ContactData[] contactData = response.getResults();
                        ContactEntity[] contactEntities = Contact.fromContactDataToEntities(contactData);
                        contactsRepository.saveContacts(contactEntities);

                        List<ContactEntity> localEntities = contactsRepository.getLocalContacts();
                        List<Contact> contactList = Contact.fromEntities(localEntities);

                        contactsResponse.postValue(contactList);
                        responseStatus.postValue(ResponseStatus.RESPONSE_NEXT_CONTACTS);
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        responseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void deleteContact(final Contact contact) {
        contactsRepository.deleteContact(contact.getId())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(@NotNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NotNull ResponseBody responseBody) {
                        contactsRepository.deleteLocalContact(contact.getId());
                        Timber.d("deleteContact");
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void getContact(long id) {
        ContactEntity contactEntity = contactsRepository.getLocalContact(id);
        Single.fromCallable(() -> Contact.fromEntity(contactEntity))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation())
                .subscribe(new SingleObserver<Contact>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onSuccess(@NonNull Contact contact) {
                contactResponse.postValue(contact);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                contactResponse.postValue(null);
                Timber.e(e);
            }
        });
    }

    public void updateContact(ContactData contactData) {
        boolean contactsEncryption = userStore.isContactsEncryptionEnabled();
        if (contactsEncryption) {
            EncryptContact encryptContact = new EncryptContact();
            encryptContact.setEmail(contactData.getEmail());
            encryptContact.setName(contactData.getName());
            encryptContact.setAddress(contactData.getAddress());
            encryptContact.setNote(contactData.getNote());
            encryptContact.setPhone(contactData.getPhone());
            encryptContact.setPhone2(contactData.getPhone2());
            encryptContact.setProvider(contactData.getProvider());

            contactData.setEmail(null);
            contactData.setName(null);
            contactData.setAddress(null);
            contactData.setNote(null);
            contactData.setPhone(null);
            contactData.setPhone2(null);
            contactData.setProvider(null);
            contactData.setEncrypted(true);

            String contactString = GENERAL_GSON.toJson(encryptContact);
            String encryptedContactString = EncryptUtils.encryptData(contactString);
            contactData.setEncryptedData(encryptedContactString);
        }

        contactsRepository.updateContact(contactData)
                .subscribe(new Observer<ContactData>() {
                    @Override
                    public void onSubscribe(@NotNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NotNull ContactData contactData) {
                        ContactEntity contactEntity = Contact.fromContactDataToEntity(contactData);
                        contactsRepository.saveContact(contactEntity);
                        responseStatus.postValue(ResponseStatus.RESPONSE_COMPLETE);
                    }

                    @Override
                    public void onError(@NotNull Throwable e) {
                        responseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }
}
