
package com.atakmap.android.sarus;

import android.content.Context;
import android.content.Intent;

import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.sarusplugin.plugin.R;

public class SARUSPluginMapComponent extends DropDownMapComponent {

    private static final String TAG = "saruspluginMapComponent";

    private Context pluginContext;

    private SARUSPluginDropDownReceiver ddr;

    private CotDetailHandler sarusCotDetailHandler;



    public void onCreate(final Context context, Intent intent,
            final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;
        ddr = new SARUSPluginDropDownReceiver(
                view, context);

        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(SARUSPluginDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
    }
}
