/*
 * Copyright (C) 2022 Project Kaleidoscope
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quickstep.views;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

import com.android.launcher3.anim.AlphaUpdateListener;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.util.MultiValueAlpha;
import com.android.launcher3.R;
import com.android.quickstep.SysUINavigationMode.Mode;

import java.lang.Runnable;
import java.math.BigDecimal;

public class MemInfoView extends TextView {

    private static final int ALPHA_STATE_CTRL = 0;
    public static final int ALPHA_FS_PROGRESS = 1;

    public static final FloatProperty<MemInfoView> STATE_CTRL_ALPHA =
            new FloatProperty<MemInfoView>("state control alpha") {
                @Override
                public Float get(MemInfoView view) {
                    return view.getAlpha(ALPHA_STATE_CTRL);
                }

                @Override
                public void setValue(MemInfoView view, float v) {
                    view.setAlpha(ALPHA_STATE_CTRL, v);
                }
            };

    private DeviceProfile mDp;
    private MultiValueAlpha mAlpha;
    private ActivityManager mActivityManager;

    private Handler mHandler;
    private MemInfoWorker mWorker;

    public MemInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mAlpha = new MultiValueAlpha(this, 2);
        mAlpha.setUpdateVisibility(true);
        mActivityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
        mWorker = new MemInfoWorker();
    }

    /* Hijack this method to detect visibility rather than
     * onVisibilityChanged() because the the latter one can be
     * influenced by more factors, leading to unstable behavior. */
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);

        if (visibility == VISIBLE)
            mHandler.post(mWorker);
        else
            mHandler.removeCallbacks(mWorker);
    }

    public void setDp(DeviceProfile dp) {
        mDp = dp;
    }

    public void setAlpha(int alphaType, float alpha) {
        mAlpha.getProperty(alphaType).setValue(alpha);
    }

    public float getAlpha(int alphaType) {
        return mAlpha.getProperty(alphaType).getValue();
    }

    public void updateVerticalMargin(Mode mode) {
        LayoutParams lp = (LayoutParams)getLayoutParams();
        int bottomMargin;

        if (mode == Mode.THREE_BUTTONS)
            bottomMargin = mDp.memInfoMarginThreeButtonPx;
        else
            bottomMargin = mDp.memInfoMarginGesturePx;

        lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin, bottomMargin);
        lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
    }

    private class MemInfoWorker implements Runnable {
        @Override
        public void run() {
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            mActivityManager.getMemoryInfo(memInfo);
            int availMemMiB = (int)((memInfo.availMem / 1048576L) + 512);
            int totalMemMiB = (int)(memInfo.totalMem / 1048576L);
            setText("RAM:" + " " + String.valueOf(availMemMiB) + "/" + String.valueOf(totalMemMiB) + " " +"MB");

            mHandler.postDelayed(this, 1000);
        }
    }
}
