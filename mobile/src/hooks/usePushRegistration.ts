import { useEffect } from 'react';
import { Platform } from 'react-native';
import * as Device from 'expo-device';
import { isRunningInExpoGo } from 'expo';
import { notificationsApi } from '../api/notifications';
import { useAuth } from '../context/AuthContext';

/**
 * The Android channel id the backend's FirebasePushSender stamps on every push
 * (see FirebasePushSender.ANDROID_CHANNEL_ID). Android 8+ silently discards a
 * notification whose channel the app has not registered, so this must match EXACTLY.
 */
const ANDROID_CHANNEL_ID = 'canteen_orders';

/**
 * Remote push was removed from Expo Go in SDK 53, so fetching a device token there is a
 * guaranteed failure.
 *
 * Detected with `isRunningInExpoGo()` rather than `Constants.executionEnvironment`: the
 * former probes for the native `ExpoGo` module and is exactly what expo-notifications uses
 * internally, so this guard and the library can never disagree about which environment
 * we are in.
 */
const isExpoGo = isRunningInExpoGo();

/**
 * expo-notifications is loaded on demand rather than with a top-level import, and this is
 * the whole reason the module reads this way.
 *
 * Importing it prints a red console.error plus a warning in Expo Go, and neither comes from
 * a call we make: `index.js` re-exports `DevicePushTokenAutoRegistration.fx`, which calls
 * `addPushTokenListener()` at module scope, which calls `warnOfExpoGoPushUsage()`. Those
 * fire the moment the module is evaluated, so no guard placed in our own code can prevent
 * them — the only way to avoid the noise is not to evaluate the module at all.
 *
 * In a development or production build nothing changes: the require runs on first use and
 * Metro resolves it exactly as a static import would.
 */
type NotificationsModule = typeof import('expo-notifications');
let cached: NotificationsModule | null = null;

function loadNotifications(): NotificationsModule {
  if (!cached) {
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    cached = require('expo-notifications') as NotificationsModule;
    // Controls how a notification behaves while the app is in the foreground. Set here
    // rather than at module scope so it still happens exactly once, on first load.
    cached.setNotificationHandler({
      handleNotification: async () => ({
        shouldPlaySound: true,
        shouldSetBadge: false,
        // shouldShowAlert is deprecated in favour of these two, which split "show a
        // heads-up banner" from "keep it in the notification tray".
        shouldShowBanner: true,
        shouldShowList: true,
      }),
    });
  }
  return cached;
}

/**
 * Registers this device with the backend so it can receive order-status pushes.
 *
 * Requires a development build. In Expo Go this is a no-op: the app itself works
 * completely, order updates simply arrive when a screen refreshes instead of as a push.
 */
export function usePushRegistration() {
  const { user } = useAuth();

  useEffect(() => {
    if (!user) return;

    // Bail before touching the library at all — see loadNotifications() above. The channel
    // registration that used to happen here was only a warm-up for the first real build,
    // and a real build performs it on its own first run anyway.
    if (isExpoGo) return;

    (async () => {
      try {
        const Notifications = loadNotifications();

        if (Platform.OS === 'android') {
          await Notifications.setNotificationChannelAsync(ANDROID_CHANNEL_ID, {
            name: 'Order updates',
            importance: Notifications.AndroidImportance.HIGH,
          });
        }

        if (!Device.isDevice) {
          return; // emulators without Play Services cannot receive push
        }

        const { status: existingStatus } = await Notifications.getPermissionsAsync();
        let finalStatus = existingStatus;
        if (existingStatus !== 'granted') {
          const { status } = await Notifications.requestPermissionsAsync();
          finalStatus = status;
        }
        if (finalStatus !== 'granted') {
          return; // the user declined; not an error
        }

        const { data: fcmToken } = await Notifications.getDevicePushTokenAsync();
        await notificationsApi.registerDevice(fcmToken, Platform.OS === 'ios' ? 'ios' : 'android');
      } catch (error) {
        // Push is an enhancement, never a requirement — a failure here must not block
        // sign-in or any other part of the app.
        console.warn('[push] Registration failed:', error);
      }
    })();
  }, [user]);
}
