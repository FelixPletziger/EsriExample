package com.example.felix.esriexample;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.ogc.WMSLayer;
import com.esri.core.ags.FeatureServiceInfo;
import com.esri.core.geodatabase.Geodatabase;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.Feature;
import com.esri.core.tasks.geodatabase.GenerateGeodatabaseParameters;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusCallback;
import com.esri.core.tasks.geodatabase.GeodatabaseStatusInfo;
import com.esri.core.tasks.geodatabase.GeodatabaseSyncTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    static ArcGISLocalTiledLayer dgkLayer;
    public int dgkTransStatus = 0;

    /************************************************************************************************************/
    String fLayerUrl;
    String fServiceUrl;
    private static File demoDataFile;
    private static String offlineDataSDCardDirName;
    private static String filename;
    static ProgressDialog mProgressDialog;
    private static Context mContext;
    protected static String OFFLINE_FILE_EXTENSION = ".geodatabase";
    protected static final String TAG = "CRGdb";
    static GeodatabaseSyncTask gdbSyncTask;
    static String localGdbFilePath;
    /*******************************************************************************************/
    private Toast toast;
    public static Callout callout;

    // The MapView.
    static MapView mMapView;
    ArcGISFeatureLayer onlineFeatureLayer;
    WMSLayer mWMSLayer;
    GraphicsLayer mGraphicsLayer;

    private Geodatabase geodatabase = null;
    private GeodatabaseFeatureTable geodatabaseFeatureTable;
    private FeatureLayer offlineFeatureLayer;

    String mFeatureServiceURL;
    String WMSURL;

    // The basemap switching menu items.
    MenuItem mStreetsMenuItem = null;
    MenuItem mTopoMenuItem = null;
    MenuItem mGrayMenuItem = null;
    MenuItem mOceansMenuItem = null;
    MenuItem mDGKMenuItem = null;

    //Layer Menu Items
    MenuItem LayerMenuItem1 = null;
    MenuItem LayerMenuItem2 = null;
    MenuItem LayerMenuItem3 = null;

    // Create MapOptions for each type of basemap.
    final MapOptions mTopoBasemap = new MapOptions(MapOptions.MapType.TOPO);
    final MapOptions mStreetsBasemap = new MapOptions(MapOptions.MapType.STREETS);
    final MapOptions mGrayBasemap = new MapOptions(MapOptions.MapType.GRAY);
    final MapOptions mOceansBasemap = new MapOptions(MapOptions.MapType.OCEANS);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /*
        try {
            Geodatabase geodatabase2 = new Geodatabase(createGeodatabaseFilePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        GeodatabaseFeatureTable geodatabaseFeatureTable2 =  geodatabase.getGeodatabaseFeatureTableByLayerId(0);
        FeatureLayer offlineFeatureLayer2 = new FeatureLayer(geodatabaseFeatureTable);
        mMapView.addLayer(offlineFeatureLayer);
        */

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /************************************************************************************************************/
        // set app context so it can be obtained to update progress
        MainActivity.setContext(this);

        // get sdcard resource names
        demoDataFile = Environment.getExternalStorageDirectory();
        offlineDataSDCardDirName = this.getResources().getString(R.string.config_data_sdcard_offline_dir);
        filename = this.getResources().getString(R.string.config_geodatabase_name);
        /*******************************************************************************************/

        // Retrieve the map and initial extent from XML layout
        mMapView = (MapView) findViewById(R.id.map);

        /**********************************************************************************************/
        // create service layer
        fServiceUrl = this.getResources()
                .getString(R.string.featureservice_url);

        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setTitle("Create local runtime geodatabase");
        /**********************************************************************************************/


        // Get the feature service URL from values->strings.xml
        mFeatureServiceURL = this.getResources().getString(R.string.featureServiceURL);

        // Add Feature layer to the MapView
        onlineFeatureLayer = new ArcGISFeatureLayer(mFeatureServiceURL, ArcGISFeatureLayer.MODE.ONDEMAND);
        mMapView.addLayer(onlineFeatureLayer);
        mMapView.getLayer(1).setVisible(false);

        //Add WMS to the Map
        WMSURL = this.getResources().getString(R.string.WMSURL);
        mWMSLayer = new WMSLayer(WMSURL);
        String[] visibleLayers = {"nw_dop40"};
        mWMSLayer.setVisibleLayer(visibleLayers);
        mMapView.addLayer(mWMSLayer);
        mMapView.getLayer(2).setVisible(false);

        // Add Graphics layer to the MapView
        mGraphicsLayer = new GraphicsLayer();
        mMapView.addLayer(mGraphicsLayer);

        //open the local geodatabase file
        try {
            geodatabase = new Geodatabase(createGeodatabaseFilePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Log.e("tet", String.valueOf(geodatabase.getGeodatabaseTables()));
        //open the layer in the geodatabase created from the service layer specified in 'LAYER_ID'
        geodatabaseFeatureTable =  geodatabase.getGeodatabaseFeatureTableByLayerId(0);

        //create a feature layer and add it to the map
        offlineFeatureLayer = new FeatureLayer(geodatabaseFeatureTable);
        mMapView.addLayer(offlineFeatureLayer);
        mMapView.getLayer(4).setVisible(false);

        dgkLayer = new ArcGISLocalTiledLayer(getFilePath("dgk5.tpk"));
        dgkLayer.setVisible(false);
        mMapView.addLayer(dgkLayer);

        // Enable map to wrap around date line.
        mMapView.enableWrapAround(true);
        mMapView.setEsriLogoVisible(false);

        mMapView.setOnSingleTapListener(new OnSingleTapListener() {

            private static final long serialVersionUID = 1L;

            @Override
            public void onSingleTap(float x, float y) {
                long[] selectedFeatures = offlineFeatureLayer.getFeatureIDs(x, y, 25, 1);

                if (selectedFeatures.length > 0) {

                    // Feature wird gew�hlt
                    offlineFeatureLayer.selectFeatures(selectedFeatures, false);

                    if (selectedFeatures != null && selectedFeatures.length > 0){

                        callout = mMapView.getCallout();

                        //Style
                        callout.setStyle(R.xml.pktpop);
                        Feature fpktNr = offlineFeatureLayer.getFeature(selectedFeatures[0]);
                        String name = fpktNr.getAttributeValue("Name").toString();
                        String plz = fpktNr.getAttributeValue("PLZ").toString();
                        String ort = fpktNr.getAttributeValue("Ort").toString();
                        String art = fpktNr.getAttributeValue("Art").toString();
                        callout.setContent(loadView(name,plz,ort, art));
                        callout.setMaxHeight(240);
                        callout.setMaxWidth(600);
                        callout.show((Point) fpktNr.getGeometry());


                    }else{
                        if(callout!=null && callout.isShowing()){
                            callout.hide();
                        }
                    }

                } else {
                    // Wenn kein Punkt getroffen wurde wird nachfolgende Meldung
                    // gezeigt
                    toast = Toast.makeText(getApplicationContext(),
                            "Kein Punkt ausgew�hlt", Toast.LENGTH_SHORT);
                    toast.show();
                    callout.hide();

                    // Selektion aufheben
                    offlineFeatureLayer.clearSelection();
                }
            }

            private View loadView(String name, String plz,
                                  String ort, String art) {
                View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.pktinfo, null);
                final TextView textNummer = (TextView) view.findViewById(R.id.pktnummer);
                textNummer.setText(" Name: "+name+"\n Art: "+art+"\n PLZ: "+plz+"\n Ort: "+ort);
                return view;
            }

        });

    }

    /***********************************************************************************************/
    // methods to ensure context is available when updating the progress dialog
    public static Context getContext(){
        return mContext;
    }

    public static void setContext(Context context){
        mContext = context;
    }

    /*
     * Create the geodatabase file location and name structure
     */
    static String createGeodatabaseFilePath() {
        return demoDataFile.getAbsolutePath() + File.separator + offlineDataSDCardDirName + File.separator + filename + OFFLINE_FILE_EXTENSION;
    }
    /***********************************************************************************************/

    protected void onPause() {
        super.onPause();
        mMapView.pause();
    }

    protected void onResume() {
        super.onResume();
        mMapView.unpause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // Get the basemap switching menu items.
        mStreetsMenuItem = menu.getItem(1).getSubMenu().getItem(0);
        mTopoMenuItem = menu.getItem(1).getSubMenu().getItem(1);
        mGrayMenuItem = menu.getItem(1).getSubMenu().getItem(2);
        mOceansMenuItem = menu.getItem(1).getSubMenu().getItem(3);
        mDGKMenuItem = menu.getItem(1).getSubMenu().getItem(4);

        // Also set the topo basemap menu item to be checked, as this is the default.
        mTopoMenuItem.setChecked(true);

        //LayerMenu
        LayerMenuItem1 = menu.getItem(2).getSubMenu().getItem(0);
        LayerMenuItem2 = menu.getItem(2).getSubMenu().getItem(1);
        LayerMenuItem3 = menu.getItem(2).getSubMenu().getItem(2);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle menu item selection.
        switch (item.getItemId()) {
            case R.id.World_Street_Map:
                mMapView.setMapOptions(mStreetsBasemap);
                mStreetsMenuItem.setChecked(true);
                return true;
            case R.id.World_Topo:
                mMapView.setMapOptions(mTopoBasemap);
                mTopoMenuItem.setChecked(true);
                return true;
            case R.id.Gray:
                mMapView.setMapOptions(mGrayBasemap);
                mGrayMenuItem.setChecked(true);
                return true;
            case R.id.Ocean_Basemap:
                mMapView.setMapOptions(mOceansBasemap);
                mOceansMenuItem.setChecked(true);
                return true;
            case R.id.onlinefeaturelayer:
                if (LayerMenuItem1.isChecked()) {
                    mMapView.getLayer(1).setVisible(false);
                    LayerMenuItem1.setChecked(false);
                } else {
                    mMapView.getLayer(1).setVisible(true);
                    LayerMenuItem1.setChecked(true);
                }
                return true;
            case R.id.offlinefeaturelayer:
                if (LayerMenuItem3.isChecked()) {
                    mMapView.getLayer(4).setVisible(false);
                    LayerMenuItem3.setChecked(false);
                } else {
                    mMapView.getLayer(4).setVisible(true);
                    LayerMenuItem3.setChecked(true);
                }
                return true;
            case R.id.wmslayer1:
                if (LayerMenuItem2.isChecked()) {
                    mMapView.getLayer(2).setVisible(false);
                    LayerMenuItem2.setChecked(false);
                } else {
                    mMapView.getLayer(2).setVisible(true);
                    LayerMenuItem2.setChecked(true);
                }
                return true;
            case R.id.DGK5:
                if (mDGKMenuItem.isChecked()) {
                    mMapView.getLayer(5).setVisible(false);
                    mDGKMenuItem.setChecked(false);
                }else{
                    mMapView.getLayer(5).setVisible(true);
                    mDGKMenuItem.setChecked(true);
                }
                return true;
            case R.id.action_download:
                downloadData(fServiceUrl);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    /**********************************************************************************************/
    /**
     * Create the GeodatabaseTask from the feature service URL w/o credentials.
     */
    private void downloadData(String url) {
        Log.i(TAG, "Create GeoDatabase");
        // create a dialog to update user on progress
        mProgressDialog.show();
        // create the GeodatabaseTask

        gdbSyncTask = new GeodatabaseSyncTask(url, null);
        gdbSyncTask.fetchFeatureServiceInfo(new CallbackListener<FeatureServiceInfo>() {

                    @Override
                    public void onError(Throwable arg0) {
                        Log.e(TAG, "Error fetching FeatureServiceInfo");
                    }

                    @Override
                    public void onCallback(FeatureServiceInfo fsInfo) {
                        if (fsInfo.isSyncEnabled()) {
                            createGeodatabase(fsInfo);
                        }
                    }
                });

    }

    /**
     * Set up parameters to pass the the  method. A
     * {@link CallbackListener} is used for the response.
     */
    private static void createGeodatabase(FeatureServiceInfo featureServerInfo) {
        // set up the parameters to generate a geodatabase
        GenerateGeodatabaseParameters params = new GenerateGeodatabaseParameters(
                featureServerInfo, mMapView.getExtent(),
                mMapView.getSpatialReference());

        // a callback which fires when the task has completed or failed.
        CallbackListener<String> gdbResponseCallback = new CallbackListener<String>() {
            @Override
            public void onError(final Throwable e) {
                Log.e(TAG, "Error creating geodatabase");
                mProgressDialog.dismiss();
            }

            @Override
            public void onCallback(String path) {
                Log.i(TAG, "Geodatabase is: " + path);
                mProgressDialog.dismiss();
                // update map with local feature layer from geodatabase
                updateFeatureLayer(path);
                // log the path to the data on device
                Log.i(TAG, "path to geodatabase: " + path);
            }
        };

        // a callback which updates when the status of the task changes
        GeodatabaseStatusCallback statusCallback = new GeodatabaseStatusCallback() {
            @Override
            public void statusUpdated(final GeodatabaseStatusInfo status) {
                // get current status
                String progress = status.getStatus().toString();
                // get activity context
                Context context = MainActivity.getContext();
                // create activity from context
                MainActivity activity = (MainActivity) context;
                // update progress bar on main thread
                showProgressBar(activity, progress);

            }
        };

        // create the fully qualified path for geodatabase file
        localGdbFilePath = createGeodatabaseFilePath();

        // get geodatabase based on params
        submitTask(params, localGdbFilePath, statusCallback,
                gdbResponseCallback);
    }

    /**
     * Request database, poll server to get status, and download the file
     */
    private static void submitTask(GenerateGeodatabaseParameters params,
                                   String file, GeodatabaseStatusCallback statusCallback,
                                   CallbackListener<String> gdbResponseCallback) {
        // submit task
        gdbSyncTask.generateGeodatabase(params, file, false, statusCallback,
                gdbResponseCallback);
    }

    /**
     * Add feature layer from local geodatabase to map
     *
     * @param featureLayerPath
     */
    private static void updateFeatureLayer(String featureLayerPath) {
        // create a new geodatabase
        Geodatabase localGdb = null;
        try {
            localGdb = new Geodatabase(featureLayerPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Geodatabase contains GdbFeatureTables representing attribute data
        // and/or spatial data. If GdbFeatureTable has geometry add it to
        // the MapView as a Feature Layer
        if (localGdb != null) {
            for (GeodatabaseFeatureTable gdbFeatureTable : localGdb
                    .getGeodatabaseTables()) {
                if (gdbFeatureTable.hasGeometry()){
                    mMapView.addLayer(new FeatureLayer(gdbFeatureTable));

                }
            }
        }
    }

    private static void showProgressBar(final MainActivity activity, final String message){
        activity.runOnUiThread(new Runnable(){

            @Override
            public void run() {
                mProgressDialog.setMessage(message);
            }

        });
    }

    /**********************************************************************************************/

    // Diese Methode liefert den Pfad zu Geodatabase im Asset Ordner
    String getFilePath(String fileName) {
        File f = new File(getCacheDir() + fileName);
        if (!f.exists())
            try {

                InputStream is = getAssets().open(fileName);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();

                FileOutputStream fos = new FileOutputStream(f);
                fos.write(buffer);
                fos.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        return f.getPath();
    }
}
