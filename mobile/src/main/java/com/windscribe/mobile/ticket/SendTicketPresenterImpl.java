/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.ticket;

import android.content.Context;
import android.util.Patterns;

import com.windscribe.mobile.R;
import com.windscribe.vpn.ActivityInteractor;
import com.windscribe.vpn.api.CreateHashMap;
import com.windscribe.vpn.api.response.ApiErrorResponse;
import com.windscribe.vpn.api.response.GenericResponseClass;
import com.windscribe.vpn.api.response.QueryType;
import com.windscribe.vpn.api.response.TicketResponse;
import com.windscribe.vpn.constants.PreferencesKeyConstants;
import com.windscribe.vpn.repository.UserRepository;

import java.util.Map;

import javax.inject.Inject;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class SendTicketPresenterImpl implements SendTicketPresenter {

    private QueryType queryType = QueryType.Account;

    private final ActivityInteractor sendTicketInteractor;

    private final SendTicketView sendTicketView;

    @Inject
    public SendTicketPresenterImpl(SendTicketView sendTicketView, ActivityInteractor activityInteractor) {
        this.sendTicketView = sendTicketView;
        this.sendTicketInteractor = activityInteractor;
    }

    @Override
    public void init() {
        sendTicketView.setActivityTitle(sendTicketInteractor.getResourceString(R.string.send_ticket));
        sendTicketView.setQueryTypeSpinner();
        sendTicketView.addTextChangeListener();
        if(sendTicketInteractor.getUserRepository().getUser().getValue()!=null && sendTicketInteractor.getUserRepository().getUser().getValue().getEmail()!=null){
            sendTicketView.setEmail(sendTicketInteractor.getUserRepository().getUser().getValue().getEmail());
        }
    }

    @Override
    public void onInputChanged(String email, String subject, String message) {
        sendTicketView.setSendButtonState(validEmail(email) && validMessage(message) && validSubject(subject));
    }

    @Override
    public void onQueryTypeSelected(QueryType queryType) {
        this.queryType = queryType;
    }

    @Override
    public void onSendTicketClicked(String email, String subject, String message) {
        sendTicketView.setProgressView(true);
        String username = sendTicketInteractor.getAppPreferenceInterface().getUserName();
        Map<String, String> queryMap = CreateHashMap.INSTANCE.buildTicketMap(email, subject, message, username, queryType);
        sendTicketInteractor.getCompositeDisposable()
                .add(sendTicketInteractor.getApiCallManager().sendTicket(queryMap)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(
                                new DisposableSingleObserver<GenericResponseClass<TicketResponse, ApiErrorResponse>>() {
                                    @Override
                                    public void onError(@NonNull Throwable e) {
                                        sendTicketView.setProgressView(false);
                                        sendTicketView.setErrorLayout("Failed to submit ticket. Try again.");
                                    }

                                    @Override
                                    public void onSuccess(
                                            @NonNull GenericResponseClass<TicketResponse, ApiErrorResponse> response) {
                                        sendTicketView.setProgressView(false);
                                        if (response.getDataClass() != null) {
                                            sendTicketView.setSuccessLayout(
                                                    "Sweet, we’ll get back to you as soon as one of our agents is back from lunch.");
                                        } else if (response.getErrorClass() != null) {
                                            sendTicketView.setErrorLayout(response.getErrorClass().getErrorMessage());
                                        } else {
                                            sendTicketView.setErrorLayout("Failed to submit ticket. Try again.");
                                        }
                                    }
                                }));
    }

    @Override
    public void setTheme(Context context) {
        String savedThem = sendTicketInteractor.getAppPreferenceInterface().getSelectedTheme();
        if (savedThem.equals(PreferencesKeyConstants.DARK_THEME)) {
            context.setTheme(R.style.DarkTheme);
        } else {
            context.setTheme(R.style.LightTheme);
        }
    }

    private boolean validEmail(String email) {
        return !email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean validMessage(String message) {
        return !message.isEmpty();
    }

    private boolean validSubject(String subject) {
        return !subject.isEmpty();
    }
}
