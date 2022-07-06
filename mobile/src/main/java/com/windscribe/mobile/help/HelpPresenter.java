/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.mobile.help;

import android.content.Context;

public interface HelpPresenter {

    void init();

    void onDiscordClick();

    void onGarryClick();

    void onKnowledgeBaseClick();

    void onRedditClick();

    void onSendDebugClicked();

    void onSendTicketClick();

    void setTheme(Context context);
}
