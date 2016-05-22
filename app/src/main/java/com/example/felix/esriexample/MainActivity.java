package com.example.felix.esriexample;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.MapOptions;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ogc.WMSLayer;
import com.esri.android.map.popup.PopupContainer;

import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private PopupContainer popupContainer;
    private ProgressDialog progressDialog;
    private AtomicInteger count;

    // The MapView.
    MapView mMapView;
    ArcGISFeatureLayer mFeatureLayer;
    WMSLayer mWMSLayer;
    GraphicsLayer mGraphicsLayer;

    String mFeatureServiceURL;
    String WMSURL;

    // The basemap switching menu items.
    MenuItem mStreetsMenuItem = null;
    MenuItem mTopoMenuItem = null;
    MenuItem mGrayMenuItem = null;
    MenuItem mOceansMenuItem = null;

    //Layer Menu Items
    MenuItem LayerMenuItem1 = null;
    MenuItem LayerMenuItem2 = null;

    // Create MapOptions for each type of basemap.
    final MapOptions mTopoBasemap = new MapOptions(MapOptions.MapType.TOPO);
    final MapOptions mStreetsBasemap = new MapOptions(MapOptions.MapType.STREETS);
    final MapOptions mGrayBasemap = new MapOptions(MapOptions.MapType.GRAY);
    final MapOptions mOceansBasemap = new MapOptions(MapOptions.MapType.OCEANS);



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Retrieve the map and initial extent from XML layout
        mMapView = (MapView) findViewById(R.id.map);

        // Get the feature service URL from values->strings.xml
        mFeatureServiceURL = this.getResources().getString(R.string.featureServiceURL);

        // Add Feature layer to the MapView
        mFeatureLayer = new ArcGISFeatureLayer(mFeatureServiceURL, ArcGISFeatureLayer.MODE.ONDEMAND);
        mMapView.addLayer(mFeatureLayer);
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

        // Enable map to wrap around date line.
        mMapView.enableWrapAround(true);
        mMapView.setEsriLogoVisible(false);


    }

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
        mStreetsMenuItem = menu.getItem(0).getSubMenu().getItem(0);
        mTopoMenuItem = menu.getItem(0).getSubMenu().getItem(1);
        mGrayMenuItem = menu.getItem(0).getSubMenu().getItem(2);
        mOceansMenuItem = menu.getItem(0).getSubMenu().getItem(3);

        // Also set the topo basemap menu item to be checked, as this is the default.
        mTopoMenuItem.setChecked(true);

        //LayerMenu
        LayerMenuItem1 = menu.getItem(1).getSubMenu().getItem(0);
        LayerMenuItem2 = menu.getItem(1).getSubMenu().getItem(1);

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
            case R.id.featurelayer1:
                if (LayerMenuItem1.isChecked()) {
                    mMapView.getLayer(1).setVisible(false);
                    LayerMenuItem1.setChecked(false);
                } else {
                    mMapView.getLayer(1).setVisible(true);
                    LayerMenuItem1.setChecked(true);
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
            default:
                return super.onOptionsItemSelected(item);

        }
    }
}
