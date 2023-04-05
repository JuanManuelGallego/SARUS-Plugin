
package com.atakmap.android.plugintemplate;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.geofence.component.GeoFenceComponent;
import com.atakmap.android.geofence.data.GeoFence;
import com.atakmap.android.geofence.data.GeoFenceDatabase;
import com.atakmap.android.geofence.monitor.GeoFenceManager;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.importexport.Exportable;
import com.atakmap.android.items.MapItemsDatabase;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.plugintemplate.plugin.HTTPSRequest;
import com.atakmap.android.rubbersheet.data.ModelProjection;
import com.atakmap.android.rubbersheet.data.RubberModelData;
import com.atakmap.android.rubbersheet.data.RubberSheetUtils;
import com.atakmap.android.rubbersheet.data.create.AbstractCreationTask;
import com.atakmap.android.rubbersheet.data.create.CreateRubberModelTask;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.util.Circle;
import com.atakmap.android.vehicle.model.VehicleModel;
import com.atakmap.android.vehicle.model.VehicleModelInfo;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.plugintemplate.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.comms.CotServiceRemote.ConnectionListener;
import com.atakmap.comms.CotServiceRemote.CotEventListener;
import com.atakmap.comms.CotDispatcher;
import com.atakmap.comms.CotServiceRemote;
import com.atakmap.comms.NetworkUtils;
import com.atakmap.comms.TAKServer;
import com.atakmap.comms.TAKServerListener;
import com.atakmap.coremap.cot.event.CotAttribute;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.Model;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import gov.tak.platform.graphics.Color;

public class PluginTemplateDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = PluginTemplateDropDownReceiver.class
            .getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.plugintemplate.SHOW_PLUGIN";
    private final View SARUS_View;

    private final MapView mapView;
    private final Context pluginContext;

    private final TextView numberText;

    private final TextView debugText;

    private int number = 0;

    private final HTTPSRequest httpsRequest = new HTTPSRequest();
    private final EditText in_gps;

    private final EditText in_gps_model;
    private final EditText in_url;

    private final LinearLayout layout_button;
    private final LinearLayout layout_3D;
    private final LinearLayout layout_LiveFeed;
    private final LinearLayout layout_MissionControl;
    private final LinearLayout layout_Unit;

    private final Vector<LinearLayout> layouts = new Vector<>();

    private final File SARUSDownloadDirectory;

    private final GeoFenceManager geoFenceManager;

    private final MapGroup rootGroup;

    private String uuid;

    private Marker myMarker;

    private static final int RED = -1845559296;

    private static final int GREEN = -1862205696;

    private static final String CIRCLE = "u-d-c-c";

    private static final String POLYLINE = "u-d-f";

    private static final String RECTANGLE = "u-d-r";



    /**************************** CONSTRUCTOR *****************************/

    public PluginTemplateDropDownReceiver(final MapView mapView,
            final Context context) {
        super(mapView);
        this.mapView = mapView;
        this.pluginContext = context;
        SARUS_View = PluginLayoutInflater.inflate(context, R.layout.main_layout, null);
        SARUSDownloadDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), pluginContext.getString(R.string.downloadPath));
        numberText = SARUS_View.findViewById(R.id.numberText);
        debugText = SARUS_View.findViewById(R.id.debugText);
        GeoFenceComponent geoFenceComponent = new GeoFenceComponent();
        geoFenceManager = new GeoFenceManager(geoFenceComponent,mapView);
        rootGroup = mapView.getRootGroup();

        in_gps = SARUS_View.findViewById(R.id.in_GPS);
        in_gps_model = SARUS_View.findViewById(R.id.in_GPS_Model);
        in_url = SARUS_View.findViewById(R.id.in_URL);

        layout_button = SARUS_View.findViewById(R.id.layout_Buttons);
        layouts.add(layout_button);
        
        layout_3D = SARUS_View.findViewById(R.id.layout_3D);
        layouts.add(layout_3D);

        layout_LiveFeed = SARUS_View.findViewById(R.id.layout_LiveFeed);
        layouts.add(layout_LiveFeed);

        layout_MissionControl = SARUS_View.findViewById(R.id.layout_MissionControl);
        layouts.add(layout_MissionControl);

        layout_Unit = SARUS_View.findViewById(R.id.layout_Unit);
        layouts.add(layout_Unit);

        final Button number = SARUS_View.findViewById(R.id.btn_number);
        number.setOnClickListener(v -> addOne());

        final Button marker = SARUS_View.findViewById(R.id.btn_marker);
        marker.setOnClickListener(v -> createMarker());

        final Button https = SARUS_View.findViewById(R.id.btn_https);
        https.setOnClickListener(v -> Create3DModel());

        final Button button = SARUS_View.findViewById(R.id.btn_button);
        button.setOnClickListener(v -> selectLayout(layout_button));

        final Button threeD = SARUS_View.findViewById(R.id.btn_3D);
        threeD.setOnClickListener(v -> selectLayout(layout_3D));

        final Button liveFeed = SARUS_View.findViewById(R.id.btn_livefeed);
        liveFeed.setOnClickListener(v -> selectLayout(layout_LiveFeed));

        final Button missionControl = SARUS_View.findViewById(R.id.btn_MissionControl);
        missionControl.setOnClickListener(v -> selectLayout(layout_MissionControl));

        final Button unit = SARUS_View.findViewById(R.id.btn_Unit);
        unit.setOnClickListener(v -> selectLayout(layout_Unit));

        final Button geoFence = SARUS_View.findViewById(R.id.btn_geoFence);
        geoFence.setOnClickListener(v -> sendGeoFenceCoT());

        final Button sendCoT = SARUS_View.findViewById(R.id.btn_sendCoT);
        sendCoT.setOnClickListener(v -> sendCoTBroadcast());

        final Button home = SARUS_View.findViewById(R.id.btn_home);
        home.setOnClickListener(v -> sendReturnHomeCoT());

        final Button waypoint = SARUS_View.findViewById(R.id.btn_waypoint);
        waypoint.setOnClickListener(v -> sendWaypoint());

        selectLayout(layout_button);
    }
    /**************************** PRIVATE METHODS *****************************/

    private void toast(String str) {
        Toast.makeText(getMapView().getContext(), str, Toast.LENGTH_LONG).show();
    }

    private GeoPoint extractGeoPoint(String gps)
    {
        String[] coords = gps.split(",");
        double lat = Double.parseDouble(coords[0]);
        double lon = Double.parseDouble(coords[1]);
        return new GeoPoint(lat, lon);
    }

    private String extractCoords(GeoPoint gps)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(gps.getLatitude())
                .append(',')
                .append(gps.getLongitude());

        return builder.toString();
    }

    private String extractCoords(GeoPoint[] geoPoints)
    {
        StringBuilder builder = new StringBuilder();

        for(GeoPoint geoPoint: geoPoints) {
            builder.append(',')
                    .append(geoPoint.getLatitude())
                    .append(',')
                    .append(geoPoint.getLongitude());
        }

        return builder.toString();
    }

    private String extractGeoFenceType(Shape shape)
    {
        switch (shape.getFillColor())
        {
            case RED:
                return "EXCLUDE";
            case GREEN:
                return "INCLUDE";
            default:
                return "UNDEFINED";
        }
    }

    private String extractGeoFenceData(GeoFence geoFence)
    {
        StringBuilder builder = new StringBuilder();

        String mapItemUuid = geoFence.getMapItemUid();
        builder.append(mapItemUuid).append(",");
        //builder.append("Namespace").append(",");
        builder.append(CoordinatedTime.currentTimeMillis()).append(",");

        MapItem mapItem = rootGroup.deepFindUID(mapItemUuid);

        if(mapItem == null)
        {
            return "Error, could not parse GeoFence";
        }

        Shape mapItemShape = (Shape) mapItem;

        switch (mapItemShape.getType())
        {
            case CIRCLE:
                builder.append(extractGeoFenceType(mapItemShape))
                        .append(',')
                        .append("CIRCLE")
                        .append(',')
                        .append(extractCoords(mapItemShape.getCenter().get()))
                        .append(',')
                        .append(geoFence.getRangeKM());
                break;

            case POLYLINE:

            case RECTANGLE:
                builder.append(extractGeoFenceType(mapItemShape))
                        .append(',')
                        .append("POLYGON")
                        .append(extractCoords(mapItemShape.getPoints()));
                break;

            default:
                builder.append("default: ")
                        .append(mapItemShape.getType());
                break;
        }

        return builder.toString();
    }

    private void sendCoT(CotDetail cotDetail)
    {
        //TAKServerListener takServerListener = TAKServerListener.getInstance();
        //TAKServer[] connectedServers = takServerListener.getConnectedServers();

        //Arrays.stream(connectedServers).findFirst().get().getConnectString();

        Marker marker = createMarker();
        CotEvent cotEvent = createDefaultEvent(marker);
        CotDetail detail = cotEvent.getDetail();

        detail.addChild(cotDetail);

        CotDetailManager.getInstance().addDetails(marker, cotEvent);

        String[] uids = {"Pionner3-AT"};
        //String[] uids = {Arrays.stream(connectedServers).findFirst().get().getUsername()};

        Bundle bundle = new Bundle();
        bundle.putStringArray("toUIDs", uids);
        //bundle.putStringArray("toConnectStrings", toConnectStrings);

        CotDispatcher externalDispatcher = CotMapComponent.getExternalDispatcher();
        externalDispatcher.dispatch(cotEvent, bundle);

        toast("CoT Dispatched");
        debugText.setText(cotEvent.toString());
    }

    private CotDetail getSarusDetail(String eventType)
    {
        CotDetail cotDetail = new CotDetail("SARUS");
        cotDetail.setAttribute("Event_Type", eventType);
        cotDetail.setAttribute("Name", "Pioneer_3-AT");
        cotDetail.setAttribute("Mission", "Find Waldo");
        cotDetail.setAttribute("Event_ID", UUID.randomUUID().toString());
        cotDetail.setAttribute("TimeStamp", String.valueOf(CoordinatedTime.currentTimeMillis()));

        return cotDetail;
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    public void selectLayout(LinearLayout selected_layout){
        for(LinearLayout layout : layouts){
            if(layout.getId() == selected_layout.getId()){
                layout.setVisibility(View.VISIBLE);
            } else {
                layout.setVisibility(View.GONE);
            }
        }
    }

    public void Create3DModel(){
        String filename = in_url.getText().toString();
        File file = new File(SARUSDownloadDirectory, filename);

        GeoPoint coords = extractGeoPoint(in_gps_model.getText().toString());
        GeoPointMetaData metaGPS = new GeoPointMetaData();
        metaGPS.set(coords);

        VehicleModelInfo vehicleModelInfo = new VehicleModelInfo(null, filename, file);
        VehicleModel vehicleModel = new VehicleModel(vehicleModelInfo, metaGPS, "f7a1cbb2-b3b8-11ed-afa1-0242ac120002");

        VehicleModelData rubberModelData = new VehicleModelData(vehicleModelInfo, metaGPS, vehicleModel.getUID());

        AbstractCreationTask.Callback callback = (abstractCreationTask, list) -> toast("Model Loaded");

        new CreateRubberModelTask(mapView, rubberModelData, false, callback).execute();
    }

    public void addOne(){
        numberText.setText(Integer.toString(++PluginTemplateDropDownReceiver.this.number));
    }

    public Marker createMarker() {
        PlacePointTool.MarkerCreator mc;

        if(!in_gps.getText().toString().isEmpty())
        {
            GeoPoint coords = extractGeoPoint(in_gps.getText().toString());
            GeoPointMetaData metaGPS = new GeoPointMetaData();
            metaGPS.set(coords);
            mc = new PlacePointTool.MarkerCreator(metaGPS);
        }
        else
        {
            mc = new PlacePointTool.MarkerCreator(getMapView().getPointWithElevation());
        }

        uuid = UUID.randomUUID().toString();
        mc.setUid(uuid);
        mc.setCallsign("Pioneer_3-AT");
        mc.setType("a-f-A");
        mc.showCotDetails(false);
        mc.setNeverPersist(true);
        //Marker m = mc.placePoint();
        myMarker = mc.placePoint();

        myMarker.setStyle(myMarker.getStyle()
                | Marker.STYLE_ROTATE_HEADING_MASK
                | Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);

        myMarker.setTrack(310, 20);
        myMarker.setMetaInteger("color", Color.YELLOW);
        //myMarker.setMetaString(UserIcon.IconsetPath, "34ae1613-9645-4222-a9d2-e5f243dea2865/Military/A10.png");
        myMarker.refresh(getMapView().getMapEventDispatcher(), null, this.getClass());
        //markerDic.add(name, mymarker)

        return myMarker;
    }

    public Marker createWaypoint() {
        PlacePointTool.MarkerCreator mc = new PlacePointTool.MarkerCreator(getMapView().getPointWithElevation());
        uuid = UUID.randomUUID().toString();

        mc.setUid(uuid);
        mc.setCallsign("Waypoint");
        mc.setType("b-m-p-s-p-i");
        mc.showCotDetails(true);
        mc.setNeverPersist(true);
        Marker waypointMarker = mc.placePoint();

        waypointMarker.setStyle(waypointMarker.getStyle()
                | Marker.STYLE_ROTATE_HEADING_MASK
                | Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);

        waypointMarker.setTrack(310, 20);
        waypointMarker.setMetaInteger("color", Color.MAGENTA);

        waypointMarker.refresh(getMapView().getMapEventDispatcher(), null, this.getClass());

        return waypointMarker;
    }

    public void updateMarker()
    {
        // idée: Faire un Dictionnaire avec un Nom et un Marker afin d'y acceder facilement
        // Cette fonction va recevoir le nom du Marker et des coordonnées
        // Cette fonction sera appellé lors de la reception des coordonnées du robot

        // Marker myMarker = markerDic.get(markerName);
        // GeoPoint newPoint = new GeoPoint(latitude, longitude);
        // myMarker.setPoint(newPoint);
        // myMarker.refresh(getMapView().getMapEventDispatcher(), null, this.getClass());


        myMarker.setMetaInteger("color", Color.CYAN);
        GeoPoint oldPoint = myMarker.getPoint();
        GeoPoint newPoint = new GeoPoint(oldPoint.getLatitude() + 1, oldPoint.getLongitude() + 1);
        myMarker.setPoint(newPoint);
        myMarker.refresh(getMapView().getMapEventDispatcher(), null, this.getClass());
    }

    public static String getEndpoint() {
        String myIp = NetworkUtils.getIP();
        if (FileSystemUtils.isEmpty(myIp))
            return null;

        return myIp + ":4242:tcp";
    }

    public CotEvent createDefaultEvent(final Marker marker) {
        CotEvent cotEvent = new CotEvent();

        GeoPoint point = marker.getPoint();

        cotEvent.setPoint(new CotPoint(point));
        cotEvent.setType(marker.getType());
        cotEvent.setUID(marker.getUID());
        String how = marker.getMetaString("how", null);
        if (how != null) {
            cotEvent.setHow(how);
        } else {
            // if how is missing, then the conversion to a marker will cause an invalid
            // CotEvent
            Log.d(TAG, "missing how type, filling in with h-e");
            cotEvent.setHow("h-e");
        }
        String opex = marker.getMetaString("opex", null);
        if (opex != null)
            cotEvent.setOpex(opex);
        String qos = marker.getMetaString("qos", null);
        if (qos != null)
            cotEvent.setQos(qos);
        String access = marker.getMetaString("access", null);
        if (access != null)
            cotEvent.setAccess(access);

        cotEvent.setVersion("2.0");

        // per Josh Sterling as part of the COVID 19 response bump the stale time
        // for markers from 300 (5 minutes) to 1 year
        int staleSeconds = 31536000;
        if (marker.hasMetaValue("cotDefaultStaleSeconds")) {
            staleSeconds = marker.getMetaInteger("cotDefaultStaleSeconds",
                    staleSeconds);
        }

        CoordinatedTime time = new CoordinatedTime();
        cotEvent.setTime(time);
        cotEvent.setStart(time);
        cotEvent.setStale(time.addSeconds(staleSeconds));

        CotDetail detail = new CotDetail("detail");
        cotEvent.setDetail(detail);

        if (marker.hasMetaValue("initialBearing")) {
            CotDetail heading = new CotDetail("initialBearing");
            heading.setAttribute("value",
                    Double.toString(marker
                            .getMetaDouble("initialBearing", 0.0d)));

            detail.addChild(heading);
        }

        if (marker.hasMetaValue("readiness")) {
            CotDetail status = new CotDetail("status");
            status.setAttribute("readiness",
                    String.valueOf(marker.getMetaBoolean("readiness", false)));
            detail.addChild(status);
        }

        if (marker.hasMetaValue("archive")) {
            CotDetail archive = new CotDetail("archive");
            detail.addChild(archive);
        }

        CotDetailManager.getInstance().addDetails(marker, cotEvent);

        return cotEvent;
    }

    public void sendCoTBroadcast()
    {
        Marker marker = createMarker();
        CotEvent cotEvent = createDefaultEvent(marker);

        CotDispatcher externalDispatcher = CotMapComponent.getExternalDispatcher();
        externalDispatcher.dispatch(cotEvent);

        toast("CoT Broadcasted");
    }

    public void sendGeoFenceCoT()
    {
        CotDetail sarusDetail = getSarusDetail("GeoFence");

        GeoFenceDatabase geoFenceDatabase = GeoFenceDatabase.instance();
        List<GeoFence> geoFences = geoFenceDatabase.getGeoFences(geoFenceManager);

        for (GeoFence geoFence: geoFences) {
            CotDetail geoFenceDetail = new CotDetail("GeoFence");
            geoFenceDetail.setInnerText(extractGeoFenceData(geoFence));
            sarusDetail.addChild(geoFenceDetail);
        }

        sendCoT(sarusDetail);
    }

    public void sendReturnHomeCoT()
    {
        CotDetail sarusDetail = getSarusDetail("Command");

        CotDetail commandDetail = new CotDetail("Command");
        commandDetail.setAttribute("Action", "ReturnHome");
        sarusDetail.addChild(commandDetail);

        sendCoT(sarusDetail);
    }

    public void sendWaypoint()
    {
        CotDetail sarusDetail = getSarusDetail("Waypoint");
        CotDetail waypointDetail = new CotDetail("Waypoint");
        Marker waypoint = createWaypoint();

        waypointDetail.setAttribute("Coordinates", extractCoords(waypoint.getPoint()));
        waypointDetail.setAttribute("lat", String.valueOf(waypoint.getPoint().getLatitude()));
        waypointDetail.setAttribute("lon", String.valueOf(waypoint.getPoint().getLongitude()));
        waypointDetail.setAttribute("alt", String.valueOf(waypoint.getPoint().getAltitude()));
        sarusDetail.addChild(waypointDetail);

        sendCoT(sarusDetail);
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {

            Log.d(TAG, "showing plugin drop down");
            showDropDown(SARUS_View, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false, this);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

    private static class VehicleModelData extends RubberModelData {
        String uid;
        VehicleModelInfo info;

        VehicleModelData(VehicleModelInfo info, GeoPointMetaData center, String uid) {
            this.info = info;
            this.label = info.name;
            this.center = center;
            this.uid = uid;
            this.rotation = new double[]{0.0, 0.0, 0.0};
            this.scale = new double[]{1.0, 1.0, 1.0};
            this.projection = ModelProjection.ENU_FLIP_YZ;
            this.dimensions = new double[]{10.0, 10.0, 10.0};
            this.file = info.file;
            Model model = info.getModel();
            if (model != null) {
                Envelope e = model.getAABB();
                this.dimensions[0] = Math.abs(e.maxX - e.minX);
                this.dimensions[1] = Math.abs(e.maxY - e.minY);
                this.dimensions[2] = Math.abs(e.maxZ - e.minZ);
            }

            this.points = RubberSheetUtils.computeCorners(center.get(), this.dimensions[1], this.dimensions[0], 0.0);
        }

        public String getUID() {
            return this.uid;
        }
    }

}
