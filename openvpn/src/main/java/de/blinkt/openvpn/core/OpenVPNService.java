package de.blinkt.openvpn.core;

import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_CONNECTED;
import static de.blinkt.openvpn.core.ConnectionStatus.LEVEL_WAITING_FOR_USER_INPUT;
import static de.blinkt.openvpn.core.NetworkSpace.IpAddress;

import android.Manifest.permission;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.windscribe.common.DNSDetails;
import com.windscribe.common.DnsType;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.Vector;

import de.blinkt.openvpn.LaunchVPN;
import com.windscribe.common.tunnel.IpNetwork;
import com.windscribe.common.tunnel.TunConfig;
import com.windscribe.common.tunnel.VpnHost;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.activities.DisconnectVPN;
import de.blinkt.openvpn.api.ExternalAppDatabase;
import de.blinkt.openvpn.core.VpnStatus.ByteCountListener;
import de.blinkt.openvpn.core.VpnStatus.StateListener;

/**
 * The OpenVPN engine.
 *
 * <h3>Why this is a ContextWrapper and not a Service</h3>
 * The TUN belongs to the one {@code android.net.VpnService}, and engines are plain objects behind
 * it — see docs/architecture/VPN_BACKEND.md §2. It stays a {@link ContextWrapper} because the file
 * makes ~44 implicit {@code Context} calls and the management thread passes {@code this} around as
 * one.
 *
 * <p>It has no {@code establish()}, {@code protect()}, {@code onRevoke()}, {@code onBind()} or
 * notification of its own; the host owns all of them.
 */
public abstract class OpenVPNService extends ContextWrapper implements StateListener, Callback, ByteCountListener, IOpenVPNServiceInternal {
    public OpenVPNService(Context base) {
        super(base);
    }

    /** Everything that requires being the registered VpnService. Set before {@link #startConnection}. */
    @Nullable private VpnHost host;

    /** Mux descriptor handed to native over SCM_RIGHTS. Owned by the engine, dup'd per open. */
    @Nullable private ParcelFileDescriptor engineFd;

    /** The TunConfig behind the last openTun(), used to answer the reopen question. */
    @Nullable private TunConfig mLastTunConfig;

    public void attachHost(VpnHost host, ParcelFileDescriptor engineFd) {
        this.host = host;
        this.engineFd = engineFd;
    }

    public void detachHost() {
        this.host = null;
        this.engineFd = null;
    }

    /**
     * Marks a socket so it bypasses the tunnel. Native reaches this through the management thread
     * ("protectfd") and {@link IOpenVPNServiceInternal}; a non-establishing component may do it.
     */
    public boolean protect(int fd) {
        VpnHost h = host;
        if (h == null) {
            VpnStatus.logError("protect() with no host attached; socket would route into the tunnel");
            return false;
        }
        return h.protect(fd);
    }

    public static final String START_SERVICE = "de.blinkt.openvpn.START_SERVICE";
    public static final String START_SERVICE_STICKY = "de.blinkt.openvpn.START_SERVICE_STICKY";
    public static final String ALWAYS_SHOW_NOTIFICATION = "de.blinkt.openvpn.NOTIFICATION_ALWAYS_VISIBLE";
    public static final String DISCONNECT_VPN = "de.blinkt.openvpn.DISCONNECT_VPN";
    public static final String NOTIFICATION_CHANNEL_BG_ID = "com.windscribe.vpn";
    public static final String NOTIFICATION_CHANNEL_NEWSTATUS_ID = "com.windscribe.vpn";
    public static final String NOTIFICATION_CHANNEL_USERREQ_ID = "openvpn_userreq";

    public static final String VPNSERVICE_TUN = "vpnservice-tun";
    public final static String ORBOT_PACKAGE_NAME = "org.torproject.android";
    public static final String PAUSE_VPN = "de.blinkt.openvpn.PAUSE_VPN";
    private static final String RESUME_VPN = "de.blinkt.openvpn.RESUME_VPN";

    public static final String EXTRA_CHALLENGE_TXT = "de.blinkt.openvpn.core.CR_TEXT_CHALLENGE";
    public static final String EXTRA_CHALLENGE_OPENURL = "de.blinkt.openvpn.core.OPENURL_CHALLENGE";

    private static final int PRIORITY_MIN = -2;
    private static final int PRIORITY_DEFAULT = 0;
    private static final int PRIORITY_MAX = 2;
    private static boolean mNotificationAlwaysVisible = false;
    private static Class mNotificationActivityClass;
    private final Vector<String> mDnslist = new Vector<>();
    private final NetworkSpace mRoutes = new NetworkSpace();
    private final NetworkSpace mRoutesv6 = new NetworkSpace();
    private final Object mProcessLock = new Object();
    private String lastChannel;
    private Thread mProcessThread = null;
    private VpnProfile mProfile;
    private String mDomain = null;
    private CIDRIP mLocalIP = null;
    private int mMtu;
    private String mLocalIPv6 = null;
    private DeviceStateReceiver mDeviceStateReceiver;
    private boolean mDisplayBytecount = false;
    private boolean mStarting = false;
    private long mConnecttime;
    private OpenVPNManagement mManagement;
    private volatile boolean shouldRollbackConnection = false;

    private final IBinder mBinder = new IOpenVPNServiceInternal.Stub() {

        @Override
        public boolean protect(int fd) throws RemoteException {
            return OpenVPNService.this.protect(fd);
        }

        @Override
        public void userPause(boolean shouldbePaused) throws RemoteException {
            OpenVPNService.this.userPause(shouldbePaused);
        }

        @Override
        public boolean stopVPN(boolean replaceConnection) throws RemoteException {
            return OpenVPNService.this.stopVPN(replaceConnection);
        }

        @Override
        public void addAllowedExternalApp(String packagename) throws RemoteException {
            OpenVPNService.this.addAllowedExternalApp(packagename);
        }

        @Override
        public boolean isAllowedExternalApp(String packagename) throws RemoteException {
            return OpenVPNService.this.isAllowedExternalApp(packagename);

        }

        @Override
        public void challengeResponse(String repsonse) throws RemoteException {
            OpenVPNService.this.challengeResponse(repsonse);
        }


    };
    private String mLastTunCfg;
    private String mRemoteGW;
    private Handler guiHandler;
    private Toast mlastToast;
    private Runnable mOpenVPNThread;

    public abstract VpnProfile getProfile();

    // Returns if service should keep running
    protected abstract boolean onProcessRestore();

    protected abstract boolean shouldEnablePacketLogging();

    /** Port ctrld is currently listening on. Read live; DNSDetails carries a stale snapshot. */
    protected abstract int getControlDPort();

    // From: http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
    public static String humanReadableByteCount(long bytes, boolean speed, Resources res) {
        if (speed)
            bytes = bytes * 8;
        int unit = speed ? 1000 : 1024;


        int exp = Math.max(0, Math.min((int) (Math.log(bytes) / Math.log(unit)), 3));

        float bytesUnit = (float) (bytes / Math.pow(unit, exp));

        if (speed)
            switch (exp) {
                case 0:
                    return res.getString(R.string.bits_per_second, bytesUnit);
                case 1:
                    return res.getString(R.string.kbits_per_second, bytesUnit);
                case 2:
                    return res.getString(R.string.mbits_per_second, bytesUnit);
                default:
                    return res.getString(R.string.gbits_per_second, bytesUnit);
            }
        else
            switch (exp) {
                case 0:
                    return res.getString(R.string.volume_byte, bytesUnit);
                case 1:
                    return res.getString(R.string.volume_kbyte, bytesUnit);
                case 2:
                    return res.getString(R.string.volume_mbyte, bytesUnit);
                default:
                    return res.getString(R.string.volume_gbyte, bytesUnit);

            }
    }

    /**
     * Sets the activity which should be opened when tapped on the permanent notification tile.
     *
     * @param activityClass The activity class to open
     */
    public static void setNotificationActivityClass(Class<? extends Activity> activityClass) {
        mNotificationActivityClass = activityClass;
    }

    @Override
    public void addAllowedExternalApp(String packagename) throws RemoteException {
        ExternalAppDatabase extapps = new ExternalAppDatabase(OpenVPNService.this);
        extapps.addApp(packagename);
    }

    @Override
    public boolean isAllowedExternalApp(String packagename) throws RemoteException {
        ExternalAppDatabase extapps = new ExternalAppDatabase(OpenVPNService.this);
        return extapps.checkRemoteActionPermission(this, packagename);
    }

    @Override
    public void challengeResponse(String response) throws RemoteException {
        if (mManagement != null) {
            String b64response = Base64.encodeToString(response.getBytes(Charset.forName("UTF-8")), Base64.DEFAULT);
            mManagement.sendCRResponse(b64response);
        }
    }


    /**
     * VPN consent was revoked. This is a framework callback that only ever arrives on the registered
     * service, so it cannot be received here — the host fans it out.
     */
    public void revoked() {
        VpnStatus.logError(R.string.permission_revoked);
        if (mManagement != null) {
            mManagement.stopVPN(false);
        }
        endVpnService();
    }

    // Similar to revoke but do not try to stop process
    public void openvpnStopped() {
        endVpnService();
    }

    private void endVpnService() {
        synchronized (mProcessLock) {
            mProcessThread = null;
        }
        VpnStatus.removeByteCountListener(this);
        unregisterDeviceStateReceiver();
        ProfileManager.setConntectedVpnProfileDisconnected(this);
        mOpenVPNThread = null;
        if (!mStarting) {
            // The notification and service lifetime belong to the single VpnService.
            if (!mNotificationAlwaysVisible) {
                VpnStatus.removeStateListener(this);
            }
        }
    }

    private void showNotification(final String msg, String tickerText, @NonNull String channel,
            long when, ConnectionStatus status, Intent intent) {
    /*    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int icon = getIconByConnectionStatus(status);

        android.app.Notification.Builder nbuilder = new Notification.Builder(this);

        int priority;
        if (channel.equals(NOTIFICATION_CHANNEL_BG_ID))
            priority = PRIORITY_MIN;
        else if (channel.equals(NOTIFICATION_CHANNEL_USERREQ_ID))
            priority = PRIORITY_MAX;
        else
            priority = PRIORITY_DEFAULT;

        if (mProfile != null)
            nbuilder.setContentTitle(getString(R.string.notifcation_title, mProfile.mName));
        else
            nbuilder.setContentTitle(getString(R.string.notifcation_title_notconnect));

        nbuilder.setContentText(msg);
        nbuilder.setOnlyAlertOnce(true);
        nbuilder.setOngoing(true);

        nbuilder.setSmallIcon(icon);
        if (status == LEVEL_WAITING_FOR_USER_INPUT) {
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
            nbuilder.setContentIntent(pIntent);
        } else {
            nbuilder.setContentIntent(getGraphPendingIntent());
        }

        if (when != 0)
            nbuilder.setWhen(when);


        // Try to set the priority available since API 16 (Jellybean)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            jbNotificationExtras(priority, nbuilder);
            addVpnActionsToNotification(nbuilder);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            lpNotificationExtras(nbuilder, Notification.CATEGORY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //noinspection NewApi
            nbuilder.setChannelId(channel);
            if (mProfile != null)
                //noinspection NewApi
                nbuilder.setShortcutId(mProfile.getUUIDString());

        }

        if (tickerText != null && !tickerText.equals(""))
            nbuilder.setTicker(tickerText);

        @SuppressWarnings("deprecation")
        Notification notification = nbuilder.getNotification();

        int notificationId = channel.hashCode();

        mNotificationManager.notify(notificationId, notification);

        startForeground(notificationId, notification);

        if (lastChannel != null && !channel.equals(lastChannel)) {
            // Cancel old notification
            mNotificationManager.cancel(lastChannel.hashCode());
        }

        // Check if running on a TV
        if (runningOnAndroidTV() && !(priority < 0))
            guiHandler.post(new Runnable() {

                @Override
                public void run() {

                    if (mlastToast != null)
                        mlastToast.cancel();
                    String toastText = String.format(Locale.getDefault(), "%s - %s", mProfile.mName, msg);
                    mlastToast = Toast.makeText(getBaseContext(), toastText, Toast.LENGTH_SHORT);
                    mlastToast.show();
                }
            }); */
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void lpNotificationExtras(Notification.Builder nbuilder, String category) {
        nbuilder.setCategory(category);
        nbuilder.setLocalOnly(true);

    }

    private boolean runningOnAndroidTV() {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    }

    private int getIconByConnectionStatus(ConnectionStatus level) {
        switch (level) {
            case LEVEL_CONNECTED:
                return R.drawable.ic_stat_vpn;
            case LEVEL_AUTH_FAILED:
            case LEVEL_NONETWORK:
            case LEVEL_NOTCONNECTED:
                return R.drawable.ic_stat_vpn_offline;
            case LEVEL_CONNECTING_NO_SERVER_REPLY_YET:
            case LEVEL_WAITING_FOR_USER_INPUT:
                return R.drawable.ic_stat_vpn_outline;
            case LEVEL_CONNECTING_SERVER_REPLIED:
                return R.drawable.ic_stat_vpn_empty_halo;
            case LEVEL_VPNPAUSED:
                return android.R.drawable.ic_media_pause;
            case UNKNOWN_LEVEL:
            default:
                return R.drawable.ic_stat_vpn;

        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void jbNotificationExtras(int priority,
            android.app.Notification.Builder nbuilder) {
        try {
            if (priority != 0) {
                Method setpriority = nbuilder.getClass().getMethod("setPriority", int.class);
                setpriority.invoke(nbuilder, priority);

                Method setUsesChronometer = nbuilder.getClass().getMethod("setUsesChronometer", boolean.class);
                setUsesChronometer.invoke(nbuilder, true);

            }

            //ignore exception
        } catch (NoSuchMethodException | IllegalArgumentException |
                InvocationTargetException | IllegalAccessException e) {
            VpnStatus.logException(e);
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void addVpnActionsToNotification(Notification.Builder nbuilder) {
        Intent disconnectVPN = new Intent(this, DisconnectVPN.class);
        disconnectVPN.setAction(DISCONNECT_VPN);
        PendingIntent disconnectPendingIntent = PendingIntent.getActivity(this, 0, disconnectVPN, 0);

        nbuilder.addAction(R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.cancel_connection), disconnectPendingIntent);

        Intent pauseVPN = new Intent(this, OpenVPNService.class);
        if (mDeviceStateReceiver == null || !mDeviceStateReceiver.isUserPaused()) {
            pauseVPN.setAction(PAUSE_VPN);
            PendingIntent pauseVPNPending = PendingIntent.getService(this, 0, pauseVPN, 0);
            nbuilder.addAction(R.drawable.ic_menu_pause,
                    getString(R.string.pauseVPN), pauseVPNPending);

        } else {
            pauseVPN.setAction(RESUME_VPN);
            PendingIntent resumeVPNPending = PendingIntent.getService(this, 0, pauseVPN, 0);
            nbuilder.addAction(R.drawable.ic_menu_play,
                    getString(R.string.resumevpn), resumeVPNPending);
        }
    }

    PendingIntent getUserInputIntent(String needed) {
        Intent intent = new Intent(getApplicationContext(), LaunchVPN.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        intent.putExtra("need", needed);
        Bundle b = new Bundle();
        b.putString("need", needed);
        PendingIntent pIntent = PendingIntent.getActivity(this, 12, intent, 0);
        return pIntent;
    }

    PendingIntent getGraphPendingIntent() {
        // Let the configure Button show the Log


        Intent intent = new Intent();
        intent.setComponent(new ComponentName(this, getPackageName() + ".activities.MainActivity"));

        intent.putExtra("PAGE", "graph");
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        PendingIntent startLW = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            startLW = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        } else {
            startLW = PendingIntent.getActivity(this, 0, intent, 0);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return startLW;

    }

    synchronized void registerDeviceStateReceiver(OpenVPNManagement magnagement) {
        // Registers BroadcastReceiver to track network connection changes.
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        mDeviceStateReceiver = new DeviceStateReceiver(magnagement);

        // Fetch initial network state
        mDeviceStateReceiver.networkStateChange(this);

        registerReceiver(mDeviceStateReceiver, filter);
        VpnStatus.addByteCountListener(mDeviceStateReceiver);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            addLollipopCMListener(); */
    }

    @Nullable
    public DeviceStateReceiver getmDeviceStateReceiver() {
        return mDeviceStateReceiver;
    }

    synchronized void unregisterDeviceStateReceiver() {
        if (mDeviceStateReceiver != null)
            try {
                VpnStatus.removeByteCountListener(mDeviceStateReceiver);
                this.unregisterReceiver(mDeviceStateReceiver);
            } catch (IllegalArgumentException iae) {
                // I don't know why  this happens:
                // java.lang.IllegalArgumentException: Receiver not registered: de.blinkt.openvpn.NetworkSateReceiver@41a61a10
                // Ignore for now ...
                iae.printStackTrace();
            }
        mDeviceStateReceiver = null;

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            removeLollipopCMListener();*/

    }

    public void userPause(boolean shouldBePaused) {
        if (mDeviceStateReceiver != null)
            mDeviceStateReceiver.userPause(shouldBePaused);
    }

    @Override
    public boolean stopVPN(boolean replaceConnection) throws RemoteException {
        // The packet path belongs to PacketMux, which outlives this engine.
        if (getManagement() != null)
            return getManagement().stopVPN(replaceConnection);
        else
            return false;
    }

    /**
     * Starts the OpenVPN process for the current profile.
     *
     * <p>No process-restart branch, {@code START_STICKY} or always-on entry: those are
     * {@code WindscribeVpnService}'s. PAUSE/RESUME go through {@link #userPause}.
     *
     * @return true if the process was started.
     */
    public boolean startConnection() {
        VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);

        guiHandler = new Handler(getMainLooper());
        shouldRollbackConnection = false;

        VpnStatus.logInfo(R.string.building_configration);
        VpnStatus.updateStateString("VPN_GENERATE_CONFIG", "", R.string.building_configration, ConnectionStatus.LEVEL_START);

        mProfile = getProfile();
        if (mProfile == null) {
            VpnStatus.logError("No OpenVPN profile to connect with.");
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            updateShortCutUsage(mProfile);
        }
        mProfile.checkForRestart(this);

        /* start the OpenVPN process itself in a background thread */
        new Thread(new Runnable() {
            @Override
            public void run() {
                startOpenVPN();
            }
        }).start();

        ProfileManager.setConnectedVpnProfile(this, mProfile);
        VpnStatus.setConnectedVPNProfile(mProfile.getUUIDString());
        return true;
    }

    private void rollbackConnection() {
        VpnStatus.updateStateString("NOPROCESS", "", 0, ConnectionStatus.LEVEL_NOTCONNECTED);
        guiHandler.post(() -> {
            try {
                stopVPN(false);
                endVpnService();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private void updateShortCutUsage(VpnProfile profile) {
        if (profile == null)
            return;
        ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
        shortcutManager.reportShortcutUsed(profile.getUUIDString());
    }

    private void startOpenVPN() {
        if (shouldRollbackConnection) {
            rollbackConnection();
            return;
        }
       //  We write configs while creating profile because we need to inject routing ip etc.
//         try {
//             mProfile.writeConfigFile(this);
//         } catch (IOException e) {
//             VpnStatus.logException("Error writing config file", e);
//             endVpnService();
//             return;
//         }
        String nativeLibraryDirectory = getApplicationInfo().nativeLibraryDir;
        String tmpDir;
        try {
            tmpDir = getCacheDir().getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            tmpDir = "/tmp";
        }

        // Write OpenVPN binary
        String[] argv = VPNLaunchHelper.buildOpenvpnArgv(this);


        // Set a flag that we are starting a new VPN
        mStarting = true;
        // Stop the previous session by interrupting the thread.

        stopOldOpenVPNProcess();
        // An old running VPN should now be exited
        mStarting = false;

        if (shouldRollbackConnection) {
            rollbackConnection();
            return;
        }

        // Start a new session by creating a new thread.
        boolean useOpenVPN3 = VpnProfile.doUseOpenVPN3(this);

        // Open the Management Interface
        if (!useOpenVPN3) {
            // start a Thread that handles incoming messages of the managment socket
            OpenVpnManagementThread ovpnManagementThread = new OpenVpnManagementThread(mProfile, this);
            if (ovpnManagementThread.openManagementInterface(this)) {
                Thread mSocketManagerThread = new Thread(ovpnManagementThread, "OpenVPNManagementThread");
                mSocketManagerThread.start();
                mManagement = ovpnManagementThread;
                VpnStatus.logInfo("started Socket Thread");
            } else {
                endVpnService();
                return;
            }
        }

        Runnable processThread;
        if (useOpenVPN3) {
            OpenVPNManagement mOpenVPN3 = instantiateOpenVPN3Core();
            processThread = (Runnable) mOpenVPN3;
            mManagement = mOpenVPN3;
        } else {
            processThread = new OpenVPNThread(this, argv, nativeLibraryDirectory, tmpDir);
            mOpenVPNThread = processThread;
        }

        synchronized (mProcessLock)

        {
            mProcessThread = new Thread(processThread, "OpenVPNProcessThread");
            mProcessThread.start();
        }

        new Handler(getMainLooper()).post(new Runnable() {
                                              @Override
                                              public void run() {
                                                  if (mDeviceStateReceiver != null)
                                                      unregisterDeviceStateReceiver();

                                                  registerDeviceStateReceiver(mManagement);
                                              }
                                          }

        );
    }


    private void stopOldOpenVPNProcess() {
        if (mManagement != null) {
            if (mOpenVPNThread != null)
                ((OpenVPNThread) mOpenVPNThread).setReplaceConnection();
            if (mManagement.stopVPN(true)) {
                // an old was asked to exit, wait 1s
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }

        forceStopOpenVpnProcess();
    }

    public void forceStopOpenVpnProcess() {
        synchronized (mProcessLock) {
            if (mProcessThread != null) {
                mProcessThread.interrupt();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //ignore
                }
            }
        }
    }

    private OpenVPNManagement instantiateOpenVPN3Core() {
        try {
            Class cl = Class.forName("de.blinkt.openvpn.core.OpenVPNThreadv3");
            return (OpenVPNManagement) cl.getConstructor(OpenVPNService.class, VpnProfile.class).newInstance(this, mProfile);
        } catch (IllegalArgumentException | InstantiationException | InvocationTargetException |
                NoSuchMethodException | ClassNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }



    @Override
    public IBinder asBinder() {
        return mBinder;
    }

    /** Tears the engine down. Called by {@code OpenVpnEngine.stop()}, not by the framework. */
    public void destroy() {
        try {
            synchronized (mProcessLock) {
                if (mProcessThread != null) {
                    mManagement.stopVPN(true);
                }
            }
            // Through unregisterDeviceStateReceiver(), the only path that nulls the field;
            // otherwise endVpnService() unregisters a second time.
            unregisterDeviceStateReceiver();
            // Just in case unregister for state
            VpnStatus.removeStateListener(this);
            VpnStatus.flushLog();
        } catch (Exception ignored) {

        }
    }

    private String getTunConfigString() {
        // The format of the string is not important, only that
        // two identical configurations produce the same result
        String cfg = "TUNCFG UNQIUE STRING ips:";

        if (mLocalIP != null)
            cfg += mLocalIP.toString();
       /* if (mLocalIPv6 != null)
            cfg += mLocalIPv6;*/


        cfg += "routes: " + TextUtils.join("|", mRoutes.getNetworks(true));
        cfg += "excl. routes:" + TextUtils.join("|", mRoutes.getNetworks(false));
        cfg += "dns: " + TextUtils.join("|", mDnslist);
        cfg += "domain: " + mDomain;
        cfg += "mtu: " + mMtu;
        return cfg;
    }

    public boolean isPrivateIPAddress(String ipAddress) {
        String[] ipParts = ipAddress.split("\\.");
        if (ipParts.length != 4) {
            return false; // Invalid IP address format
        }
        int[] ipBytes = new int[4];
        for (int i = 0; i < 4; i++) {
            try {
                ipBytes[i] = Integer.parseInt(ipParts[i]);
            } catch (NumberFormatException e) {
                return false; // Invalid IP address format
            }
        }
        if (ipBytes[0] == 10) {
            return true;
        } else if (ipBytes[0] == 172 && ipBytes[1] >= 16 && ipBytes[1] <= 31) {
            return true;
        } else if (ipBytes[0] == 192 && ipBytes[1] == 168) {
            return true;
        } else return ipBytes[0] == 169 && ipBytes[1] == 254;
    }

    /**
     * Called by the management thread when native asks for a tun device.
     *
     * <p>Assembles a protocol-neutral {@link TunConfig} from the pushed values and asks the host,
     * which re-establishes only if the live interface cannot carry it.
     *
     * <p>Native receives the mux's socketpair descriptor, not the TUN. OpenVPN does plain
     * {@code read()}/{@code write()} on whatever arrives over SCM_RIGHTS.
     *
     * <p>A fresh dup each time: {@code sendTunFD()} closes its copy after the SCM_RIGHTS send, and
     * native may reopen mid-session (OPEN_BEFORE_CLOSE).
     */
    public ParcelFileDescriptor openTun() {
        VpnHost h = host;
        ParcelFileDescriptor mux = engineFd;
        if (h == null || mux == null) {
            VpnStatus.logError("openTun() with no host attached");
            return null;
        }

        VpnStatus.logInfo(R.string.last_openvpn_tun_config);

        if (mLocalIP == null && mLocalIPv6 == null) {
            VpnStatus.logError(getString(R.string.opentun_no_ipaddr));
            return null;
        }

        TunConfig config;
        try {
            config = buildTunConfig();
        } catch (Exception e) {
            VpnStatus.logError("Could not assemble tun config: " + e.getMessage());
            return null;
        }

        // Reset the accumulated push state exactly as before, so the next reopen starts clean.
        mLastTunCfg = getTunConfigString();
        mLastTunConfig = config;
        mDnslist.clear();
        mRoutes.clear();
        mRoutesv6.clear();
        mLocalIP = null;
        mLocalIPv6 = null;
        mDomain = null;

        try {
            if (h.tunCovers(config)) {
                VpnStatus.logInfo("Live TUN already carries this config; not re-establishing.");
            } else {
                h.requestTun(config);
            }
            // Native gets a dup; the engine keeps the original for the life of the connection.
            return ParcelFileDescriptor.dup(mux.getFileDescriptor());
        } catch (Exception e) {
            if (e instanceof SecurityException && e.getMessage() != null
                    && e.getMessage().contains("INTERACT_ACROSS_USERS")) {
                VpnStatus.updateStateString("MULTI_USER_PERMISSION", "", R.string.state_user_vpn_permission, ConnectionStatus.LEVEL_MULTI_USER_PERMISSION);
            }
            VpnStatus.logError(R.string.tun_open_error);
            VpnStatus.logError("Error" + e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Translates the pushed OpenVPN configuration into a {@link TunConfig}.
     *
     * <p>Faithfulness matters more than tidiness: any gratuitous difference is one
     * {@link TunConfig#coversSameInterfaceAs} notices, forcing an avoidable re-establish. Excluded
     * routes are absent because the host applies them once while building.
     */
    private TunConfig buildTunConfig() throws java.net.UnknownHostException {
        List<IpNetwork> addresses = new ArrayList<>();
        List<IpNetwork> routes = new ArrayList<>();
        List<InetAddress> dnsServers = new ArrayList<>();
        List<String> allowedApps = new ArrayList<>();
        List<String> disallowedApps = new ArrayList<>();

        if (mLocalIP != null) {
            if (!VpnProfile.doUseOpenVPN3(this))
                addLocalNetworksToRoutes();
            addresses.add(new IpNetwork(InetAddress.getByName(mLocalIP.mIp), mLocalIP.len));
        }
        if (mLocalIPv6 != null) {
            String[] ipv6parts = mLocalIPv6.split("/");
            addresses.add(new IpNetwork(InetAddress.getByName(ipv6parts[0]), Integer.parseInt(ipv6parts[1])));
        }

        // Exclusive: the server pushes its own DNS and openvpn reports both back.
        if (mProfile.mOverrideDNS && !TextUtils.isEmpty(mProfile.mDNS1)) {
            try {
                dnsServers.add(InetAddress.getByName(mProfile.mDNS1));
            } catch (Exception e) {
                VpnStatus.logError(R.string.dns_add_error, mProfile.mDNS1, e.getLocalizedMessage());
            }
        } else {
            for (String dns : mDnslist) {
                try {
                    dnsServers.add(InetAddress.getByName(dns));
                } catch (Exception e) {
                    VpnStatus.logError(R.string.dns_add_error, dns, e.getLocalizedMessage());
                }
            }
        }

        Collection<NetworkSpace.IpAddress> positiveIPv4Routes = mRoutes.getPositiveIPList();
        NetworkSpace.IpAddress multicastRange = new NetworkSpace.IpAddress(new CIDRIP("224.0.0.0", 3), true);
        for (NetworkSpace.IpAddress route : positiveIPv4Routes) {
            try {
                if (multicastRange.containsNet(route)) {
                    VpnStatus.logDebug(R.string.ip_not_cidr, route.getIPv4Address(), route.networkMask, "multicast");
                } else {
                    routes.add(new IpNetwork(InetAddress.getByName(route.getIPv4Address()), route.networkMask));
                }
            } catch (Exception e) {
                VpnStatus.logError("Route rejected " + route + " " + e.getLocalizedMessage());
            }
        }
        addSamsungDnsRoute(positiveIPv4Routes, dnsServers, routes);

        // Unfloored: the user's packet size arrives as mMtu.
        int mtu = mMtu;

        collectAllowedVpnPackages(allowedApps, disallowedApps);

        String session = mProfile.mName;
        if (mLocalIP != null && mLocalIPv6 != null)
            session = getString(R.string.session_ipv6string, session, mLocalIP, mLocalIPv6);
        else if (mLocalIP != null)
            session = getString(R.string.session_ipv4string, session, mLocalIP);
        else
            session = getString(R.string.session_ipv4string, session, mLocalIPv6);

        if (dnsServers.isEmpty())
            VpnStatus.logInfo(R.string.warn_no_dns);

        boolean allowUnsetAF = !mProfile.mBlockUnusedAddressFamilies;

        List<String> searchDomains = new ArrayList<>();
        if (mDomain != null) {
            searchDomains.add(mDomain);
        }

        return new TunConfig(
                session,
                addresses,
                routes,
                dnsServers,
                searchDomains,
                mtu,
                allowedApps,
                disallowedApps,
                allowUnsetAF);
    }

    private boolean isLockdownEnabledCompat() {
        VpnHost h = host;
        return h != null && h.isSystemLockdownEnabled();
    }

    /**
     * Samsung ignores a DNS server no route covers and uses the network's own instead, so it needs
     * a host route. Appends to {@code routes} in place.
     */
    private void addSamsungDnsRoute(Collection<NetworkSpace.IpAddress> positiveRoutes,
                                    List<InetAddress> dnsServers, List<IpNetwork> routes) {
        if (!"samsung".equals(Build.BRAND) || dnsServers.isEmpty()) {
            return;
        }
        InetAddress resolver = dnsServers.get(0);
        if (!(resolver instanceof Inet4Address)) {
            return;
        }
        String ip = resolver.getHostAddress();
        try {
            NetworkSpace.IpAddress asNetwork = new NetworkSpace.IpAddress(new CIDRIP(ip, 32), true);
            for (NetworkSpace.IpAddress route : positiveRoutes) {
                if (route.containsNet(asNetwork)) {
                    return;
                }
            }
            if (mProfile.mAllowLocalLAN && isPrivateIPAddress(ip)) {
                // LAN bypass put it off the routes; a tunnel route would make it unreachable.
                VpnStatus.logWarning("Samsung ignores DNS servers outside the VPN's routes, and " + ip
                        + " is on the bypassed LAN. Turn LAN bypass off to use it.");
                return;
            }
            VpnStatus.logWarning("Samsung ignores DNS servers outside the VPN's routes; adding a route to "
                    + ip + " so it resolves.");
            routes.add(new IpNetwork(resolver, 32));
        } catch (Exception e) {
            VpnStatus.logError("Could not add the Samsung DNS route for " + ip + ": " + e.getLocalizedMessage());
        }
    }

    private void addLocalNetworksToRoutes() {
        for (String net: NetworkUtils.getLocalNetworks(this, false))
        {
            String[] netparts = net.split("/");
            String ipAddr = netparts[0];
            int netMask = Integer.parseInt(netparts[1]);
            if (ipAddr.equals(mLocalIP.mIp))
                continue;

            if (mProfile.mAllowLocalLAN) {
                if(isPrivateIPAddress(ipAddr)) {
                    mRoutes.addIP(new CIDRIP(ipAddr, netMask), false);
                } else {
                    VpnStatus.logDebug("Non Rfc-1918 address found "+ ipAddr);
                }
            }
        }

        // IPv6 is Lollipop+ only so we can skip the lower than KITKAT case
        /*if (mProfile.mAllowLocalLAN) {
            for (String net : NetworkUtils.getLocalNetworks(this, true)) {
                addRoutev6(net, false);;
            }
        }*/


    }


    /**
     * Resolves the profile's app rules into the two lists {@link TunConfig} carries. Entries that no
     * longer resolve are dropped, and an empty allow-list gets ourselves, so the interface is never
     * created with nothing routed through it.
     */
    private void collectAllowedVpnPackages(List<String> allowedApps, List<String> disallowedApps) {
        boolean profileUsesOrBot = false;
        for (Connection c : mProfile.mConnections) {
            if (c.mProxyType == Connection.ProxyType.ORBOT)
                profileUsesOrBot = true;
        }
        if (profileUsesOrBot)
            VpnStatus.logDebug("VPN Profile uses at least one server entry with Orbot. Setting up VPN so that OrBot is not redirected over VPN.");

        boolean atLeastOneAllowedApp = false;

        if (mProfile.mAllowedAppsVpnAreDisallowed && profileUsesOrBot) {
            disallowedApps.add(ORBOT_PACKAGE_NAME);
        }

        for (String pkg : new ArrayList<>(mProfile.mAllowedAppsVpn)) {
            if (!packageExists(pkg)) {
                mProfile.mAllowedAppsVpn.remove(pkg);
                VpnStatus.logInfo(R.string.app_no_longer_exists, pkg);
                continue;
            }
            if (mProfile.mAllowedAppsVpnAreDisallowed) {
                disallowedApps.add(pkg);
            } else if (!(profileUsesOrBot && pkg.equals(ORBOT_PACKAGE_NAME))) {
                allowedApps.add(pkg);
                atLeastOneAllowedApp = true;
            }
        }

        if (!mProfile.mAllowedAppsVpnAreDisallowed && !atLeastOneAllowedApp) {
            VpnStatus.logDebug(R.string.no_allowed_app, getPackageName());
            allowedApps.add(getPackageName());
        }

        if (mProfile.mAllowedAppsVpnAreDisallowed) {
            VpnStatus.logDebug(R.string.disallowed_vpn_apps_info, TextUtils.join(", ", mProfile.mAllowedAppsVpn));
        } else {
            VpnStatus.logDebug(R.string.allowed_vpn_apps_info, TextUtils.join(", ", mProfile.mAllowedAppsVpn));
        }
    }

    private boolean packageExists(String pkg) {
        try {
            getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public void addDNS(String dns) {
        mDnslist.add(dns);
    }

    public void setDomain(String domain) {
        if (mDomain == null) {
            mDomain = domain;
        }
    }

    /**
     * Route that is always included, used by the v3 core
     */
    public void addRoute(CIDRIP route, boolean include) {
        mRoutes.addIP(route, include);
    }

    public void addRoute(String dest, String mask, String gateway, String device) {
        CIDRIP route = new CIDRIP(dest, mask);
        boolean include = isAndroidTunDevice(device);

        IpAddress gatewayIP = new IpAddress(new CIDRIP(gateway, 32), false);

        if (mLocalIP == null) {
            VpnStatus.logError("Local IP address unset and received. Neither pushed server config nor local config specifies an IP addresses. Opening tun device is most likely going to fail.");
            return;
        }
        IpAddress localNet = new IpAddress(mLocalIP, true);
        if (localNet.containsNet(gatewayIP))
            include = true;

        if (gateway != null &&
                (gateway.equals("255.255.255.255") || gateway.equals(mRemoteGW)))
            include = true;


        if (route.len == 32 && !mask.equals("255.255.255.255")) {
            VpnStatus.logWarning(R.string.route_not_cidr, dest, mask);
        }

        if (route.normalise())
            VpnStatus.logWarning(R.string.route_not_netip, dest, route.len, route.mIp);

        mRoutes.addIP(route, include);
    }

    public void addRoutev6(String network, String device) {
        // Tun is opened after ROUTE6, no device name may be present
        boolean included = isAndroidTunDevice(device);
        addRoutev6(network, included);
    }

    public void addRoutev6(String network, boolean included) {
        /*String[] v6parts = network.split("/");

        try {
            Inet6Address ip = (Inet6Address) InetAddress.getAllByName(v6parts[0])[0];
            int mask = Integer.parseInt(v6parts[1]);
            mRoutesv6.addIPv6(ip, mask, included);

        } catch (UnknownHostException e) {
            VpnStatus.logException(e);
        }*/
    }

    private boolean isAndroidTunDevice(String device) {
        return device != null &&
                (device.startsWith("tun") || "(null)".equals(device) || VPNSERVICE_TUN.equals(device));
    }

    public void setMtu(int mtu) {
        mMtu = mtu;
    }

    public void setLocalIP(CIDRIP cdrip) {
        mLocalIP = cdrip;
    }

    public void setLocalIP(String local, String netmask, int mtu, String mode) {
        mLocalIP = new CIDRIP(local, netmask);
        mMtu = mtu;
        mRemoteGW = null;

        long netMaskAsInt = CIDRIP.getInt(netmask);

        if (mLocalIP.len == 32 && !netmask.equals("255.255.255.255")) {
            // get the netmask as IP

            int masklen;
            long mask;
            if ("net30".equals(mode)) {
                masklen = 30;
                mask = 0xfffffffc;
            } else {
                masklen = 31;
                mask = 0xfffffffe;
            }

            // Netmask is Ip address +/-1, assume net30/p2p with small net
            if ((netMaskAsInt & mask) == (mLocalIP.getInt() & mask)) {
                mLocalIP.len = masklen;
            } else {
                mLocalIP.len = 32;
                if (!"p2p".equals(mode))
                    VpnStatus.logWarning(R.string.ip_not_cidr, local, netmask, mode);
            }
        }
        if (("p2p".equals(mode) && mLocalIP.len < 32) || ("net30".equals(mode) && mLocalIP.len < 30)) {
            VpnStatus.logWarning(R.string.ip_looks_like_subnet, local, netmask, mode);
        }


        /* Workaround for Lollipop, it  does not route traffic to the VPNs own network mask */
        if (mLocalIP.len <= 31 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CIDRIP interfaceRoute = new CIDRIP(mLocalIP.mIp, mLocalIP.len);
            interfaceRoute.normalise();
            addRoute(interfaceRoute, true);
        }


        // Configurations are sometimes really broken...
        mRemoteGW = netmask;
    }

    public void setLocalIPv6(String ipv6addr) {
        /* mLocalIPv6 = ipv6addr;*/
    }

    @Override
    public void updateState(String state, String logmessage, int resid, ConnectionStatus level, Intent intent) {
        // If the process is not running, ignore any state,
        // Notification should be invisible in this state

        doSendBroadcast(state, level);
        if (mProcessThread == null && !mNotificationAlwaysVisible)
            return;

        String channel = NOTIFICATION_CHANNEL_NEWSTATUS_ID;
        // Display byte count only after being connected

        {
            if (level == LEVEL_CONNECTED) {
                mDisplayBytecount = true;
                mConnecttime = System.currentTimeMillis();
                if (!runningOnAndroidTV())
                    channel = NOTIFICATION_CHANNEL_BG_ID;
            } else {
                mDisplayBytecount = false;
            }

            // Other notifications are shown,
            // This also mean we are no longer connected, ignore bytecount messages until next
            // CONNECTED
            // Does not work :(
            showNotification(VpnStatus.getLastCleanLogMessage(this),
                    VpnStatus.getLastCleanLogMessage(this), channel, 0, level, intent);

        }
    }

    @Override
    public void setConnectedVPN(String uuid) {
    }

    private void doSendBroadcast(String state, ConnectionStatus level) {
        Intent vpnstatus = new Intent();
        vpnstatus.setAction("de.blinkt.openvpn.VPN_STATUS");
        vpnstatus.putExtra("status", level.toString());
        vpnstatus.putExtra("detailstatus", state);
        sendBroadcast(vpnstatus, permission.ACCESS_NETWORK_STATE);
    }

    @Override
    public void updateByteCount(long in, long out, long diffIn, long diffOut) {
        if (mDisplayBytecount) {
            String netstat = String.format(getString(R.string.statusline_bytecount),
                    humanReadableByteCount(in, false, getResources()),
                    humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true, getResources()),
                    humanReadableByteCount(out, false, getResources()),
                    humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true, getResources()));


            showNotification(netstat, null, NOTIFICATION_CHANNEL_BG_ID, mConnecttime, LEVEL_CONNECTED, null);
        }

    }

    @Override
    public boolean handleMessage(Message msg) {
        Runnable r = msg.getCallback();
        if (r != null) {
            r.run();
            return true;
        } else {
            return false;
        }
    }

    public OpenVPNManagement getManagement() {
        return mManagement;
    }

    /**
     * Answers native's PERSIST_TUN question on every reconnect.
     *
     * <p>The host's live TUN is consulted first: the interface belongs to the session and may have
     * been established by another protocol, so {@code mLastTunCfg} cannot answer for it (§5.2). If
     * the live one already carries what was pushed, there is nothing to reopen.
     */
    public String getTunReopenStatus() {
        String currentConfiguration = getTunConfigString();
        VpnHost h = host;
        if (h != null) {
            // buildTunConfig() adds this network's prefixes to mRoutes, and only openTun() clears
            // them. NOACTION skips openTun, so the probe is rolled back.
            TreeSet<NetworkSpace.IpAddress> routesBeforeProbe = mRoutes.snapshot();
            try {
                TunConfig pushed = buildTunConfig();
                if (h.tunCovers(pushed)) {
                    return "NOACTION";
                }
            } catch (Exception e) {
                // Fall through to the string comparison below rather than guessing.
                VpnStatus.logDebug("Could not evaluate pushed tun config: " + e.getMessage());
            } finally {
                mRoutes.restore(routesBeforeProbe);
            }
        }
        if (currentConfiguration.equals(mLastTunCfg)) {
            return "NOACTION";
        } else {
            String release = Build.VERSION.RELEASE;
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT && !release.startsWith("4.4.3")
                    && !release.startsWith("4.4.4") && !release.startsWith("4.4.5") && !release.startsWith("4.4.6"))
                // There will be probably no 4.4.4 or 4.4.5 version, so don't waste effort to do parsing here
                return "OPEN_AFTER_CLOSE";
            else
                return "OPEN_BEFORE_CLOSE";
        }
    }

    public void requestInputFromUser(int resid, String needed) {
        VpnStatus.updateStateString("NEED", "need " + needed, resid, LEVEL_WAITING_FOR_USER_INPUT);
        showNotification(getString(resid), getString(resid), NOTIFICATION_CHANNEL_NEWSTATUS_ID, 0, LEVEL_WAITING_FOR_USER_INPUT, null);
    }


    public void trigger_sso(String info) {
    /*    String channel = NOTIFICATION_CHANNEL_USERREQ_ID;
        String method = info.split(":", 2)[0];

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder nbuilder = new Notification.Builder(this);
        nbuilder.setAutoCancel(true);
        int icon = android.R.drawable.ic_dialog_info;
        nbuilder.setSmallIcon(icon);

        Intent intent;

        int reason;
        if (method.equals("OPEN_URL")) {
            String url = info.split(":", 2)[1];
            reason = R.string.openurl_requested;
            nbuilder.setContentTitle(getString(reason));

            nbuilder.setContentText(url);
            intent = VariantConfig.getOpenUrlIntent(this);
            intent.setData(Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } else if (method.equals("CR_TEXT")) {
            String challenge = info.split(":", 2)[1];
            reason = R.string.crtext_requested;
            nbuilder.setContentTitle(getString(reason));
            nbuilder.setContentText(challenge);

            intent = new Intent();
            intent.setComponent(new ComponentName(this, getPackageName() + ".activities.CredentialsPopup"));

            intent.putExtra(EXTRA_CHALLENGE_TXT, challenge);

        } else {
            VpnStatus.logError("Unknown SSO method found: " + method);
            return;
        }

        // updateStateString trigger the notification of the VPN to be refreshed, save this intent
        // to have that notification also this intent to be set
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        VpnStatus.updateStateString("USER_INPUT", "waiting for user input", reason, LEVEL_WAITING_FOR_USER_INPUT, intent);
        nbuilder.setContentIntent(pIntent);


        // Try to set the priority available since API 16 (Jellybean)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            jbNotificationExtras(PRIORITY_MAX, nbuilder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            lpNotificationExtras(nbuilder, Notification.CATEGORY_STATUS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //noinspection NewApi
            nbuilder.setChannelId(channel);
        }

        @SuppressWarnings("deprecation")
        Notification notification = nbuilder.getNotification();


        int notificationId = channel.hashCode();

        mNotificationManager.notify(notificationId, notification);
        */
    }
}
