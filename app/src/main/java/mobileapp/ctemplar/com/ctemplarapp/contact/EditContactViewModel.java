package mobileapp.ctemplar.com.ctemplarapp.contact;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import mobileapp.ctemplar.com.ctemplarapp.CTemplarApp;
import mobileapp.ctemplar.com.ctemplarapp.net.ResponseStatus;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Contacts.ContactData;
import mobileapp.ctemplar.com.ctemplarapp.net.response.Contacts.ContactsResponse;
import mobileapp.ctemplar.com.ctemplarapp.repository.ContactsRepository;
import mobileapp.ctemplar.com.ctemplarapp.repository.entity.ContactEntity;
import timber.log.Timber;

public class EditContactViewModel extends ViewModel {
    private ContactsRepository contactsRepository;
    private MutableLiveData<ResponseStatus> responseStatus = new MutableLiveData<>();
    private MutableLiveData<Contact> contactResponse = new MutableLiveData<>();

    public EditContactViewModel() {
        this.contactsRepository = CTemplarApp.getContactsRepository();;
    }

    public void getContact(long id) {
        ContactEntity contactEntity = contactsRepository.getLocalContact(id);
        if (contactEntity != null) {
            contactResponse.postValue(Contact.fromEntity(contactEntity));
        } else {

            contactsRepository.getContact(id)
                    .subscribe(new Observer<ContactsResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(ContactsResponse contactsResponse) {
                            ContactData[] contacts = contactsResponse.getResults();

                            if (contacts == null || contacts.length == 0) {
                                responseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                            } else {
                                contactResponse.postValue(Contact.fromResponseResult(contacts[0]));
                                responseStatus.postValue(ResponseStatus.RESPONSE_NEXT);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            responseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                            Timber.e(e, "Updating contact error");
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    public void updateContact(ContactData contactData) {
        contactsRepository.updateContact(contactData)
                .subscribe(new Observer<ContactData>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ContactData contactData) {
                        contactsRepository.saveLocalContact(contactData);
                        responseStatus.postValue(ResponseStatus.RESPONSE_COMPLETE);
                    }

                    @Override
                    public void onError(Throwable e) {
                        responseStatus.postValue(ResponseStatus.RESPONSE_ERROR);
                        Timber.e(e, "Saving contact error");
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public MutableLiveData<ResponseStatus> getResponseStatus() {
        return responseStatus;
    }

    public MutableLiveData<Contact> getContactResponse() {
        return contactResponse;
    }
}