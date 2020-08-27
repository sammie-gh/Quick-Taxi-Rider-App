package com.gh.sammie.quicktaxiriderapp.callbacks;

import com.gh.sammie.quicktaxiriderapp.model.DriverGeoModel;

public interface IFirebaseDriversInfoListener {
    void onDriverInfoLoadSuccess(DriverGeoModel driverGeoModel);

}
