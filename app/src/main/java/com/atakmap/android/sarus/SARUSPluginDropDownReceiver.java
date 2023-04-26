package com.atakmap.android.sarus;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.drawing.mapItems.DrawingShape;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.geofence.component.GeoFenceComponent;
import com.atakmap.android.geofence.data.GeoFence;
import com.atakmap.android.geofence.data.GeoFenceDatabase;
import com.atakmap.android.geofence.monitor.GeoFenceManager;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.Shape;
import com.atakmap.android.sarusplugin.plugin.R;
import com.atakmap.android.rubbersheet.data.ModelProjection;
import com.atakmap.android.rubbersheet.data.RubberModelData;
import com.atakmap.android.rubbersheet.data.RubberSheetUtils;
import com.atakmap.android.rubbersheet.data.create.AbstractCreationTask;
import com.atakmap.android.rubbersheet.data.create.CreateRubberModelTask;
import com.atakmap.android.user.PlacePointTool;
import com.atakmap.android.vehicle.model.VehicleModel;
import com.atakmap.android.vehicle.model.VehicleModelInfo;
import com.atakmap.commoncommo.CoTSendMethod;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.comms.CotDispatcher;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.Model;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import gov.tak.platform.graphics.Color;

public class SARUSPluginDropDownReceiver extends DropDownReceiver implements
        OnStateListener {
    public static final String TAG = SARUSPluginDropDownReceiver.class.getSimpleName();

    public static final String SHOW_PLUGIN = "com.atakmap.android.sarus.SHOW_PLUGIN";
    private final View SARUS_View;

    private final MapView mapView;

    private final TextView unitNameText, unitStatusText, unitBatteryText, debugText;

    private final EditText in_gps, in_gps_model, in_url;

    private final LinearLayout layout_button, layout_3D, layout_LiveFeed, layout_MissionControl, layout_Unit;

    private final Vector<LinearLayout> layouts = new Vector<>();

    private final File SARUSDownloadDirectory;

    private final GeoFenceManager geoFenceManager;

    private final MapGroup rootGroup;

    private String uuid;

    private Marker myMarker;

    private final CotDispatcher externalDispatcher, internalDispatcher, myDispatcher;

    private static final int RED = 1979645952;

    private static final int GREEN = 1962999552;

    private static final String CIRCLE = "u-d-c-c";

    private static final String POLYLINE = "u-d-f";

    private static final String RECTANGLE = "u-d-r";

    private static final String SERVER_NAME = "Pionner3-AT";

    private static final String CALLSING = "Pioneer_3-AT";

    private static final String UNIT_NAME = "Unit Name: ";

    private static final String UNIT_STATUS = "Unit Status: ";

    private static final String UNIT_BATTERY = "Unit Battery: ";

    /**************************** CONSTRUCTOR *****************************/

    public SARUSPluginDropDownReceiver(final MapView mapView, final Context context) {
        super(mapView);     
        this.mapView = mapView;
        context.setTheme(R.style.ATAKPluginTheme);
        SARUS_View = PluginLayoutInflater.inflate(context, R.layout.main_layout, null);
        SARUSDownloadDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), context.getString(R.string.downloadPath));
        geoFenceManager = new GeoFenceManager(new GeoFenceComponent(),mapView);
        rootGroup = mapView.getRootGroup();
        externalDispatcher = CotMapComponent.getExternalDispatcher();
        internalDispatcher = CotMapComponent.getInternalDispatcher();
        myDispatcher = new CotDispatcher();

        //Layout ans Text References
        layout_button = SARUS_View.findViewById(R.id.layout_Buttons);
        layout_3D = SARUS_View.findViewById(R.id.layout_3D);
        layout_LiveFeed = SARUS_View.findViewById(R.id.layout_LiveFeed);
        layout_MissionControl = SARUS_View.findViewById(R.id.layout_MissionControl);
        layout_Unit = SARUS_View.findViewById(R.id.layout_Unit);
        debugText = SARUS_View.findViewById(R.id.debugText);
        in_gps = SARUS_View.findViewById(R.id.in_GPS);
        in_gps_model = SARUS_View.findViewById(R.id.in_GPS_Model);
        in_url = SARUS_View.findViewById(R.id.in_URL);
        unitNameText = SARUS_View.findViewById(R.id.unit_name);
        unitStatusText = SARUS_View.findViewById(R.id.status);
        unitBatteryText = SARUS_View.findViewById(R.id.battery);

        //Button Listeners
        SARUS_View.findViewById(R.id.btn_marker).setOnClickListener(v -> createMarker());
        SARUS_View.findViewById(R.id.btn_https).setOnClickListener(v -> create3DModel());
        SARUS_View.findViewById(R.id.btn_button).setOnClickListener(v -> selectLayout(layout_button));
        SARUS_View.findViewById(R.id.btn_3D).setOnClickListener(v -> selectLayout(layout_3D));
        SARUS_View.findViewById(R.id.btn_livefeed).setOnClickListener(v -> selectLayout(layout_LiveFeed));
        SARUS_View.findViewById(R.id.btn_MissionControl).setOnClickListener(v -> selectLayout(layout_MissionControl));
        SARUS_View.findViewById(R.id.btn_Unit).setOnClickListener(v -> selectLayout(layout_Unit));
        SARUS_View.findViewById(R.id.btn_geoFence).setOnClickListener(v -> sendGeoFenceCoT());
        SARUS_View.findViewById(R.id.btn_waypoint).setOnClickListener(v -> sendWaypoint());
        SARUS_View.findViewById(R.id.btn_home).setOnClickListener(v -> sendCommand("ReturnHome"));
        SARUS_View.findViewById(R.id.btn_start).setOnClickListener(v -> sendCommand("Start"));
        SARUS_View.findViewById(R.id.btn_enable).setOnClickListener(v -> sendCommand("Enable"));

        //Layouts
        layouts.add(layout_button);
        layouts.add(layout_3D);
        layouts.add(layout_LiveFeed);
        layouts.add(layout_MissionControl);
        layouts.add(layout_Unit);
        selectLayout(layout_MissionControl);

        //CoTListener
        CotDetailManager.getInstance().registerHandler(new CotDetailHandler("sarus") {
               @Override
               public CommsMapComponent.ImportResult toItemMetadata(MapItem mapItem, CotEvent cotEvent, CotDetail cotDetail) {
                   toast("CoT Received");
                   setUnitName(cotDetail.getElementName());
                   setUnitStatus(cotDetail.getAttribute("Action"));
                   setUnitBattery(String.valueOf(cotDetail.childCount()));
                   return null;
               }

               @Override
               public boolean toCotDetail(MapItem mapItem, CotEvent cotEvent, CotDetail cotDetail) {
                   toast("CoT Sent");
                   return true;
               }
           }
        );
    }

    /**************************** PRIVATE METHODS *****************************/

    private void toast(String message) {
        Toast.makeText(getMapView().getContext(), message, Toast.LENGTH_LONG).show();
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
        return gps.getLatitude() + ", " + gps.getLongitude();
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
        String mapItemUuid = geoFence.getMapItemUid();
        MapItem mapItem = rootGroup.deepFindUID(mapItemUuid);

        if(mapItem == null) {
            return "Error, could not parse GeoFence, mapItem is null";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(mapItemUuid).append(",");
        builder.append(CoordinatedTime.currentTimeMillis()).append(",");

        Shape mapItemShape = (Shape) mapItem;

        if(mapItemShape == null) {
            return "Error, could not parse GeoFence, mapItem is null";
        }

        if(mapItem instanceof DrawingCircle)
        {
            DrawingCircle circle = (DrawingCircle) mapItem;
            builder.append(extractGeoFenceType(mapItemShape))
                    .append(',')
                    .append("CIRCLE")
                    .append(',')
                    .append(extractCoords(mapItemShape.getCenter().get()))
                    .append(',')
                    .append(circle.getRadius());
        }
        else if(mapItem instanceof DrawingShape || mapItemShape.getType().equals(RECTANGLE) || mapItemShape.getType().equals(POLYLINE))
        {
            builder.append(extractGeoFenceType(mapItemShape))
                    .append(',')
                    .append("POLYGON")
                    .append(extractCoords(mapItemShape.getPoints()));
        }
        else toast("GeoFence is undefined");

        return builder.toString();
    }

    private void sendCoT(CotDetail cotDetail)
    {
        Marker marker = createMarker();
        CotEvent cotEvent = createDefaultEvent(marker);
        CotDetail detail = cotEvent.getDetail();
        detail.addChild(cotDetail);
        CotDetailManager.getInstance().addDetails(marker, cotEvent);

        //Hardcoded Target UID for now, in the futur it will be editable by operator
        String[] uids = {"ANDROID-af5106f35612f980"};

        Bundle bundle = new Bundle();
        bundle.putStringArray("toUIDs", uids);

        myDispatcher.dispatchToBroadcast(cotEvent, CoTSendMethod.ANY);

        externalDispatcher.dispatch(cotEvent, bundle);
        internalDispatcher.dispatch(cotEvent, bundle);
    }

    private CotDetail getSarusDetail(String eventType)
    {
        CotDetail cotDetail = new CotDetail("sarus");
        cotDetail.setAttribute("EventType", eventType);
        cotDetail.setAttribute("Name", SERVER_NAME);
        cotDetail.setAttribute("Mission", "Find Waldo");
        cotDetail.setAttribute("EventID", UUID.randomUUID().toString());
        cotDetail.setAttribute("TimeStamp", String.valueOf(CoordinatedTime.currentTimeMillis()));

        return cotDetail;
    }

    private void selectLayout(LinearLayout selected_layout){
        for(LinearLayout layout : layouts){
            if(layout.getId() == selected_layout.getId()){
                layout.setVisibility(View.VISIBLE);
            } else {
                layout.setVisibility(View.GONE);
            }
        }
    }

    private void setUnitName(String unitName)
    {
        String name = UNIT_NAME + unitName;
        unitNameText.setText(name);
    }

    private void setUnitStatus(String unitStatus)
    {
        String status = UNIT_STATUS + unitStatus;
        unitStatusText.setText(status);
    }

    private void setUnitBattery(String unitBattery)
    {
        String battery = UNIT_BATTERY + unitBattery;
        unitBatteryText.setText(battery);
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
    }

    public void create3DModel(){
        String filename = in_url.getText().toString();
        File file = new File(SARUSDownloadDirectory, filename);

        GeoPoint coords = extractGeoPoint(in_gps_model.getText().toString());
        GeoPointMetaData metaGPS = new GeoPointMetaData();
        metaGPS.set(coords);

        VehicleModelInfo vehicleModelInfo = new VehicleModelInfo(null, filename, file);
        VehicleModel vehicleModel = new VehicleModel(vehicleModelInfo, metaGPS, UUID.randomUUID().toString());

        VehicleModelData rubberModelData = new VehicleModelData(vehicleModelInfo, metaGPS, vehicleModel.getUID());

        AbstractCreationTask.Callback callback = (abstractCreationTask, list) -> toast("Model Loaded");

        new CreateRubberModelTask(mapView, rubberModelData, false, callback).execute();
    }

    public Marker createMarker() {
        PlacePointTool.MarkerCreator mc;

        if(!in_gps.getText().toString().isEmpty()){
            GeoPoint coords = extractGeoPoint(in_gps.getText().toString());
            GeoPointMetaData metaGPS = new GeoPointMetaData();
            metaGPS.set(coords);
            mc = new PlacePointTool.MarkerCreator(metaGPS);
        }
        else{
            mc = new PlacePointTool.MarkerCreator(getMapView().getPointWithElevation());
        }

        uuid = UUID.randomUUID().toString();
        mc.setUid(uuid);
        mc.setCallsign(CALLSING);
        mc.setType("a-f-A");
        mc.showCotDetails(true);
        mc.setNeverPersist(true);
        myMarker = mc.placePoint();

        myMarker.setStyle(myMarker.getStyle()
                | Marker.STYLE_ROTATE_HEADING_MASK
                | Marker.STYLE_ROTATE_HEADING_NOARROW_MASK);

        myMarker.setTrack(310, 20);
        myMarker.setMetaInteger("color", Color.YELLOW);
        myMarker.refresh(getMapView().getMapEventDispatcher(), null, this.getClass());

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

    public void updateMarker(/*String markerName, long latitude, long longitude*/)
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

        int staleSeconds = 31536000;
        if (marker.hasMetaValue("cotDefaultStaleSeconds")) {
            staleSeconds = marker.getMetaInteger("cotDefaultStaleSeconds", staleSeconds);
        }

        CoordinatedTime time = new CoordinatedTime();
        cotEvent.setTime(time);
        cotEvent.setStart(time);
        cotEvent.setStale(time.addSeconds(staleSeconds));

        CotDetail detail = new CotDetail("detail");
        cotEvent.setDetail(detail);

        return cotEvent;
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

    public void sendWaypoint()
    {
        CotDetail sarusDetail = getSarusDetail("Waypoint");
        CotDetail waypointDetail = new CotDetail("Waypoint");
        Marker waypoint = createWaypoint();

        waypointDetail.setAttribute("ID", waypoint.getUID().toString());
        waypointDetail.setAttribute("Coordinates", extractCoords(waypoint.getPoint()));
        waypointDetail.setAttribute("lat", String.valueOf(waypoint.getPoint().getLatitude()));
        waypointDetail.setAttribute("lon", String.valueOf(waypoint.getPoint().getLongitude()));
        waypointDetail.setAttribute("alt", String.valueOf(waypoint.getPoint().getAltitude()));
        sarusDetail.addChild(waypointDetail);

        sendCoT(sarusDetail);
    }

    public void sendCommand(String command)
    {
        CotDetail sarusDetail = getSarusDetail("Command");

        CotDetail commandDetail = new CotDetail("Command");
        commandDetail.setAttribute("Action", command);
        sarusDetail.addChild(commandDetail);

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