/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.account;

public interface AccountView {


    void goToConfirmEmailActivity();

    void goToEmailActivity();

    void hideProgress();

    void openEditAccountInBrowser(String url);

    void openUpgradeActivity();

    void setActivityTitle(String title);

    void setEmail(String email, int textColor, int labelColor);

    void setEmailConfirm(String emailConfirm, String warningText, int emailColor, int emailLabelColor, int infoIcon, int containerBackground);

    void setEmailConfirmed(String emailConfirm, String warningText, int emailColor, int emailLabelColor, int infoIcon, int containerBackground);

    void setPlanName(String planName);

    void setDataLeft(String dataLeft);

    void setResetDate(String resetDateLabel, String resetDate);

    void setUsername(String username);

    void setWebSessionLoading(boolean show);

    void setupLayoutForFreeUser(String upgradeText);

    void setupLayoutForGhostMode(boolean proUser);

    void setupLayoutForPremiumUser(String upgradeText);

    void showEnterCodeDialog();

    void showErrorDialog(String error);

    void showErrorMessage(String errorMessage);

    void showProgress(String progressText);

    void showSuccessDialog(String message);
}
