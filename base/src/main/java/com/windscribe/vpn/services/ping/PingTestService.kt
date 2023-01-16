/*
 * Copyright (c) 2021 Windscribe Limited.
 */

package com.windscribe.vpn.services.ping

import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentWorkAroundService
import com.windscribe.vpn.ServiceInteractor
import com.windscribe.vpn.Windscribe.Companion.appContext
import com.windscribe.vpn.commonutils.WindUtilities
import com.windscribe.vpn.exceptions.TooHighLatencyError
import com.windscribe.vpn.repository.UserRepository
import com.windscribe.vpn.serverlist.entity.PingTime
import com.windscribe.vpn.serverlist.entity.StaticRegion
import com.windscribe.vpn.state.PreferenceChangeObserver
import com.windscribe.vpn.state.VPNConnectionStateManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.SingleSource
import io.reactivex.functions.Function
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.schedulers.Schedulers
import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import kotlin.math.roundToInt

class PingTestService : JobIntentWorkAroundService() {

    @Inject
    lateinit var serviceInteractor: ServiceInteractor

    @Inject
    lateinit var preferenceChangeObserver: PreferenceChangeObserver

    @Inject
    lateinit var vpnConnectionStateManager: VPNConnectionStateManager

    @Inject
    lateinit var userRepository: UserRepository

    private val logger = LoggerFactory.getLogger("ping_test_s")
    override fun onCreate() {
        super.onCreate()
        appContext.serviceComponent.inject(this)
    }

    override fun onHandleWork(intent: Intent) {
        if (userRepository.loggedIn()) {
            logger.debug("ping test service started")
            requestPings()
        } else {
            stopSelf()
        }
    }

    private fun cleanup() {
        if (!serviceInteractor.compositeDisposable.isDisposed) {
            serviceInteractor.compositeDisposable.dispose()
        }
    }

    private fun getPingResult(
            id: Int,
            regionId: Int,
            ip: String?,
            isStatic: Boolean,
            isPro: Boolean
    ): Single<PingTime> {
        return Single.fromCallable {
            val pingTime = PingTime()
            val dnsResolved = System.currentTimeMillis()
            try {
                val address = InetSocketAddress(ip, 443)
                val socket = Socket()
                socket.connect(address, 500)
                socket.close()
                val probeFinish = System.currentTimeMillis()
                pingTime.id = id
                pingTime.isPro = isPro
                pingTime.setRegionId(regionId)
                val time = (probeFinish - dnsResolved).toInt()
                pingTime.setPingTime(time)
                pingTime.setStatic(isStatic)
                return@fromCallable pingTime
            } catch (e: Exception) {
                pingTime.id = id
                pingTime.isPro = isPro
                pingTime.setRegionId(regionId)
                pingTime.setPingTime(-1)
                pingTime.setStatic(isStatic)
                return@fromCallable pingTime
            }
        }.subscribeOn(Schedulers.newThread())
    }

    private fun getPings(
            id: Int,
            regionId: Int,
            ip: String?,
            isStatic: Boolean,
            isPro: Boolean
    ): Single<PingTime> {
        return Single.fromCallable {
            if (ip == null) {
                throw Exception()
            }
            Inet4Address.getByName(ip)
        }.flatMap { inetAddress: InetAddress? ->
            val ping = Ping()
            val pingTime = PingTime()
            ping.run(inetAddress, 500).flatMap { timeMs: Long ->
                    pingTime.id = id
                    pingTime.isPro = isPro
                    pingTime.setRegionId(regionId)
                    pingTime.setPingTime(timeMs.toFloat().roundToInt())
                    pingTime.setStatic(isStatic)
                    Single.fromCallable { pingTime }
                }
        }.onErrorResumeNext { getPingResult(id, regionId, ip, isStatic, isPro) }
                .subscribeOn(Schedulers.computation())
    }

    private fun requestPings() {
        if (WindUtilities.isOnline()) {
            serviceInteractor.preferenceHelper.pingTestRequired = false
            logger.debug("Starting ping testing for all nodes.")
            serviceInteractor.compositeDisposable.add(
                    serviceInteractor.getAllCities()
                            .flatMapPublisher {
                                Flowable.fromIterable(it)
                            }.flatMapSingle {
                                val ip = it.pingIp
                                getPings(it.getId(), it.regionID, ip, false, it.pro == 1)
                            }.retry(1) {
                                it is TooHighLatencyError
                            }.takeUntil {
                                if (WindUtilities.isOnline() && !vpnConnectionStateManager.isVPNActive()) {
                                    return@takeUntil false
                                } else {
                                    serviceInteractor.preferenceHelper.pingTestRequired = true
                                    return@takeUntil true
                                }
                            }.flatMapCompletable {
                                serviceInteractor.addPing(it)
                            }.andThen(serviceInteractor.getAllStaticRegions())
                            .flatMapPublisher {
                                Flowable.fromIterable(it)
                            }.flatMapSingle(
                                    Function<StaticRegion, SingleSource<PingTime>> label@{
                                        if (it.staticIpNode != null) {
                                            return@label getPings(
                                                it.id,
                                                it.ipId,
                                                it.staticIpNode.ip.toString(),
                                                isStatic = true,
                                                isPro = true
                                            )
                                        } else {
                                            throw Exception("Static region has no ip")
                                        }
                                    }
                            ).retry(1) { throwable: Throwable? -> throwable is TooHighLatencyError }
                            .takeUntil {
                                if (WindUtilities.isOnline() && !vpnConnectionStateManager.isVPNActive()) {
                                    return@takeUntil false
                                } else {
                                    serviceInteractor.preferenceHelper.pingTestRequired = true
                                    return@takeUntil true
                                }
                            }.flatMapCompletable {
                                serviceInteractor.addPing(it)
                            }
                            .andThen(serviceInteractor.getLowestPingId())
                            .flatMapCompletable {
                                Completable.fromAction {
                                    serviceInteractor.preferenceHelper.lowestPingId = it
                                }
                            }.subscribeWith(object : DisposableCompletableObserver() {
                                override fun onComplete() {
                                    preferenceChangeObserver.postLatencyChange()
                                    logger.debug("Ping testing finished successfully.")
                                    cleanup()
                                }

                                override fun onError(e: Throwable) {
                                    serviceInteractor.preferenceHelper.pingTestRequired = true
                                    logger.debug("Ping testing failed :" + e.localizedMessage)
                                    cleanup()
                                }
                            })
            )
        }
    }

    companion object {

        private const val PING_JOB_ID = 7777
        private fun enqueueWork(context: Context, intent: Intent) {
            try {
                enqueueWork(context, PingTestService::class.java, PING_JOB_ID, intent)
            } catch (ignored: IllegalStateException) {
            }
        }

        private fun isJobServiceOn(context: Context): Boolean {
            val scheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
            var hasBeenScheduled = false
            for (jobInfo in scheduler.allPendingJobs) {
                if (jobInfo.id == PING_JOB_ID) {
                    hasBeenScheduled = true
                    break
                }
            }
            return hasBeenScheduled
        }

        @JvmStatic
        fun startPingTestService() {
            if (!isJobServiceOn(appContext)) {
                enqueueWork(
                        appContext,
                        Intent(appContext, PingTestService::class.java)
                )
            }
        }
    }
}
