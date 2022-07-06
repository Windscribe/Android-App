/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.worker;


import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.work.ListenableWorker.Result;
import androidx.work.testing.TestListenableWorkerBuilder;
import com.windscribe.vpn.workers.worker.CredentialsWorker;
import com.windscribe.vpn.workers.worker.NotificationWorker;
import com.windscribe.vpn.workers.worker.SessionWorker;
import org.junit.*;
import org.junit.runner.*;

@RunWith(AndroidJUnit4.class)
public class TestWorkers {

    Context mContext;

    @Before
    public void init() {
        mContext = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testConfigWorker() throws Exception {
        CredentialsWorker openVPNConfigAndCredentialsWorker = TestListenableWorkerBuilder.from(mContext, CredentialsWorker.class).build();
        Result result = openVPNConfigAndCredentialsWorker.startWork().get();
        assertThat(result.getOutputData()).isNotNull();
    }

    @Test
    public void testNotificationWorker() throws Exception {
        NotificationWorker notificationWorker = TestListenableWorkerBuilder.from(mContext, NotificationWorker.class)
                .build();
        Result result = notificationWorker.startWork().get();
        assertThat(result).isEqualTo(Result.success());
    }

    @Test
    public void testSessionWorker() throws Exception {
        SessionWorker sessionWorker = TestListenableWorkerBuilder.from(mContext, SessionWorker.class).build();
        Result result = sessionWorker.startWork().get();
        assertThat(result).isEqualTo(Result.success());
    }
}
