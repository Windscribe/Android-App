/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package androidx.core.app;

public abstract class JobIntentWorkAroundService extends JobIntentService {

    /**
     * A workaround to fix a crash in Services extending from
     * JobIntentService . In future avoid Job intent services completely and use work manager.
     */
    @Override
    JobIntentService.GenericWorkItem dequeueWork() {
        try {
            return super.dequeueWork();
        } catch (SecurityException ignored) {
            return null;
        }
    }
}
