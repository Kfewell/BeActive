package org.swanseacharm.bactive;

import android.app.Application;
import org.acra.*;
import org.acra.annotation.*;

@ReportsCrashes(formKey="dDhJcVV1TnhORkVaZmhPQ3BzWnFSZEE6MQ")
public class bActiveApp extends Application {
	@Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        super.onCreate();
    }
}
