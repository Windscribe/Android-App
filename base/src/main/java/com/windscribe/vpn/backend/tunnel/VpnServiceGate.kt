/*
 * Copyright (c) 2026 Windscribe Limited.
 */

package com.windscribe.vpn.backend.tunnel

import android.content.Intent
import androidx.core.content.ContextCompat
import com.windscribe.vpn.Windscribe.Companion.appContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the running [WindscribeVpnService] so callers can wait for one.
 *
 * Starting a `Service` is asynchronous and only the service can `establish()`, so a connection has
 * to suspend until it registers.
 */
@Singleton
class VpnServiceGate
    @Inject
    constructor() {
        private val logger = LoggerFactory.getLogger("vpn")

        /** One value, so no reader can pair a service with a flag that never went with it. */
        private data class Attachment(
            val service: WindscribeVpnService,
            val stopping: Boolean,
        )

        private val attachment = MutableStateFlow<Attachment?>(null)

        /**
         * The attached service, or null when none is running. Never blocks. Still returns one that is
         * leaving: teardown needs it. Use [ensureRunning] to start a session on.
         */
        val current: WindscribeVpnService?
            get() = attachment.value?.service

        /**
         * True from the moment the service decides to stop until it is started or replaced.
         *
         * `current` alone is not enough: Android tears the service down milliseconds after
         * `stopSelf()`, so anything reacting in between sees one attached but already leaving. Set
         * before that state is published, so it cannot be raced.
         */
        val isStopping: Boolean
            get() = attachment.value?.stopping == true

        /** Safe to start a session on. */
        private val live: WindscribeVpnService?
            get() = attachment.value?.takeUnless { it.stopping }?.service

        fun markStopping() {
            attachment.value = attachment.value?.copy(stopping = true)
        }

        /**
         * Registers [instance] as live. Called from `onCreate` and from every `onStartCommand`: a
         * start arriving before a pending `stopSelf()` revives the same instance, without `onCreate`.
         */
        fun attach(instance: WindscribeVpnService) {
            attachment.value = Attachment(instance, stopping = false)
        }

        /**
         * Clears [instance], if it is still the registered one.
         *
         * Identity-checked because a replacement can attach before the outgoing service's `onDestroy`
         * runs. An unconditional clear would then orphan the live service, and everything that
         * reaches it through [current] would silently do nothing.
         */
        fun detach(instance: WindscribeVpnService) {
            attachment.value = attachment.value?.takeUnless { it.service === instance }
        }

        /**
         * Returns a service safe to start a session on, starting one and waiting if there is none.
         * A stopping instance is never handed back; starting revives it or gets a replacement.
         */
        suspend fun ensureRunning(): WindscribeVpnService? {
            live?.let { return it }
            logger.info("Starting the Windscribe VPN service.")
            val context = appContext.applicationContext
            return try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, WindscribeVpnService::class.java)
                        .setAction(WindscribeVpnService.ACTION_CONNECT),
                )
                await()
            } catch (e: Exception) {
                logger.error("Failed to start the VPN service.", e)
                null
            }
        }

        /** Suspends until a service that is not leaving is up, or returns null if none arrives. */
        suspend fun await(timeoutMs: Long = AWAIT_TIMEOUT_MS): WindscribeVpnService? =
            live ?: withTimeoutOrNull(timeoutMs) {
                attachment.filterNotNull().first { !it.stopping }.service
            }

        private companion object {
            const val AWAIT_TIMEOUT_MS = 5_000L
        }
    }
