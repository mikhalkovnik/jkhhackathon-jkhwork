package ru.lazybones.jkh.jkhwork;

import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.here.android.mpa.mapping.MapObject;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainDrawActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth mAuth;
    private DatabaseReference mydatabase;
    private ProgressBar progressBar;
    private LinearLayout parkmenu;
    private static final int REQUEST_CODE_SCAN = 0x0000c0de;
    private FloatingActionButton scanQrCodeButton;
    MapObject userplace;
    boolean allupdates;
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private ArrayList<PreOrder> preOrders;
    private RecyclerView mRecyclerView;
    private ReserveRVadapter adapter;
    private TextView preordtv;
    private OrderControlPanel bottomSheet;

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(this, SignINActivity.class);
            startActivity(intent);
            finish();
        }
        else {

            Constants.user = new User();
            Constants.user.setUserid(currentUser.getUid());
            Constants.user.setUserphone(currentUser.getPhoneNumber());
            settoken();
            updateuserdp();

        }
    }

    private void updateuserdp() {
        mydatabase.child("workers").child(Constants.user.getUserphone()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                        User element = dataSnapshot.getValue(User.class);
                        Constants.user=element;
                        updatedp();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void updatedp() {
        mydatabase.child("preorders").child(Constants.objectid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                progressBar.setVisibility(View.VISIBLE);
                preOrders = new ArrayList<>();

                if (dataSnapshot.exists()) {

                    for (DataSnapshot snapshot1 :
                            dataSnapshot.getChildren()) {

                        PreOrder element = snapshot1.getValue(PreOrder.class);
                        if (element!= null)

                            preOrders.add(element);


                    }

                }
                progressBar.setVisibility(View.GONE);
                Current.preOrders=preOrders;
                updatevepreorders();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });



    }

    private void updatevepreorders() {
        if (preOrders.size()>0) preordtv.setText("Мои текущие заявки");
        else preordtv.setText("Моих заявок в работе нет");


        adapter = new ReserveRVadapter(this, preOrders);
        mRecyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(new ReserveRVadapter.OnItemClickListener() {
            @Override
            public void onItemClick(PreOrder item, View v) {

                Current.preOrder=item;

                    bottomSheet.show(getSupportFragmentManager(), "");


            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_draw);

        mAuth = FirebaseAuth.getInstance();
        mydatabase= FirebaseDatabase.getInstance().getReference();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startActivity(new Intent(MainDrawActivity.this, AddneworderActivity.class));
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        mRecyclerView = (RecyclerView) findViewById(R.id.rec_view_orders);
        LinearLayoutManager   linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        preordtv = (TextView) findViewById(R.id.ordersinfotv);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        Constants.objectid = "12345544545";
        bottomSheet = new OrderControlPanel();


    }

    private void settoken() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {

                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        mydatabase.child("users").child(mAuth.getCurrentUser().getUid()).child("nottoken").setValue(token);

                    }
                });

        FirebaseMessaging.getInstance().subscribeToTopic("allnot");

    }

    public void toservices(View view) {

        Order myorder = new Order(Current.preOrder, Constants.user.getUserid(),Constants.user.getUserlogin(),Constants.user.getUserphone());
        Map<String, Object> newPerformerV = myorder.toMap();

        Map<String, Object> childUpdates = new HashMap<>();


        childUpdates.put("/preorders/" + myorder.getObjectid() +"/"+ myorder.getId() , newPerformerV);
        childUpdates.put("/users/" + myorder.getUserid() + "/preorders/" + myorder.getObjectid() +"/"+ myorder.getId(), newPerformerV);
        childUpdates.put("/workOrders/"+ Constants.user.getUserid()+"/"+ myorder.getId(), newPerformerV );

        progressBar.setVisibility(View.VISIBLE);

        mydatabase.updateChildren(childUpdates).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                // Write was successful!
                // ...
                Toast.makeText(MainDrawActivity.this, " Заказ принят!",
                        Toast.LENGTH_SHORT).show();


                progressBar.setVisibility(View.GONE);


            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Write failed
                        // ...
                        Toast.makeText(MainDrawActivity.this, "ошибка. ",
                                Toast.LENGTH_SHORT).show();
                    }
                });
//        startActivity(new Intent(this, ServicesActivity.class));

    }

    public void toinfo(View view) {


        startActivity(new Intent(this, OrderInfoActivity.class));


    }


    public void toask(View view) {

        startActivity(new Intent(this, ChatBoxActivity.class));

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
        getMenuInflater().inflate(R.menu.main_draw, menu);
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
//           Intent intent = new Intent(this, MainDrawActivity.class);
//           startActivity(intent);
        } else if (id == R.id.nav_orders) {
            Intent intent = new Intent(this, MyordersActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_payment) {
            Intent intent = new Intent(this, PaymentActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_notification) {
            Intent intent = new Intent(this, NotificationActivity.class);
            startActivity(intent);


        } else if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

        }
        else if (id == R.id.nav_map) {
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
