package com.hokolinks.utils.lifecycle;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import com.hokolinks.activity.HokoActivity;
import com.hokolinks.utils.log.HokoLog;

import java.util.ArrayList;
import java.util.List;

/**
 * A small wrapper around activity life cycles to provide actual Application lifecycle status with
 * callbacks on background/foreground changes.
 */
public class ApplicationLifecycle {

    /**
     * Static instance to handle static callback registration
     */
    private static ApplicationLifecycle sInstance;
    private HokoApplicationStatus mApplicationStatus;
    private List<ApplicationLifecycleCallback> mCallbacks;

    /**
     * Private initializer, starting on FOREGROUND since it is what actually makes sense.
     *
     * @param context A context.
     */
    private ApplicationLifecycle(Context context) {
        mCallbacks = new ArrayList<>();
        mApplicationStatus = HokoApplicationStatus.FOREGROUND;
        try {
            registerActivityLifecycle(context);
        } catch (NullPointerException e) {
            HokoLog.e(e);
        }
    }

    /**
     * Retrieves the static instance for the ApplicationLifecycle handler.
     *
     * @param context A context.
     * @return The static ApplicationLifecycle instance.
     */
    private static ApplicationLifecycle getInstance(Context context) {
        if (sInstance == null)
            sInstance = new ApplicationLifecycle(context);
        return sInstance;
    }

    /**
     * The only public function. Allows the registration of application lifecycle callbacks, by
     * passing a context and a callback. You can register multiple callbacks, they shall be executed
     * in order of registration.
     *
     * @param context  A context.
     * @param callback A callback.
     */
    public static void registerApplicationLifecycleCallback(
            Context context, ApplicationLifecycleCallback callback) {
        getInstance(context).registerApplicationLifecycleCallback(callback);
    }

    /**
     * Registers an application lifecycle callback.
     *
     * @param callback A ApplicationLifecycleCallback instance.
     */
    private void registerApplicationLifecycleCallback(ApplicationLifecycleCallback callback) {
        mCallbacks.add(callback);
    }

    /**
     * Method to be called when an Application onPause is detected.
     * This will check the current application status is foreground, if it is it will be set as
     * background and call all callbacks' onPause() method.
     */
    private void onPause() {
        if (mApplicationStatus == HokoApplicationStatus.FOREGROUND) {
            mApplicationStatus = HokoApplicationStatus.BACKGROUND;
            for (ApplicationLifecycleCallback callback : mCallbacks) {
                callback.onPause();
            }
        }
    }

    /**
     * Method to be called when an Application onResume is detected.
     * This will check the current application status is background, if it is it will be set as
     * foreground and call all callbacks' onResume() method.
     */
    private void onResume() {
        if (mApplicationStatus == HokoApplicationStatus.BACKGROUND) {
            mApplicationStatus = HokoApplicationStatus.FOREGROUND;
            for (ApplicationLifecycleCallback callback : mCallbacks) {
                callback.onResume();
            }
        }
    }

    /**
     * Background happens when an activity is paused and stopped without any resumes in the middle.
     * Foreground might happen on every resume, it is only triggered when the application's state is
     * BACKGROUND. Ignoring HokoActivity due to Intent flags.
     *
     * @param context A context.
     */
    private void registerActivityLifecycle(Context context) {
        if (context != null) {
            Application application = (Application) context.getApplicationContext();
            application.registerActivityLifecycleCallbacks(
                    new Application.ActivityLifecycleCallbacks() {


                        private ArrayList<HokoActivityStatus> statusHistory =
                                new ArrayList<>();

                        @Override
                        public void onActivityCreated(Activity activity, Bundle bundle) {
                        }

                        @Override
                        public void onActivityStarted(Activity activity) {
                        }

                        @Override
                        public void onActivityResumed(Activity activity) {
                            if (activity instanceof HokoActivity)
                                return;
                            if (statusHistory.size() != 0)
                                statusHistory.add(HokoActivityStatus.RESUMED);
                            onResume();
                        }

                        @Override
                        public void onActivityPaused(Activity activity) {
                            if (activity instanceof HokoActivity)
                                return;
                            statusHistory.add(HokoActivityStatus.PAUSED);
                        }


                        @Override
                        public void onActivityStopped(Activity activity) {
                            statusHistory.add(HokoActivityStatus.STOPPED);
                            handlePossibleBackground();

                        }

                        @Override
                        public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
                        }

                        @Override
                        public void onActivityDestroyed(Activity activity) {
                        }

                        private void handlePossibleBackground() {
                            if (statusHistory.get(0) == HokoActivityStatus.PAUSED
                                    && statusHistory.get(1) == HokoActivityStatus.STOPPED) {
                                onPause();
                            }
                            statusHistory = new ArrayList<>();
                        }

                    });
        } else {
            throw new NullPointerException();
        }
    }

    /**
     * The Activity statuses that actually matter.
     */
    private enum HokoActivityStatus {
        PAUSED, RESUMED, STOPPED
    }

    /**
     * The Application statuses for inner logic.
     */
    private enum HokoApplicationStatus {
        FOREGROUND, BACKGROUND
    }

}
