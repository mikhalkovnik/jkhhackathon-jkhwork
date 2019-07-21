package ru.lazybones.jkh.jkhwork;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapGesture;


import com.here.android.mpa.mapping.MapMarker;

import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.SupportMapFragment;


import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ru.lazybones.jkh.jkhwork.Current.preOrders;

public class MapActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private Map map = null;
    private DatabaseReference mydatabase;
    private ProgressBar progressBar;
    private ArrayList<Buildingscity> buildings;
    private ArrayList<PreOrder> preOrders;


    // map fragment embedded in this activity
    private SupportMapFragment mapFragment = null;
    /**
     * permissions request code
     */
    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    /**
     * Permissions that need to be explicitly requested from end user.
     */
    private static final String[] REQUIRED_SDK_PERMISSIONS = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mydatabase= FirebaseDatabase.getInstance().getReference();
        checkPermissions();


    }

    /**
     * Checks the dynamically-controlled permissions and requests missing permissions from end user.
     */
    protected void checkPermissions() {
        final List<String> missingPermissions = new ArrayList<String>();
        // check all required dynamic permissions
        for (final String permission : REQUIRED_SDK_PERMISSIONS) {
            final int result = ContextCompat.checkSelfPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        if (!missingPermissions.isEmpty()) {
            // request all missing permissions
            final String[] permissions = missingPermissions
                    .toArray(new String[missingPermissions.size()]);
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            final int[] grantResults = new int[REQUIRED_SDK_PERMISSIONS.length];
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED);
            onRequestPermissionsResult(REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                    grantResults);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                for (int index = permissions.length - 1; index >= 0; --index) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        // exit the app if one permission is not granted
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                }
                // all permissions were granted
                initialize();
                break;
        }
    }

    private void initialize() {
        setContentView(R.layout.activity_map);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(this);

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapfragment);

        // Set up disk cache path for the map service for this application
        // It is recommended to use a path under your application folder for storing the disk cache
        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                getApplicationContext().getExternalFilesDir(null) + File.separator + ".here-maps",
                "MapService"); /* ATTENTION! Do not forget to update {YOUR_INTENT_NAME} */

        if (!success) {
            Toast.makeText(getApplicationContext(), "Unable to set isolated disk cache path.", Toast.LENGTH_LONG);
        } else {
            mapFragment.init(new OnEngineInitListener() {
                @Override
                public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                    if (error == OnEngineInitListener.Error.NONE) {
                        // retrieve a reference of the map from the map fragment
                        map = mapFragment.getMap();
                        // Set the map center to the Vancouver region (no animation)
                        map.setCenter(new GeoCoordinate(56.296119, 43.978929, 0.0),
                                Map.Animation.NONE);
                        // Set the zoom level to the average between min and max
                        map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2+5);
                        updatedb();
                    } else {
                        System.out.println("ERROR: Cannot initialize Map Fragment");
                    }
                }
            });
        }


        progressBar = (ProgressBar) findViewById(R.id.progress_bar);


    }

    private void updatedb() {

        mydatabase.child("objects").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.VISIBLE);
                buildings = new ArrayList<>();

                if (dataSnapshot.exists()) {

                    for (DataSnapshot snapshot1 :
                            dataSnapshot.getChildren()) {

                        Buildingscity element = snapshot1.getValue(Buildingscity.class);
                        if (element!= null)

                            buildings.add(element);


                    }

                }
                progressBar.setVisibility(View.GONE);

                updatevepreorders();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void updatevepreorders() {

        mydatabase.child("preorders").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.VISIBLE);
                preOrders = new ArrayList<>();

                if (dataSnapshot.exists()) {

                    for (DataSnapshot snapshot1 :
                            dataSnapshot.getChildren()) {
                        for (DataSnapshot snapshot2 :
                                snapshot1.getChildren()) {

                            PreOrder element = snapshot2.getValue(PreOrder.class);
                            if (element != null)

                                preOrders.add(element);
                        }


                    }

                }
                progressBar.setVisibility(View.GONE);

                updatemap();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void updatemap() {

        for (PreOrder order: preOrders) {
            double lat=0.0;
            double lng=0.0;
            String orderbuildid= order.getObjectid();
         for (Buildingscity bcity: buildings) {
             String buildid = (String) bcity.getId() + "";

             if (buildid.equals(orderbuildid)) {
                 lat = bcity.getLat();
                 lng = bcity.getLng();
             }
         }
// Create a custom marker image
             com.here.android.mpa.common.Image myImage =
                     new com.here.android.mpa.common.Image();

             try {
                 myImage.setImageResource(R.drawable.infoicsmall);
             } catch (IOException e) {
                 finish();
             }
// Create the MapMarker
             MapMarker myMapMarker =   new MapMarker(new GeoCoordinate(lat, lng), myImage);
             map.addMapObject(myMapMarker);
// Create a gesture listener and add it to the SupportMapFragment
             MapGesture.OnGestureListener listener =
                     new MapGesture.OnGestureListener.OnGestureListenerAdapter() {
                         @Override
                         public boolean onMapObjectsSelected(List<ViewObject> objects) {
                             for (ViewObject viewObj : objects) {
                                 if (viewObj.getBaseType() == ViewObject.Type.USER_OBJECT) {
                                     if (((MapObject)viewObj).getType() == MapObject.Type.MARKER) {
                                         // At this point we have the originally added
                                         // map marker, so we can do something with it
                                         // (like change the visibility, or more
                                         // marker-specific actions)
                                         ((MapObject)viewObj).setVisible(false);
                                     }
                                 }
                             }
                             // return false to allow the map to handle this callback also
                             return false;
                         }

                     };
         }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_tools) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
