/**
 * Created by fidel on 7/14/17.
 *
 * En este codigo se aplica dos estados para nuestros activities, onCreate y onStart
 *
 */
package com.google.fidel.shiprating;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private static final String TAG = MainActivity.class.getSimpleName();

    private FirebaseAuth.AuthStateListener authListener;
    private final String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
    private FirebaseAuth auth;
    private Button signOut;
    private Button addIngredient;
    private Button btnJamear;
    private TextView submitText;
    private EditText newIngredient;
    private int positionTextView = 6;
    private final String tim = "5";
    private SerGPS gps;
    private final String DATA_BASE = "UserPreferences";
    private DatabaseReference mDatabaseUserPreferences;
    private List<String> ingredientListUpload;
    private LinearLayout ingredientList;
    private FirebaseUser user;
    private ProgressBar progressBar;

    /**
     * ======================
     * onCreate
     * =====================
     * Este es el metodo inicial cuando se inicia la aplicacion y este recibe un parametro
     * @param savedInstanceState , quien avisa a la aplicacion el estado del usuario en conexion
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();
        mDatabaseUserPreferences = FirebaseDatabase.getInstance().getReference(DATA_BASE);
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };
        setTitle(user.getEmail());
        ingredientListUpload = new ArrayList<String>();
        newIngredient = (EditText)findViewById(R.id.new_ingredient);
        addIngredient = (Button) findViewById(R.id.add_ingredient);
        ingredientList = (LinearLayout) findViewById(R.id.ingredients);
        submitText = (TextView) findViewById(R.id.submit_text);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        positionTextView = 5;
        addIngredient.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                positionTextView = agregarIngrediente() + 1;
                newIngredient.setHint("Ingrediente "+ (positionTextView-4));
                newIngredient.setText("");
            }
        });
        btnJamear = (Button) findViewById(R.id.jamear);
        btnJamear.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                storeInDatabase();
                startActivity(new Intent(MainActivity.this, MapsActivity.class));
            }
        });
        if(!runtime_permission()){
            gps = new SerGPS(MainActivity.this,tim);
        }
        runtime_permission();
    }

    private int agregarIngrediente(){
        String linea = newIngredient.getText().toString();
        if(linea.isEmpty())return positionTextView-1;
        TextView textView1 = new TextView(MainActivity.this);
        textView1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        textView1.setText(Integer.toString(positionTextView-5) + ".- "+linea);
        ingredientListUpload.add(linea);
        textView1.setTextSize(20);
        textView1.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        textView1.setPadding(1, 1, 5, 1);// in pixels (left, top, right, bottom)
        ingredientList.addView(textView1,positionTextView);
        return positionTextView;
    }

    private void storeInDatabase() {
        submitText.setText("Buscando Lugares");
        mDatabaseUserPreferences.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long countItem = dataSnapshot.child(user.getUid()).child(date).getChildrenCount();
                addFirebase(String.valueOf(countItem));
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("Error Firebase",""+databaseError.getCode());
            }
        });
    }
    private void addFirebase(String id){
        gps = new SerGPS(MainActivity.this,tim);
        startService(new Intent(MainActivity.this,SerGPS.class));
        if(gps.canGetLocation()){
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            ClientPreference newPeticion = new ClientPreference(longitude, latitude, ingredientListUpload, ServerValue.TIMESTAMP);
            progressBar.setVisibility(View.VISIBLE);
            btnJamear.setVisibility(View.INVISIBLE);
            addIngredient.setVisibility(View.INVISIBLE);
            mDatabaseUserPreferences.child(user.getUid()).child(date).child(id).setValue(newPeticion, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError != null){
                        Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(MainActivity.this, "Bienvenido al Mapa", Toast.LENGTH_SHORT).show();
                        btnJamear.setVisibility(View.VISIBLE);
                        addIngredient.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.INVISIBLE);
                        submitText.setText("Ingresando al mapa");
                    }
                }
            });

        }else{
            gps.showSettingsAlert();
        }
    }

    public void signOut() {
        auth.signOut();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
        Toast.makeText(this, "Regresa Pronto", Toast.LENGTH_SHORT).show();
        finish();
    }
    public void cambiarPassword() {
        auth.signOut();
        startActivity(new Intent(MainActivity.this, ResetPasswordActivity.class));
        Toast.makeText(this, "Regresa Pronto", Toast.LENGTH_SHORT).show();
        finish();
    }
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
    }
    private boolean runtime_permission() {
        if(Build.VERSION.SDK_INT>=23 && ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED&& ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},123);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==123){
            if(!(grantResults[0]==PackageManager.PERMISSION_GRANTED && grantResults[1]==PackageManager.PERMISSION_GRANTED)){
                runtime_permission();
            }
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.sign_out:
                signOut();
                return true;
            case R.id.cambiar_pass:
                cambiarPassword();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
