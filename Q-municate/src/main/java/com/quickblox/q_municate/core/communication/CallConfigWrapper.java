package com.quickblox.q_municate.core.communication;

import android.os.Parcel;
import android.os.Parcelable;

import com.quickblox.videochat.webrtc.model.CallConfig;

public class CallConfigWrapper implements Parcelable {

    private CallConfig callConfig;

    CallConfigWrapper(CallConfig callConfig) {

        this.callConfig = callConfig;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }
}