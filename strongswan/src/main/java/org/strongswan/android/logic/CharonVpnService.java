/*
 * Copyright (C) 2012-2018 Tobias Brunner
 * Copyright (C) 2012 Giuliano Grassi
 * Copyright (C) 2012 Ralf Sager
 * HSR Hochschule fuer Technik Rapperswil
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.  See <http://www.fsf.org/copyleft/gpl.txt>.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 */

package org.strongswan.android.logic;

import static com.windscribe.common.CommonUtilKt.startSafeForeground;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.security.KeyChain;
import android.security.KeyChainException;
import android.system.OsConstants;
import android.util.Log;

import com.windscribe.common.DNSDetails;
import com.windscribe.common.DnsType;
import com.windscribe.common.tunnel.IpNetwork;
import com.windscribe.common.tunnel.TunConfig;
import com.windscribe.common.tunnel.VpnHost;

import org.strongswan.android.data.VpnProfile;
import org.strongswan.android.data.VpnProfile.SelectedAppsHandling;
import org.strongswan.android.data.VpnProfileDataSource;
import org.strongswan.android.data.VpnType.VpnTypeFeature;
import org.strongswan.android.logic.VpnStateService.ErrorState;
import org.strongswan.android.logic.VpnStateService.State;
import org.strongswan.android.logic.imc.ImcState;
import org.strongswan.android.logic.imc.RemediationInstruction;
import org.strongswan.android.utils.Constants;
import org.strongswan.android.utils.IPRange;
import org.strongswan.android.utils.IPRangeSet;
import org.strongswan.android.utils.SettingsWriter;
import org.strongswan.android.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.SortedSet;


/**
 * The IKEv2 engine.
 *
 * <h3>Constraints this class cannot break</h3>
 * {@code libandroidbridge.so} resolves classes and methods <b>by name</b>, so the following are
 * non-negotiable without rebuilding the {@code .so} (docs/architecture/VPN_BACKEND.md §2):
 *
 * <ul>
 *   <li><b>C1</b> — the class must stay {@code org.strongswan.android.logic.CharonVpnService} with a
 *       nested {@code $BuilderAdapter}.</li>
 *   <li><b>C2</b> — the instance must expose {@code public boolean protect(int)}, hand-written
 *       below since this class is not a {@code VpnService}. There is no compile-time signal: miss
 *       it and {@code GetMethodID} fails on threads charon owns, routing IKE/ESP into the tunnel.
 *       Hence the loud assert rather than a quiet {@code false}.</li>
 *   <li><b>C3</b> — the instance must remain a {@code Context}, because native constructs
 *       {@code NetworkManager(Context)} and {@code Scheduler(Context)} from it. Satisfied by
 *       {@link ContextWrapper}.</li>
 *   <li><b>C4</b> — {@code BuilderAdapter} keeps its method names and signatures, and
 *       {@code establish()}/{@code establishNoDns()} return a <b>detached, native-owned</b>
 *       descriptor — the mux's socketpair, not a TUN.</li>
 *   <li><b>C5</b> — {@code getAndroidVersion()} / {@code getDeviceString()} keep their signatures.</li>
 * </ul>
 *
 * <p>Charon replaces the TUN mid-session on reauth (twice each), CHILD_SA rekey failure and roam,
 * so {@code requestTun} is re-entrant and this class calls it many times per connection.
 */
public abstract class CharonVpnService extends ContextWrapper implements Runnable, VpnStateService.VpnStateListener
{
	private static final String TAG = CharonVpnService.class.getSimpleName();
	private static final String VPN_SERVICE_ACTION = "android.net.VpnService";
	public static final String DISCONNECT_ACTION = "org.strongswan.android.CharonVpnService.DISCONNECT";
	private static final String NOTIFICATION_CHANNEL = "org.strongswan.android.CharonVpnService.VPN_STATE_NOTIFICATION";
	public static final String LOG_FILE = "charon.log";
	public static final String KEY_IS_RETRY = "retry";

	private String mLogFile;
	private String mAppDir;
	private VpnProfileDataSource mDataSource;
	private Thread mConnectionHandler;
	private VpnProfile mCurrentProfile;
	private volatile String mCurrentCertificateAlias;
	private volatile String mCurrentUserCertificateAlias;
	private VpnProfile mNextProfile;
	private volatile boolean mProfileUpdated;
	private volatile boolean mTerminate;
	private volatile boolean mIsDisconnecting;
	private volatile boolean mShowNotification;
	private BuilderAdapter mBuilderAdapter = new BuilderAdapter();

	public CharonVpnService(Context base)
	{
		super(base);
	}

	/** Everything that requires being the registered VpnService. */
	private volatile VpnHost mHost;

	/** The mux descriptor. Charon receives detached dups of it; this is our own copy. */
	private volatile ParcelFileDescriptor mEngineFd;

	public void attachHost(VpnHost host, ParcelFileDescriptor engineFd)
	{
		mHost = host;
		mEngineFd = engineFd;
	}

	public void detachHost()
	{
		mHost = null;
		mEngineFd = null;
	}

	/**
	 * C2. Resolved by name from native and called on threads charon owns, including a roam job that
	 * re-bypasses all sockets. Hand-written because this class is not a {@code VpnService}.
	 */
	public boolean protect(int socket)
	{
		VpnHost host = mHost;
		if (host == null)
		{
			// Loud on purpose: a quiet false routes IKE/ESP into the tunnel.
			Log.e(TAG, "protect(" + socket + ") with no host attached — IKE/ESP would be tunnelled");
			return false;
		}
		return host.protect(socket);
	}
	private Handler mHandler;
	private Handler mBackgroundHandler;
	private VpnStateService mService;
	private static final String ISRGX1Sha256Hash = "22B557A27055B33606B6559F37703928D3E4AD79F110B407D04986E1843543D1";
	private final Object mServiceLock = new Object();

	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name)
		{	/* since the service is local this is theoretically only called when the process is terminated */
			synchronized (mServiceLock)
			{
				mService = null;
			}
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			synchronized (mServiceLock)
			{
				mService = ((VpnStateService.LocalBinder)service).getService();
			}
			/* we are now ready to start the handler thread */
			mService.registerListener(CharonVpnService.this);
			mConnectionHandler.start();
		}
	};

	/**
	 * as defined in charonservice.h
	 */
	static final int STATE_CHILD_SA_UP = 1;
	static final int STATE_CHILD_SA_DOWN = 2;
	static final int STATE_AUTH_ERROR = 3;
	static final int STATE_PEER_AUTH_ERROR = 4;
	static final int STATE_LOOKUP_ERROR = 5;
	static final int STATE_UNREACHABLE_ERROR = 6;
	static final int STATE_CERTIFICATE_UNAVAILABLE = 7;
	static final int STATE_GENERIC_ERROR = 8;

	abstract protected boolean shouldEnablePacketLogging();

	/** Port ctrld is currently listening on. Read live; DNSDetails carries a stale snapshot. */
	abstract protected int getControlDPort();

	/**
	 * Starts a connection, replacing {@code onStartCommand}. No foreground promotion and no
	 * Always-on entry: the single {@code WindscribeVpnService} owns both.
	 */
	public void startFromIntent(Intent intent)
	{
		if (intent != null)
		{
			VpnProfile profile = null;
			boolean retry = false;

			if (VPN_SERVICE_ACTION.equals(intent.getAction()))
			{	/* triggered when Always-on VPN is activated */
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
				String uuid = pref.getString(Constants.PREF_DEFAULT_VPN_PROFILE, null);
				if (uuid == null || uuid.equals(Constants.PREF_DEFAULT_VPN_PROFILE_MRU))
				{
					uuid = pref.getString(Constants.PREF_MRU_VPN_PROFILE, null);
				}
				profile = mDataSource.getVpnProfile(uuid);
			}
			else if (!DISCONNECT_ACTION.equals(intent.getAction()))
			{
				Bundle bundle = intent.getExtras();
				if (bundle != null)
				{
					profile = mDataSource.getVpnProfile(bundle.getString(VpnProfileDataSource.KEY_UUID));
					if (profile != null)
					{
						String password = bundle.getString(VpnProfileDataSource.KEY_PASSWORD);
						profile.setPassword(password);

						retry = bundle.getBoolean(CharonVpnService.KEY_IS_RETRY, false);

						SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
						pref.edit().putString(Constants.PREF_MRU_VPN_PROFILE, profile.getUUID().toString())
							.apply();
					}
				}
			}
			if (profile != null && !retry)
			{	/* delete the log file if this is not an automatic retry */
				deleteFile(LOG_FILE);
			}
			setNextProfile(profile);
		}
	}

	/** Replaces {@code onCreate}. Called by the engine before the first connection. */
	public void create()
	{
		mLogFile = getFilesDir().getAbsolutePath() + File.separator + LOG_FILE;
		mAppDir = getFilesDir().getAbsolutePath();

		/* Explicitly the main looper: create() runs on whatever thread the orchestrator is on,
		 * where the no-arg constructor throws. */
		mHandler = new Handler(Looper.getMainLooper());

		/* handler for background work to avoid blocking main thread */
		android.os.HandlerThread backgroundThread = new android.os.HandlerThread("CharonVpnServiceBg");
		backgroundThread.start();
		mBackgroundHandler = new Handler(backgroundThread.getLooper());

		mDataSource = new VpnProfileDataSource(this);
		mDataSource.open();
		/* use a separate thread as main thread for charon */
		mConnectionHandler = new Thread(this);
		/* the thread is started when the service is bound */
		bindService(new Intent(this, VpnStateService.class),
					mServiceConnection, Service.BIND_AUTO_CREATE);

		createNotificationChannel();
	}

	/**
	 * VPN consent was revoked. A framework callback that only arrives on the registered service, so
	 * the host fans it out to here.
	 */
	public void revoked()
	{
		setNextProfile(null);
	}

	/** Replaces {@code onDestroy}. Called by the engine on stop. */
	public void destroy()
	{
		mTerminate = true;
		setNextProfile(null);

		// Safely clean up connection handler thread
		if (mConnectionHandler != null)
		{
			try
			{
				// Only try to join if the thread was actually started
				Thread.State state = mConnectionHandler.getState();
				if (state != Thread.State.NEW)
				{
					// Interrupt the thread to wake it from any wait() calls
					mConnectionHandler.interrupt();

					// Wait for thread to finish with a timeout (5 seconds)
					// This prevents indefinite blocking during service shutdown
					mConnectionHandler.join(5000);

					// If thread is still alive after timeout, log it but continue cleanup
					if (mConnectionHandler.isAlive())
					{
						Log.w(TAG, "Connection handler thread did not terminate within timeout");
					}
				}
			}
			catch (InterruptedException e)
			{
				Log.e(TAG, "Interrupted while waiting for connection handler to terminate", e);
				// Restore interrupt status
				Thread.currentThread().interrupt();
			}
			catch (Exception e)
			{
				Log.e(TAG, "Exception during connection handler cleanup", e);
			}
		}

		if (mService != null)
		{
			mService.unregisterListener(this);
			unbindService(mServiceConnection);
		}
		mDataSource.close();
		// Clean up background thread
		if (mBackgroundHandler != null)
		{
			mBackgroundHandler.getLooper().quit();
		}
		// The foreground notification belongs to the single VpnService now, so there is nothing to
		// stop here. It also must NOT be cancelled on this engine's teardown: the service has to
		// stay foreground across a protocol handover.
		if (mShowNotification)
		{
			try {
				mShowNotification = false;
			} catch (Exception e) {
				Log.e(TAG, "Failed to clear notification flag", e);
			}
		}
	}

	/**
	 * Set the profile that is to be initiated next. Notify the handler thread.
	 * This method is asynchronous to avoid blocking the caller (especially main thread).
	 *
	 * @param profile the profile to initiate
	 */
	public void setNextProfile(VpnProfile profile)
	{
		// Always post to background thread to avoid blocking caller
		if (mBackgroundHandler != null)
		{
			mBackgroundHandler.post(() -> setNextProfileInternal(profile));
		}
		else
		{
			// Fallback for early calls before onCreate completes
			setNextProfileInternal(profile);
		}
	}

	/**
	 * Internal implementation that actually sets the profile.
	 * Called from background thread to avoid blocking main thread.
	 */
	private void setNextProfileInternal(VpnProfile profile)
	{
		synchronized (this)
		{
			this.mNextProfile = profile;
			mProfileUpdated = true;
			notifyAll();
		}
	}

	@Override
	public void run()
	{
		while (true)
		{
			synchronized (this)
			{
				try
				{
					while (!mProfileUpdated)
					{
						wait();
					}

					mProfileUpdated = false;
					stopCurrentConnection();
					if (mNextProfile == null)
					{
						setState(State.DISABLED);
						if (mTerminate)
						{
							break;
						}
					}
					else
					{
						mCurrentProfile = mNextProfile;
						mNextProfile = null;

						/* store this in a separate (volatile) variable to avoid
						 * a possible deadlock during deinitialization */
						mCurrentCertificateAlias = mCurrentProfile.getCertificateAlias();
						mCurrentUserCertificateAlias = mCurrentProfile.getUserCertificateAlias();

						startConnection(mCurrentProfile);
						mIsDisconnecting = false;

						SimpleFetcher.enable();
						addNotification();
						mBuilderAdapter.setProfile(mCurrentProfile);
						if (initializeCharon(mBuilderAdapter, mLogFile, mAppDir, mCurrentProfile.getVpnType().has(VpnTypeFeature.BYOD),
											false))
						{
							Log.i(TAG, "charon started");

							if (mCurrentProfile.getVpnType().has(VpnTypeFeature.USER_PASS) &&
								mCurrentProfile.getPassword() == null)
							{	/* this can happen if Always-on VPN is enabled with an incomplete profile */
								setError(ErrorState.PASSWORD_MISSING);
								continue;
							}

							SettingsWriter writer = new SettingsWriter();
							writer.setValue("global.language", Locale.getDefault().getLanguage());
							writer.setValue("global.mtu", mCurrentProfile.getMTU());
							writer.setValue("global.nat_keepalive", mCurrentProfile.getNATKeepAlive());
							writer.setValue("global.rsa_pss", (mCurrentProfile.getFlags() & VpnProfile.FLAGS_RSA_PSS) != 0);
							writer.setValue("global.crl", (mCurrentProfile.getFlags() & VpnProfile.FLAGS_DISABLE_CRL) == 0);
							writer.setValue("global.ocsp", (mCurrentProfile.getFlags() & VpnProfile.FLAGS_DISABLE_OCSP) == 0);
							writer.setValue("connection.type", mCurrentProfile.getVpnType().getIdentifier());
							writer.setValue("connection.server", mCurrentProfile.getGateway());
							writer.setValue("connection.port", mCurrentProfile.getPort());
							writer.setValue("connection.username", mCurrentProfile.getUsername());
							writer.setValue("connection.password", mCurrentProfile.getPassword());
							writer.setValue("connection.local_id", mCurrentProfile.getLocalId());
							writer.setValue("connection.remote_id", mCurrentProfile.getRemoteId());
							writer.setValue("connection.certreq", (mCurrentProfile.getFlags() & VpnProfile.FLAGS_SUPPRESS_CERT_REQS) == 0);
							writer.setValue("connection.strict_revocation", (mCurrentProfile.getFlags() & VpnProfile.FLAGS_STRICT_REVOCATION) != 0);
							writer.setValue("connection.ike_proposal", mCurrentProfile.getIkeProposal());
							writer.setValue("connection.esp_proposal", mCurrentProfile.getEspProposal());
							initiate(writer.serialize());
						}
						else
						{
							Log.e(TAG, "failed to start charon");
							setError(ErrorState.GENERIC_ERROR);
							setState(State.DISABLED);
							mCurrentProfile = null;
						}
					}
				}
				catch (InterruptedException ex)
				{
					stopCurrentConnection();
					setState(State.DISABLED);
				}
			}
		}
	}

	/**
	 * Stop any existing connection by deinitializing charon.
	 */
	private void stopCurrentConnection()
	{
		VpnProfile profileToStop = null;
		VpnProfile nextProfileSnapshot = null;

		// Step 1: Prepare state while holding lock
		synchronized (this)
		{
		   /* the packet path is PacketMux's and outlives this engine, so there is nothing to stop */
			/*
			if (mNextProfile != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				mBuilderAdapter.setProfile(mNextProfile);
				mBuilderAdapter.establishBlocking();
			}
			 */

			if (mCurrentProfile != null)
			{
				setState(State.DISCONNECTING);
				mIsDisconnecting = true;
				SimpleFetcher.disable();
				profileToStop = mCurrentProfile;
				nextProfileSnapshot = mNextProfile;
			}
		}

		// Step 2: Call native code WITHOUT holding lock - this prevents deadlock
		if (profileToStop != null)
		{
			deinitializeCharon();
			Log.i(TAG, "charon stopped");

			// Step 3: Clean up while holding lock
			synchronized (this)
			{
				mCurrentProfile = null;
				if (nextProfileSnapshot == null)
				{	/* only do this if we are not connecting to another profile */
					removeNotification();
					mBuilderAdapter.closeBlocking();
				}
			}
		}
	}

	/**
	 * Add a permanent notification while we are connected to avoid the service getting killed by
	 * the system when low on memory.
	 */
	private void addNotification()
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				// Empty: the single VpnService owns the one persistent notification.
				mShowNotification = true;
			}
		});
	}

	/**
	 * Remove the permanent notification.
	 */
	private void removeNotification()
	{
		mHandler.post(new Runnable()
		{
			@Override
			public void run()
			{
				mShowNotification = false;

				// Don't call stopForeground() here - service must stay foreground until onDestroy()
				// to avoid ForegroundServiceDidNotStartInTimeException when started via startForegroundService()
				// Notification will be removed in onDestroy() by the wrapper service
			}
		});
	}

	/**
	 * Create a notification channel for Android 8+
	 */
	private void createNotificationChannel()
	{
		/*
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			NotificationChannel channel;
			channel = new NotificationChannel(NOTIFICATION_CHANNEL, getString(R.string.permanent_notification_name),
											  NotificationManager.IMPORTANCE_LOW);
			channel.setDescription(getString(R.string.permanent_notification_description));
			channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
			channel.setShowBadge(false);
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			notificationManager.createNotificationChannel(channel);
		}
		*/
	}


	/**
	 * Build a notification matching the current state
	 */
	/*
	private Notification buildNotification(boolean publicVersion)
	{
		VpnProfile profile = mService.getProfile();
		State state = mService.getState();
		ErrorState error = mService.getErrorState();
		String name = "";
		boolean add_action = false;

		if (profile != null)
		{
			name = profile.getName();
		}
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
				.setSmallIcon(R.drawable.ic_notification)
				.setCategory(NotificationCompat.CATEGORY_SERVICE)
				.setVisibility(publicVersion ? NotificationCompat.VISIBILITY_PUBLIC
											 : NotificationCompat.VISIBILITY_PRIVATE);
		int s = R.string.state_disabled;
		if (error != ErrorState.NO_ERROR)
		{
			s = mService.getErrorText();
			builder.setSmallIcon(R.drawable.ic_notification_warning);
			builder.setColor(ContextCompat.getColor(this, R.color.error_text));

			if (!publicVersion && profile != null)
			{
				int retry = mService.getRetryIn();
				if (retry > 0)
				{
					builder.setContentText(getResources().getQuantityString(R.plurals.retry_in, retry, retry));
					builder.setProgress(mService.getRetryTimeout(), retry, false);
				}

				Intent intent = new Intent(getApplicationContext(), VpnProfileControlActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.setAction(VpnProfileControlActivity.START_PROFILE);
				intent.putExtra(VpnProfileControlActivity.EXTRA_VPN_PROFILE_ID, profile.getUUID().toString());
				PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, intent,
																  PendingIntent.FLAG_UPDATE_CURRENT);
				builder.addAction(R.drawable.ic_notification_connecting, getString(R.string.retry), pending);
				add_action = true;
			}
		}
		else
		{
			builder.setProgress(0, 0, false);

			switch (state)
			{
				case CONNECTING:
					s = R.string.state_connecting;
					builder.setSmallIcon(R.drawable.ic_notification_connecting);
					builder.setColor(ContextCompat.getColor(this, R.color.warning_text));
					add_action = true;
					break;
				case CONNECTED:
					s = R.string.state_connected;
					builder.setColor(ContextCompat.getColor(this, R.color.success_text));
					builder.setUsesChronometer(true);
					add_action = true;
					break;
				case DISCONNECTING:
					s = R.string.state_disconnecting;
					break;
			}
		}
		builder.setContentTitle(getString(s));
		if (!publicVersion)
		{
			if (add_action)
			{
				Intent intent = new Intent(getApplicationContext(), VpnProfileControlActivity.class);
				intent.setAction(VpnProfileControlActivity.DISCONNECT);
				PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, intent,
																  PendingIntent.FLAG_UPDATE_CURRENT);
				builder.addAction(R.drawable.ic_notification_disconnect, getString(R.string.disconnect), pending);
			}
			if (error == ErrorState.NO_ERROR)
			{
				builder.setContentText(name);
			}
			builder.setPublicVersion(buildNotification(true));
		}

		Intent intent = new Intent(getApplicationContext(), MainActivity.class);
		PendingIntent pending = PendingIntent.getActivity(getApplicationContext(), 0, intent,
														  PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pending);
		return builder.build();
	}
	*/

	@Override
	public void stateChanged() {
		/* notification updates are the single service's job, driven by connection state */
	}

	/**
	 * Notify the state service about a new connection attempt.
	 * Called by the handler thread.
	 *
	 * @param profile currently active VPN profile
	 */
	private void startConnection(VpnProfile profile)
	{
		synchronized (mServiceLock)
		{
			if (mService != null)
			{
				mService.startConnection(profile);
			}
		}
	}

	/**
	 * Update the current VPN state on the state service. Called by the handler
	 * thread and any of charon's threads.
	 *
	 * @param state current state
	 */
	private void setState(State state)
	{
		synchronized (mServiceLock)
		{
			if (mService != null)
			{
				mService.setState(state);
			}
		}
	}

	/**
	 * Set an error on the state service. Called by the handler thread and any
	 * of charon's threads.
	 *
	 * @param error error state
	 */
	private void setError(ErrorState error)
	{
		synchronized (mServiceLock)
		{
			if (mService != null)
			{
				mService.setError(error);
			}
		}
	}

	/**
	 * Set the IMC state on the state service. Called by the handler thread and
	 * any of charon's threads.
	 *
	 * @param state IMC state
	 */
	private void setImcState(ImcState state)
	{
		synchronized (mServiceLock)
		{
			if (mService != null)
			{
				mService.setImcState(state);
			}
		}
	}

	/**
	 * Set an error on the state service. Called by the handler thread and any
	 * of charon's threads.
	 *
	 * @param error error state
	 */
	private void setErrorDisconnect(ErrorState error)
	{
		synchronized (mServiceLock)
		{
			if (mService != null)
			{
				if (!mIsDisconnecting)
				{
					mService.setError(error);
				}
			}
		}
	}

	/**
	 * Updates the state of the current connection.
	 * Called via JNI by different threads (but not concurrently).
	 *
	 * @param status new state
	 */
	public void updateStatus(int status)
	{
		switch (status)
		{
			case STATE_CHILD_SA_DOWN:
				if (!mIsDisconnecting)
				{
					setState(State.CONNECTING);
				}
				break;
			case STATE_CHILD_SA_UP:
				setState(State.CONNECTED);
				break;
			case STATE_AUTH_ERROR:
				setErrorDisconnect(ErrorState.AUTH_FAILED);
				break;
			case STATE_PEER_AUTH_ERROR:
				setErrorDisconnect(ErrorState.PEER_AUTH_FAILED);
				break;
			case STATE_LOOKUP_ERROR:
				setErrorDisconnect(ErrorState.LOOKUP_FAILED);
				break;
			case STATE_UNREACHABLE_ERROR:
				setErrorDisconnect(ErrorState.UNREACHABLE);
				break;
			case STATE_CERTIFICATE_UNAVAILABLE:
				setErrorDisconnect(ErrorState.CERTIFICATE_UNAVAILABLE);
				break;
			case STATE_GENERIC_ERROR:
				setErrorDisconnect(ErrorState.GENERIC_ERROR);
				break;
			default:
				Log.e(TAG, "Unknown status code received");
				break;
		}
	}

	/**
	 * Updates the IMC state of the current connection.
	 * Called via JNI by different threads (but not concurrently).
	 *
	 * @param value new state
	 */
	public void updateImcState(int value)
	{
		ImcState state = ImcState.fromValue(value);
		if (state != null)
		{
			setImcState(state);
		}
	}

	/**
	 * Add a remediation instruction to the VPN state service.
	 * Called via JNI by different threads (but not concurrently).
	 *
	 * @param xml XML text
	 */
	public void addRemediationInstruction(String xml)
	{
		for (RemediationInstruction instruction : RemediationInstruction.fromXml(xml))
		{
			synchronized (mServiceLock)
			{
				if (mService != null)
				{
					mService.addRemediationInstruction(instruction);
				}
			}
		}
	}

	/**
	 * Function called via JNI to generate a list of DER encoded CA certificates
	 * as byte array.
	 *
	 * @return a list of DER encoded CA certificates
	 */
	private byte[][] getTrustedCertificates()
	{
		ArrayList<byte[]> certs = new ArrayList<byte[]>();
		TrustedCertificateManager certman = TrustedCertificateManager.getInstance().load();
		try
		{
			String alias = this.mCurrentCertificateAlias;
			if (alias != null)
			{
				X509Certificate cert = certman.getCACertificateFromAlias(alias);
				if (cert == null)
				{
					return null;
				}
				certs.add(cert.getEncoded());
			} else {
				for (X509Certificate cert : certman.getAllCACertificates().values()) {
					certs.add(cert.getEncoded());
				}
			}
			// Android 7.1.1 and below do not trust ISRG Root X1 hence app will fail to connect.
			// Workaround is to add ISRG Root X1 to Strongswan's trusted certificate.
			// https://letsencrypt.org/certificates/
			// https://letsencrypt.org/docs/dst-root-ca-x3-expiration-september-2021/
			if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
				CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
				InputStream certificateInputStream = getAssets().open("isrgrootx1.pem");
				X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(certificateInputStream);
				certificate.checkValidity();
				String hash = assetFileToSha256Hash("isrgrootx1.pem");
				if (Objects.equals(hash, ISRGX1Sha256Hash)){
					certs.add(certificate.getEncoded());
				}

			}
		} catch (CertificateException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return certs.toArray(new byte[certs.size()][]);
	}

	public String assetFileToSha256Hash(String fileName) {
		try(InputStream inputStream = getAssets().open(fileName)) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			StringBuilder builder = new StringBuilder();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				builder.append(line);
				builder.append("\n");
			}
			digest.update(builder.toString().getBytes(StandardCharsets.UTF_8));
			byte [] hashBytes = digest.digest();
			return convertHashToString(hashBytes);
		} catch (Exception e) {
			return null;
		}
	}

	private static String convertHashToString(byte[] hashBytes) {
		StringBuilder returnVal = new StringBuilder();
		for (byte hashByte : hashBytes) {
			returnVal.append(Integer.toString((hashByte & 0xff) + 0x100, 16).substring(1));
		}
		return returnVal.toString().toUpperCase();
	}

	/**
	 * Function called via JNI to get a list containing the DER encoded certificates
	 * of the user selected certificate chain (beginning with the user certificate).
	 *
	 * Since this method is called from a thread of charon's thread pool we are safe
	 * to call methods on KeyChain directly.
	 *
	 * @return list containing the certificates (first element is the user certificate)
	 * @throws InterruptedException
	 * @throws KeyChainException
	 * @throws CertificateEncodingException
	 */
	private byte[][] getUserCertificate() throws KeyChainException, InterruptedException, CertificateEncodingException
	{
		ArrayList<byte[]> encodings = new ArrayList<byte[]>();
		X509Certificate[] chain = KeyChain.getCertificateChain(getApplicationContext(), mCurrentUserCertificateAlias);
		if (chain == null || chain.length == 0)
		{
			return null;
		}
		for (X509Certificate cert : chain)
		{
			encodings.add(cert.getEncoded());
		}
		return encodings.toArray(new byte[encodings.size()][]);
	}

	/**
	 * Function called via JNI to get the private key the user selected.
	 *
	 * Since this method is called from a thread of charon's thread pool we are safe
	 * to call methods on KeyChain directly.
	 *
	 * @return the private key
	 * @throws InterruptedException
	 * @throws KeyChainException
	 */
	private PrivateKey getUserKey() throws KeyChainException, InterruptedException
	{
		return KeyChain.getPrivateKey(getApplicationContext(), mCurrentUserCertificateAlias);
	}

	/**
	 * Initialization of charon, provided by libandroidbridge.so
	 *
	 * @param builder BuilderAdapter for this connection
	 * @param logfile absolute path to the logfile
	 * @param appdir absolute path to the data directory of the app
	 * @param byod enable BYOD features
	 * @param ipv6 enable IPv6 transport
	 * @return TRUE if initialization was successful
	 */
	public native boolean initializeCharon(BuilderAdapter builder, String logfile, String appdir, boolean byod, boolean ipv6);

	/**
	 * Deinitialize charon, provided by libandroidbridge.so
	 */
	public native void deinitializeCharon();

	/**
	 * Initiate VPN, provided by libandroidbridge.so
	 */
	public native void initiate(String config);

	/**
	 * Adapter for VpnService.Builder which is used to access it safely via JNI.
	 * There is a corresponding C object to access it from native code.
	 */
	public class BuilderAdapter
	{
		private VpnProfile mProfile;
		private BuilderCache mCache;
		private BuilderCache mEstablishedCache;

		public synchronized void setProfile(VpnProfile profile)
		{
			mProfile = profile;
			mCache = new BuilderCache(mProfile);
		}

		public synchronized boolean addAddress(String address, int prefixLength)
		{
			try
			{
				mCache.addAddress(address, prefixLength);
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
			return true;
		}

		public synchronized boolean addDnsServer(String address)
		{
			try
			{
				mCache.addDnsServer(address);
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
			return true;
		}

		public synchronized boolean addRoute(String address, int prefixLength)
		{
			try
			{
				mCache.addRoute(address, prefixLength);
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
			return true;
		}

		public synchronized boolean addSearchDomain(String domain)
		{
			try
			{
				mCache.addSearchDomain(domain);
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
			return true;
		}

		public synchronized boolean setMtu(int mtu)
		{
			try
			{
				mCache.setMtu(mtu);
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
			return true;
		}

		/**
		 * Hands charon a descriptor to run on, replacing the TUN only if it has to. Charon takes
		 * ownership of what it is given, so a fresh dup goes out every time.
		 */
		private synchronized ParcelFileDescriptor establishIntern()
		{
			VpnHost host = mHost;
			ParcelFileDescriptor mux = mEngineFd;
			if (host == null || mux == null)
			{
				Log.e(TAG, "establish() with no host attached");
				return null;
			}
			try
			{
				TunConfig config = mCache.toTunConfig(mProfile.getName());
				if (host.tunCovers(config))
				{
					Log.i(TAG, "Live TUN already carries this config; not re-establishing.");
				}
				else
				{
					host.requestTun(config);
				}
			}
			catch (Exception ex)
			{
				if (ex instanceof SecurityException && ex.getMessage() != null
					&& ex.getMessage().contains("INTERACT_ACROSS_USERS"))
				{
					setErrorDisconnect(ErrorState.MULTI_USER_PERMISSION);
				}
				ex.printStackTrace();
				return null;
			}

			ParcelFileDescriptor fd;
			try
			{
				fd = ParcelFileDescriptor.dup(mux.getFileDescriptor());
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return null;
			}

			/* the cache is kept so a reauth can rebuild the same interface without the DNS parts */
			mEstablishedCache = mCache;
			mCache = new BuilderCache(mProfile);
			return fd;
		}

		public synchronized int establish()
		{
			ParcelFileDescriptor fd = establishIntern();
			return fd != null ? fd.detachFd() : -1;
		}

		/**
		 * Asks the host for a default-routed blackhole interface; draining it is
		 * {@link com.windscribe.common.tunnel.PacketMux}'s DROP state. Its only call site is
		 * commented out, so this path is unexercised.
		 */
		public synchronized void establishBlocking()
		{
			VpnHost host = mHost;
			if (host == null)
			{
				return;
			}
			mCache.addAddress("172.16.252.1", 32);
			mCache.addRoute("0.0.0.0", 0);
			try
			{
				host.requestTun(mCache.toTunConfig(mProfile.getName()));
				mEstablishedCache = mCache;
				mCache = new BuilderCache(mProfile);
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
		}

		public synchronized void closeBlocking()
		{
			/* nothing to stop: the mux's DROP state is not a thread we own */
		}

		/**
		 * Reauth make-after-break: charon calls this, then {@code establish()} again. Same path as
		 * {@link #establish()}; a raw {@code Builder} would swap charon onto a TUN the mux is not pumping.
		 */
		public synchronized int establishNoDns()
		{
			VpnHost host = mHost;
			ParcelFileDescriptor mux = mEngineFd;
			if (mEstablishedCache == null || host == null || mux == null)
			{
				return -1;
			}
			try
			{
				TunConfig config = mEstablishedCache.toTunConfig(mProfile.getName());
				if (!host.tunCovers(config))
				{
					host.requestTun(config);
				}
				return ParcelFileDescriptor.dup(mux.getFileDescriptor()).detachFd();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return -1;
			}
		}
	}

	/**
	 * Cache non DNS related information so we can recreate the builder without
	 * that information when reestablishing IKE_SAs
	 */
	public class BuilderCache
	{
		private final List<IPRange> mAddresses = new ArrayList<>();
		private final List<IPRange> mRoutesIPv4 = new ArrayList<>();
		private final IPRangeSet mIncludedSubnetsv4 = new IPRangeSet();
		private final IPRangeSet mExcludedSubnets;
		private final int mSplitTunneling;
		private final SelectedAppsHandling mAppHandling;
		private final SortedSet<String> mSelectedApps;
		private final List<InetAddress> mDnsServers = new ArrayList<>();
		private final List<String> mSearchDomains = new ArrayList<>();
		private int mMtu;
		private boolean mIPv4Seen, mDnsServersConfigured;

		public BuilderCache(VpnProfile profile)
		{
			IPRangeSet included = IPRangeSet.fromString(profile.getIncludedSubnets());
			for (IPRange range : included)
			{
				if (range.getFrom() instanceof Inet4Address)
				{
					mIncludedSubnetsv4.add(range);
				}
			}
			mExcludedSubnets = IPRangeSet.fromString(profile.getExcludedSubnets());
			Integer splitTunneling = profile.getSplitTunneling();
			mSplitTunneling = splitTunneling != null ? splitTunneling : 0;
			SelectedAppsHandling appHandling = profile.getSelectedAppsHandling();
			mSelectedApps = profile.getSelectedAppsSet();
			/* exclude our own app, otherwise the fetcher is blocked */
			switch (appHandling)
			{
				case SELECTED_APPS_DISABLE:
					appHandling = SelectedAppsHandling.SELECTED_APPS_EXCLUDE;
					mSelectedApps.clear();
					/* fall-through */
				case SELECTED_APPS_EXCLUDE:
					// We never want our app bypass own VPN tunnel, that'd break our connectivity when
					// Kill-Switch is enabled.
					// mSelectedApps.add(getPackageName());
					break;
				case SELECTED_APPS_ONLY:
					// mSelectedApps.remove(getPackageName());
					break;
			}
			mAppHandling = appHandling;

			if (profile.getDnsServers() != null)
			{
				for (String server : profile.getDnsServers().split("\\s+"))
				{
					try
					{
						mDnsServers.add(Utils.parseInetAddress(server));
						recordAddressFamily(server);
						mDnsServersConfigured = true;
					}
					catch (UnknownHostException e)
					{
						e.printStackTrace();
					}
				}
			}

			/* set a default MTU, will be set by the daemon for regular interfaces */
			Integer mtu = profile.getMTU();
			mMtu = mtu == null ? Constants.MTU_MAX : mtu;
		}

		public void addAddress(String address, int prefixLength)
		{
			try
			{
				mAddresses.add(new IPRange(address, prefixLength));
				recordAddressFamily(address);
			}
			catch (UnknownHostException ex)
			{
				ex.printStackTrace();
			}
		}

		public void addDnsServer(String address)
		{
			/* ignore received DNS servers if any were configured */
			if (mDnsServersConfigured)
			{
				return;
			}

			try
			{
				mDnsServers.add(Utils.parseInetAddress(address));
				recordAddressFamily(address);
			}
			catch (UnknownHostException e)
			{
				e.printStackTrace();
			}
		}

		public void addRoute(String address, int prefixLength)
		{
			try
			{
				mRoutesIPv4.add(new IPRange(address, prefixLength));
			}
			catch (UnknownHostException ex)
			{
				ex.printStackTrace();
			}
		}

		public void addSearchDomain(String domain)
		{
			mSearchDomains.add(domain);
		}

		public void setMtu(int mtu)
		{
			mMtu = mtu;
		}

		public void recordAddressFamily(String address)
		{
			mIPv4Seen = true;
		}

		/**
		 * Translates charon's cached configuration into a {@link TunConfig}. Faithfulness matters:
		 * any gratuitous difference is one {@link TunConfig#coversSameInterfaceAs} notices, forcing
		 * an avoidable re-establish on reauth.
		 */
		public TunConfig toTunConfig(String session)
		{
			List<IpNetwork> addresses = new ArrayList<>();
			List<IpNetwork> routes = new ArrayList<>();
			List<InetAddress> dnsServers = new ArrayList<>(mDnsServers);
			List<String> allowedApps = new ArrayList<>();
			List<String> disallowedApps = new ArrayList<>();
			boolean allowAllFamilies = false;

			for (IPRange address : mAddresses)
			{
				addresses.add(new IpNetwork(address.getFrom(), address.getPrefix()));
			}
			/* add routes depending on whether split tunneling is allowed or not,
			 * that is, whether we have to handle and block non-VPN traffic */
			if ((mSplitTunneling & VpnProfile.SPLIT_TUNNELING_BLOCK_IPV4) == 0)
			{
				if (mIPv4Seen)
				{	/* split tunneling is used depending on the routes and configuration */
					IPRangeSet ranges = new IPRangeSet();
					if (mIncludedSubnetsv4.size() > 0)
					{
						ranges.add(mIncludedSubnetsv4);
					}
					else
					{
						ranges.addAll(mRoutesIPv4);
					}
					ranges.remove(mExcludedSubnets);
					for (IPRange subnet : ranges.subnets())
					{
						if (subnet.getFrom().isMulticastAddress())
						{	/* some Android versions don't seem to like multicast addresses here */
							continue;
						}
						routes.add(new IpNetwork(subnet.getFrom(), subnet.getPrefix()));
					}
				}
				else
				{	/* allow traffic that would otherwise be blocked to bypass the VPN */
					allowAllFamilies = true;
				}
			}
			else if (mIPv4Seen)
			{	/* only needed if we've seen any addresses.  otherwise, traffic
				 * is blocked by default (we also install no routes in that case) */
				try
				{
					routes.add(new IpNetwork(InetAddress.getByName("0.0.0.0"), 0));
				}
				catch (UnknownHostException e)
				{
					e.printStackTrace();
				}
			}
			/* apply selected applications */
			if (mSelectedApps.size() > 0)
			{
				switch (mAppHandling)
				{
					case SELECTED_APPS_EXCLUDE:
						disallowedApps.addAll(mSelectedApps);
						break;
					case SELECTED_APPS_ONLY:
						allowedApps.addAll(mSelectedApps);
						break;
					default:
						break;
				}
			}

			/* Excluded routes are not here: they are identical for every protocol and the host
			 * applies them once while building. */
			return new TunConfig(
				session,
				addresses,
				routes,
				dnsServers,
				new ArrayList<>(mSearchDomains),
				mMtu,
				allowedApps,
				disallowedApps,
				allowAllFamilies);
		}
	}

	/**
	 * Function called via JNI to determine information about the Android version.
	 */
	private static String getAndroidVersion()
	{
		String version = "Android " + Build.VERSION.RELEASE + " - " + Build.DISPLAY;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
		{
			version += "/" + Build.VERSION.SECURITY_PATCH;
		}
		return version;
	}

	/**
	 * Function called via JNI to determine information about the device.
	 */
	private static String getDeviceString()
	{
		return Build.MODEL + " - " + Build.BRAND + "/" + Build.PRODUCT + "/" + Build.MANUFACTURER;
	}
}
