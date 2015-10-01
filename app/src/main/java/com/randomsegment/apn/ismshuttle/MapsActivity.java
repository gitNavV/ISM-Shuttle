package com.randomsegment.apn.ismshuttle;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.StrictMode;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.kml.KmlLayer;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

public class MapsActivity extends ActionBarActivity {

    private GoogleMap mMap;
    private boolean moreThanOne = false;
    public LatLng prev = new LatLng(0, 0);
    public LatLng default_location = new LatLng(23.815717,86.441069);
    public int DEFAULT_ZOOM_LEVEL = 16;
    private boolean checkCancelAsyncTask = false;
    private Marker marker;
    private getMapData task;
    private final int DEFAULT_REFRESH_TIME = 2000;
    // Toast Variable

    private Toast mToast;

    // Google Ad
    private AdView mAdView;


    // Navigation + Recycler View
    //First We Declare Titles And Icons For Our Navigation Drawer List View
    //This Icons And Titles Are holded in an Array as you can see
    String TITLES[] = {"Route","Feedback","Setting","About"};
    int ICONS[] = {R.drawable.route_icon,R.drawable.feedback_icon1,R.drawable.setting_icon,R.drawable.about_icon};

    //Similarly we Create a String Resource for the name and email in the header view
    //And we also create a int resource for profile picture in the header view



    RecyclerView mRecyclerView;                           // Declaring RecyclerView
    RecyclerView.Adapter mAdapter;                        // Declaring Adapter For Recycler View
    RecyclerView.LayoutManager mLayoutManager;            // Declaring Layout Manager as a linear layout manager
    DrawerLayout Drawer;                                  // Declaring DrawerLayout

    ActionBarDrawerToggle mDrawerToggle;

    //Navigation Drawer Variables
    private AlertDialog.Builder dialogBuilder;
    private String strRoute = "",strFeedback = "",strEmail = "";
    int route = 0;
    int ref_time = 2000;
    Context context;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main_appbar);
        toolbar = (Toolbar) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //NavigationDrawerFragment navigationDrawerFragment =
        //        (NavigationDrawerFragment)getSupportFragmentManager()
        //                .findFragmentById(R.id.fragment_navigation_drawer);

        //navigationDrawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), toolbar);

        //New


        mRecyclerView = (RecyclerView) findViewById(R.id.RecyclerView); // Assigning the RecyclerView Object to the xml View

        mRecyclerView.setHasFixedSize(true);                            // Letting the system know that the list objects are of fixed size

        mAdapter = new MyAdapter(TITLES,ICONS,this);       // Creating the Adapter of MyAdapter class(which we are going to see in a bit)
        // And passing the titles,icons,header view name, header view email,
        // and header view profile picture

        mRecyclerView.setAdapter(mAdapter);                              // Setting the adapter to RecyclerView

        final GestureDetector mGestureDetector = new GestureDetector(MapsActivity.this, new GestureDetector.SimpleOnGestureListener() {

            @Override public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

        });

        mRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
                View child = recyclerView.findChildViewUnder(motionEvent.getX(),motionEvent.getY());



                if(child!=null && mGestureDetector.onTouchEvent(motionEvent)){
                    Drawer.closeDrawers();
                    Toast.makeText(MapsActivity.this, "The Item Clicked is: " + recyclerView.getChildPosition(child), Toast.LENGTH_SHORT).show();
                    if(recyclerView.getChildPosition(child) == 0)
                        route_map();
                    else if (recyclerView.getChildPosition(child) == 1)
                        feedback();
                    else if (recyclerView.getChildPosition(child) == 2)
                        setting();
                    else if (recyclerView.getChildPosition(child) == 3)
                        about();
                    return true;

                }

                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean b) {

            }
        });

        //Google Ads

        mAdView = (AdView) findViewById(R.id.adView);
        mAdView.setAdListener(new ToastAdListener(this));
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Save the refresh time
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        ref_time = preferences.getInt("refreshTime",ref_time);

        // Layout Manager
        mLayoutManager = new LinearLayoutManager(this);                 // Creating a layout Manager

        mRecyclerView.setLayoutManager(mLayoutManager);                 // Setting the layout Manager


        Drawer = (DrawerLayout) findViewById(R.id.drawer_layout);        // Drawer object Assigned to the view
        mDrawerToggle = new ActionBarDrawerToggle(this,Drawer,toolbar,R.string.drawer_open,R.string.drawer_close){

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                // code here will execute once the drawer is opened( As I dont want anything happened whe drawer is
                // open I am not going to put anything here)
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                // Code here will execute once drawer is closed
            }



        };
        // Drawer Toggle Object Made
        Drawer.setDrawerListener(mDrawerToggle); // Drawer Listener set to the Drawer toggle
        mDrawerToggle.syncState();               // Finally we set the drawer toggle sync State

        // Map part

        createKML(context);
        setUpMapIfNeeded();

        //Modify
        ref_time = getIntent().getIntExtra("refreshTime", DEFAULT_REFRESH_TIME);
        // Store
        //throw new RuntimeException(String.valueOf(ref_time));
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("refreshTime",ref_time);
        editor.apply();


        //Modify



    }



    @Override
    protected void onResume() {
        super.onResume();
        if (mAdView != null) {
            mAdView.resume();
        }
        checkCancelAsyncTask = false;
        setUpMapIfNeeded();
    }
    @Override
    protected void onStop(){
        if (task != null)
            task.cancel(true);
        checkCancelAsyncTask = true;
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (mAdView != null) {
            mAdView.destroy();
        }
        if (task != null)
            task.cancel(true);
        checkCancelAsyncTask = true;
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (mAdView != null) {
            mAdView.pause();
        }
        if (task != null)
            task.cancel(true);
        checkCancelAsyncTask = true;
        super.onPause();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #} once when {@link #mMap} is not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                onMapReady();
            }
        }
    }

    /**
     * To Add a KML Layer to Map
     * createKML() and findMap()
     */

    private void createKML(Context context) {
        try {
            KmlLayer routeLayer = new KmlLayer(findMap(), R.raw.route1, context);
            routeLayer.addLayerToMap();
        }
        catch (Exception e) {
            // Something
        }
    }

    private GoogleMap findMap() {
        return ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                .getMap();
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */


    private double coordinates(double f) {
        int a = (int) f;
        double frac = (f - (double) a) / 0.6;
        return (double) a + frac;
    }
    private class getMapData extends AsyncTask<Void, Void, String>{
        private Context m;
        private URL url;
        public getMapData(Context c){
            m = c;
        }
        @Override
        protected void onCancelled(){
            checkCancelAsyncTask = true;
            //Toast.makeText(m, "cancelling task", Toast.LENGTH_LONG).show();
        }
        @Override
        protected void onPreExecute(){
            try {
                url = new URL("http://shuttletracker.zz.mu/?bus=1&latest=1");
            }catch (Exception e){
                Toast.makeText(m, "invalid URL", Toast.LENGTH_LONG).show();
            }
        }
        @Override
        protected String doInBackground(Void... params){
            String lat = new String();
            String lng = new String();
            try {
                URLConnection uc = url.openConnection();
                uc.setConnectTimeout(3000);
                BufferedReader br = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                String responseBody = br.readLine();
                JSONObject json = new JSONObject(responseBody);
                lat = json.getString("latitude");
                lng = json.getString("longitude");
            } catch (Exception e) {
                return "";
                //Toast.makeText(m, "Connection Error", Toast.LENGTH_LONG).show();
                //e.printStackTrace();
            }
            return lat+" "+lng;
        }
        //@Override
        protected void onPostExecute(String result){
            if (result == ""){
                Toast.makeText(m, "Internet Connection Problem", Toast.LENGTH_LONG).show();
                return ;
            }
            Calendar c = Calendar.getInstance();
            int seconds = c.get(Calendar.SECOND);
            if (result.length() >= 8) {
                String[] coords = result.split(" ");
                if (coords[0].length() > 0 && coords[1].length() > 0) {
                    Double lat = Double.parseDouble(coords[0]);
                    Double lng = Double.parseDouble(coords[1]);
                    LatLng myLoc = new LatLng(coordinates(lat / 100.0), coordinates(lng / 100.0));
                    float curZoom = mMap.getCameraPosition().zoom;

                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLoc, curZoom));
                    mMap.getUiSettings().setZoomControlsEnabled(true);
                    mMap.getUiSettings().setMapToolbarEnabled(false);
                    if (!moreThanOne){
                        moreThanOne = true;
                        prev = myLoc;
                        marker = mMap.addMarker(new MarkerOptions()
                                .title("ISM")
                                .snippet("Indian School Of Mines.")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_marker))
                                .position(myLoc));
                        marker.setVisible(true);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLoc, DEFAULT_ZOOM_LEVEL));
                        }
                    else{
                        marker.setPosition(myLoc);
                    }

                }
            }
            final Toast t = Toast.makeText(m, result + "->" + Integer.toString(seconds) , Toast.LENGTH_SHORT);
            t.show();
            Handler h = new Handler();
            h.postDelayed(new Runnable() {
                @Override
                public void run() {
                    t.cancel();
                }
            },750);
        }
    }
    //@Override
    public void onMapReady() {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(default_location, DEFAULT_ZOOM_LEVEL));
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        callAsynchronousTask();
    }
    public void callAsynchronousTask() {
        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        try{
                            if (!checkCancelAsyncTask) {
                                task = new getMapData(MapsActivity.this.context);
                                // PerformBackgroundTask this class is the class that extends AsynchTask
                                task.execute();
                            }
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                        }
                    }
                });
            }
        };

        // New one
        timer.schedule(doAsynchronousTask, 0, ref_time); //execute in every 2000 ms

        //timer.schedule(doAsynchronousTask, 0, 2000); //execute in every 2000 ms
    }

    // Navigation Drawer Part
    // Route
    /*
    public void route_map(Context mcontext){
        dialogBuilder = new AlertDialog.Builder(context);
        final String[] strrouteType = {"Route 1"};
        dialogBuilder.setTitle("Route");
        dialogBuilder.setSingleChoiceItems(strrouteType,-1,new DialogInterface.OnClickListener()
        {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                strRoute = strrouteType[which];
                dialog.dismiss();
                if (strRoute == "Route 1")


            }
        });
        AlertDialog dialogRemark = dialogBuilder.create();
        dialogRemark.show();

    }*/
    private void route_map() {
        new MaterialDialog.Builder(this)
                .title(R.string.route)
                .items(R.array.no_of_route)
                .itemsCallbackSingleChoice(route, new MaterialDialog.ListCallbackSingleChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                        showToast(which + ": " + text);
                        route = which;
                        if (route == 0)
                            createKML(context);
                        return true; // allow selection
                    }
                })
                .positiveText(R.string.choose)
                .show();
    }
    // Feedback
    /*
    public void feedback(Context mContext){
        dialogBuilder = new AlertDialog.Builder(mContext);
        final EditText txtFeedback = new EditText(mContext);
        dialogBuilder.setTitle("Feedback");
        dialogBuilder.setMessage("Message");
        dialogBuilder.setView(txtFeedback);
        dialogBuilder.setPositiveButton("SEND FEEDBACK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                strFeedback += txtFeedback.getText().toString();
            }
        });
        dialogBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //
            }
        });
        AlertDialog dialogRemark = dialogBuilder.create();
        dialogRemark.show();
    }*/
    private void feedback() {
        new MaterialDialog.Builder(this)
                .title(R.string.feedback)
                .content(R.string.textfeedback)
                .inputType(InputType.TYPE_CLASS_TEXT |
                        InputType.TYPE_TEXT_VARIATION_PERSON_NAME |
                        InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                .positiveText(R.string.send)
                .input(R.string.input_hint, R.string.null_entry, false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog dialog, CharSequence input) {
                        showToast("Hello, " + input.toString() + "!");
                        try {
                            String urlResponse = "http://shuttletracker.hostei.com/feedback.php?response=";
                            urlResponse += URLEncoder.encode(input.toString());
                            URL url = new URL(urlResponse);
                            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                        } catch (Exception E) {
                            showToast("Error in sending the feedback");
                        }
                    }
                }).show();
    }
    // Setting
    public void setting(){
        Intent myIntent = new Intent(MapsActivity.this, Setting.class);
        MapsActivity.this.startActivity(myIntent);
    }
    // About
    public void about(){
        Intent myIntent = new Intent(MapsActivity.this, About.class);
        MapsActivity.this.startActivity(myIntent);
    }

    // Toast
    private void showToast(String message) {
        if (mToast != null) {
            mToast.cancel();
            mToast = null;
        }
        mToast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToast.show();
    }
}