package com.web_temple.mapbox.mapboxtest;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.mapboxsdk.MapboxAccountManager;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.offline.OfflineManager;
import com.mapbox.mapboxsdk.offline.OfflineRegion;
import com.mapbox.mapboxsdk.offline.OfflineRegionError;
import com.mapbox.mapboxsdk.offline.OfflineRegionStatus;
import com.mapbox.mapboxsdk.offline.OfflineTilePyramidRegionDefinition;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    // JSON encoding/decoding
    public final static String JSON_CHARSET = "UTF-8";
    public final static String JSON_FIELD_REGION_NAME = "FIELD_REGION_NAME";

    // UI elements
    private MapView mapView;
    private MapboxMap mapboxMap;
    private ProgressBar progressBar;

    private boolean isEndNotified = true;
    private int regionSelected = -1;

    // Offline objects
    private OfflineManager offlineManager;
    private OfflineRegion offlineRegion;

    private String styleUrl = "mapbox://styles/roderich/cimudkmdr00878skowh3rfhak" ; // cirvhj46r001fgum6z05cbwjq

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapboxAccountManager.start(this, getResources().getString(R.string.access_token));

        mapView = (MapView) findViewById(R.id.mapView);

        if(null != mapView) {

            mapView.onCreate(savedInstanceState);

            mapView.getMapAsync(this) ;

        }

        // Assign progressBar for later use
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        // Set up the offlineManager
        offlineManager = OfflineManager.getInstance(this);

    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {

        this.mapboxMap = mapboxMap;

        try {

            Log.d(TAG, "Style: " + styleUrl) ;

            mapView.setStyleUrl(styleUrl);

        } catch(Exception e) {

            Log.d(TAG, "Map Style URL exception: " + e.getMessage());

            e.printStackTrace();

        }

    }

    @Override
    public void onResume() {

        super.onResume();

        mapView.onResume();

    }

    @Override
    public void onPause() {

        super.onPause();

        mapView.onPause();

    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        mapView.onDestroy();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        mapView.onSaveInstanceState(outState);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.mapstyle_menu, menu);

        menu.findItem(R.id.action_default).setVisible(!styleUrl.equals(Style.MAPBOX_STREETS)) ;

        //Log.d(MainActivity.LOG_TAG, "Menu: " + menu) ;

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {

        MenuItem item = menu.findItem(R.id.action_save_offline);

        if (isEndNotified) {

            item.setEnabled(true);

            item.getIcon().setAlpha(255);

        } else {

            // disabled

            item.setEnabled(false);

            item.getIcon().setAlpha(130);

        }

        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        String style = null ;

        switch(id) {

            case R.id.action_default:

                style = styleUrl;

                break;

            case R.id.action_dark:

                style = Style.DARK;

                break;

            case R.id.action_outdoors:

                style = Style.OUTDOORS;

                break;

            case R.id.action_light:

                style = Style.LIGHT;

                break;

            case R.id.action_mapbox_streets:

                style = Style.MAPBOX_STREETS;

                break;

            case R.id.action_satellite:

                style = Style.SATELLITE;

                break;

            case R.id.action_satellite_streets:

                style = Style.SATELLITE_STREETS;

                break;

            case R.id.action_save_offline:

                //Toast.makeText(this, item.getTitle() + " is pressed", Toast.LENGTH_LONG).show();

                downloadRegionDialog();

                break;

            case android.R.id.home:

                // app icon in action bar clicked; goto parent activity.
                onBackPressed();

                return true;

        }

        if(null != style) {

            try {

                Log.d(TAG, "Style: " + style) ;

                mapView.setStyleUrl(style);

                Toast.makeText(this, item.getTitle() + " style is loaded", Toast.LENGTH_LONG).show();

            } catch(Exception e) {

                Log.d(TAG, "Map Style URL exception: " + e.getMessage());

                e.printStackTrace();

                //mapView.setStyleUrl(Style.MAPBOX_STREETS);

            }

            return true;

        } else {

            return super.onOptionsItemSelected(item);

        }

    }

    // Progress bar methods
    private void startProgress() {

        // Start and show the progress bar
        isEndNotified = false;

        progressBar.setIndeterminate(true);
        progressBar.setVisibility(View.VISIBLE);

        invalidateOptionsMenu();

    }

    private void setPercentage(final int percentage) {

        progressBar.setIndeterminate(false);
        progressBar.setProgress(percentage);

    }

    private void endProgress(final String message) {
        // Don't notify more than once
        if (isEndNotified) return;

        // Stop and hide the progress bar
        isEndNotified = true;

        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.GONE);

        invalidateOptionsMenu();

        // Show a toast
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

    }

    private void downloadRegion(final String regionName) {

        // Define offline region parameters, including bounds,
        // min/max zoom, and metadata

        // Start the progressBar

        startProgress();

        // Create offline definition using the current
        // style and boundaries of visible map area

        String styleURL = mapboxMap.getStyleUrl();

        LatLngBounds bounds = mapboxMap.getProjection().getVisibleRegion().latLngBounds;

        double minZoom = mapboxMap.getCameraPosition().zoom;
        double maxZoom = mapboxMap.getMaxZoom();

        float pixelRatio = this.getResources().getDisplayMetrics().density;

        OfflineTilePyramidRegionDefinition definition = new OfflineTilePyramidRegionDefinition(
                styleURL, bounds, minZoom, maxZoom, pixelRatio
        );

        // Build a JSONObject using the user-defined offline region title,
        // convert it into string, and use it to create a metadata variable.
        // The metadata varaible will later be passed to createOfflineRegion()
        byte[] metadata;

        try {

            JSONObject jsonObject = new JSONObject();

            jsonObject.put(JSON_FIELD_REGION_NAME, regionName);

            String json = jsonObject.toString();
            metadata = json.getBytes(JSON_CHARSET);

        } catch (Exception e) {

            Log.e(TAG, "Failed to encode metadata: " + e.getMessage());

            metadata = new byte[0];

        }

        // Create the offline region and launch the download
        offlineManager.createOfflineRegion(definition, metadata, new OfflineManager.CreateOfflineRegionCallback() {

            @Override
            public void onCreate(OfflineRegion offlineRegion) {

                Log.d(TAG, "Offline region created: " + regionName);

                MainActivity.this.offlineRegion = offlineRegion;

                launchDownload();

            }

            @Override
            public void onError(String error) {

                Log.e(TAG, "Error: " + error);

            }

        });

    }

    private void launchDownload() {

        // Set up an observer to handle download progress and
        // notify the user when the region is finished downloading

        offlineRegion.setObserver(new OfflineRegion.OfflineRegionObserver() {

            @Override
            public void onStatusChanged(OfflineRegionStatus status) {

                // Compute a percentage
                double percentage = status.getRequiredResourceCount() >= 0 ?
                        (100.0 * status.getCompletedResourceCount() / status.getRequiredResourceCount()) :
                        0.0;

                if (status.isComplete()) {

                    // Download complete

                    endProgress("Region downloaded successfully.");

                } else if (status.isRequiredResourceCountPrecise()) {

                    // Switch to determinate state

                    setPercentage((int) Math.round(percentage));

                }

                // Log what is being currently downloaded
                /*
                Log.d(MainActivity.LOG_TAG, String.format("%s/%s resources; %s bytes downloaded.",
                        String.valueOf(status.getCompletedResourceCount()),
                        String.valueOf(status.getRequiredResourceCount()),
                        String.valueOf(status.getCompletedResourceSize())));
                */

            }

            @Override
            public void onError(OfflineRegionError error) {

                Log.e(TAG, "onError reason: " + error.getReason());
                Log.e(TAG, "onError message: " + error.getMessage());

            }

            @Override
            public void mapboxTileCountLimitExceeded(long limit) {

                Log.e(TAG, "Mapbox tile count limit exceeded: " + limit);

            }

        });

        // Change the region state
        offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE);

    }

    private void downloadRegionDialog() {

        // Build a region list when the user clicks the list button

        // Query the DB asynchronously
        offlineManager.listOfflineRegions(new OfflineManager.ListOfflineRegionsCallback() {

            @Override
            public void onList(final OfflineRegion[] offlineRegions) {

                AlertDialog.Builder offlineDialogBuilder = new AlertDialog.Builder(MainActivity.this);

                offlineDialogBuilder.setTitle(MainActivity.this.getString(R.string.text_offline_use));

                View offline = getLayoutInflater().inflate(R.layout.mapbox_offline_dialog, new LinearLayout(MainActivity.this), false);

                final EditText newRegionText = (EditText) offline.findViewById(R.id.newRegionText) ;

                offlineDialogBuilder.setView(offline) ;

                offlineDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialogInterface, int i) {

                        // do nothing

                    }

                });

                LinearLayout regionsList = (LinearLayout) offline.findViewById(R.id.regionsList);

                if (offlineRegions == null || offlineRegions.length == 0) {

                    TextView tv = new TextView(MainActivity.this) ;

                    tv.setText(MainActivity.this.getText(R.string.text_no_regions_yet));

                    regionsList.addView(tv);

                } else {

                    final RadioGroup regionRadios = new RadioGroup(MainActivity.this);

                    // Add all of the region names to a list

                    for (int i = 0; i < offlineRegions.length; i++) {

                        RadioButton regionButton = new RadioButton(MainActivity.this) ;

                        regionButton.setText(getRegionName(offlineRegions[i]));

                        regionButton.setChecked(regionSelected == i);

                        regionButton.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {

                                if(regionSelected >= 0 && regionSelected < offlineRegions.length) {

                                    ((RadioButton) regionRadios.getChildAt(regionSelected)).setChecked(false);

                                }

                                regionSelected = regionRadios.indexOfChild(view);

                            }

                        });

                        regionRadios.addView(regionButton);

                    }

                    regionsList.addView(regionRadios);

                    offlineDialogBuilder.setPositiveButton("Navigate To", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialogInterface, int i) {

                            if(regionSelected >= 0 && regionSelected < offlineRegions.length) {

                                Toast.makeText(MainActivity.this, getRegionName(offlineRegions[regionSelected]), Toast.LENGTH_LONG).show();

                                // Get the region bounds and zoom
                                LatLngBounds bounds = ((OfflineTilePyramidRegionDefinition) offlineRegions[regionSelected].getDefinition()).getBounds();
                                double regionZoom = ((OfflineTilePyramidRegionDefinition) offlineRegions[regionSelected].getDefinition()).getMinZoom();

                                CameraPosition cameraPosition = new CameraPosition.Builder()
                                        .target(bounds.getCenter()) // Sets the new camera position
                                        .zoom(regionZoom) // Sets the zoom
                                        .bearing(180) // Rotate the camera
                                        .tilt(30) // Set the camera tilt
                                        .build(); // Creates a CameraPosition from the builder

                                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 7000);

                            }

                        }

                    });

                    offlineDialogBuilder.setNeutralButton("Delete", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialogInterface, int i) {

                            if(regionSelected >= 0 && regionSelected < offlineRegions.length) {

                                // Make progressBar indeterminate and
                                // set it to visible to signal that
                                // the deletion process has begun

                                progressBar.setIndeterminate(true);
                                progressBar.setVisibility(View.VISIBLE);

                                // Begin the deletion process
                                offlineRegions[regionSelected].delete(new OfflineRegion.OfflineRegionDeleteCallback() {

                                    @Override
                                    public void onDelete() {

                                        // Once the region is deleted, remove the
                                        // progressBar and display a toast
                                        progressBar.setVisibility(View.INVISIBLE);
                                        progressBar.setIndeterminate(false);

                                        Toast.makeText(MainActivity.this, "Region deleted", Toast.LENGTH_LONG).show();

                                        regionSelected = -1 ;

                                    }

                                    @Override
                                    public void onError(String error) {

                                        progressBar.setVisibility(View.INVISIBLE);
                                        progressBar.setIndeterminate(false);

                                        Log.e(TAG, "Error: " + error);

                                    }

                                });

                            }

                        }

                    });

                }

                final AlertDialog offlineDialog = offlineDialogBuilder.show() ;

                newRegionText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

                    @Override
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                        boolean handled = false;

                        if (actionId == EditorInfo.IME_ACTION_DONE) {

                            String regionName = v.getText().toString();

                            // Require a region name to begin the download.
                            // If the user-provided string is empty, display
                            // a toast message and do not begin download.
                            if (regionName.length() == 0) {

                                Toast.makeText(MainActivity.this, "Region name cannot be empty.", Toast.LENGTH_SHORT).show();

                            } else {

                                // Begin download process
                                downloadRegion(regionName);

                                offlineDialog.dismiss();

                            }

                            handled = true;

                        }

                        return handled;

                    }

                });

            }

            @Override
            public void onError(String error) {

                Log.e(TAG, "Error: " + error);

            }

        });

    }

    private String getRegionName(OfflineRegion offlineRegion) {

        // Get the region name from the offline region metadata
        String regionName;

        try {

            byte[] metadata = offlineRegion.getMetadata();

            String json = new String(metadata, JSON_CHARSET);

            JSONObject jsonObject = new JSONObject(json);
            regionName = jsonObject.getString(JSON_FIELD_REGION_NAME);

        } catch (Exception e) {

            Log.e(TAG, "Failed to decode metadata: " + e.getMessage());

            regionName = "Region " + offlineRegion.getID();

        }

        return regionName;

    }

}
