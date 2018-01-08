package com.google.fidel.shiprating;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by fidel on 7/15/17.
 */

public class SignupActivity extends AppCompatActivity{

    private EditText inputEmail, inputPassword;
    private Button btnSignIn, btnSignUp, btnResetPassword;
    private ProgressBar progressBar;
    private FirebaseAuth auth;
    final String database_name = "Users";
    final String database_cliente = "cliente";
    final String database_restaurant = "restaurant";
    final String tim = "5";
    final String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
    SerGPS gps;
    DatabaseReference mDatabaseUsers;
    DatabaseReference mDatabaseCliente;
    DatabaseReference mDatabaseRestaurant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        btnSignIn = (Button) findViewById(R.id.sign_in_button);
        btnSignUp = (Button) findViewById(R.id.sign_up_button);
        inputEmail = (EditText) findViewById(R.id.email);
        inputPassword = (EditText) findViewById(R.id.password);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        final CheckBox clientType = (CheckBox) findViewById(R.id.restaurant_cliente);
        btnResetPassword = (Button) findViewById(R.id.btn_reset_password);
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference(database_name);
        mDatabaseCliente = mDatabaseUsers.child(database_cliente);
        mDatabaseRestaurant = mDatabaseUsers.child(database_restaurant);


        btnResetPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, ResetPasswordActivity.class));
            }
        });

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Ingrese Password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), "Ingrese minimo 6 caracteres", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                //create user
                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignupActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Toast.makeText(SignupActivity.this, "Creacion de Usuario:EXITO:" + task.isSuccessful(), Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.GONE);
                                if (!task.isSuccessful()) {
                                    Toast.makeText(SignupActivity.this, "Error de Autentificacion." + task.getException(),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    if(clientType.isChecked()){
                                        gps = new SerGPS(SignupActivity.this,tim);
                                        startService(new Intent(SignupActivity.this,SerGPS.class));
                                        double latitude = 0.0;
                                        double longitude = 0.0;

                                        Thread background = new Thread(new Runnable() {
                                            public void run() {
                                                try {
                                                    Date date = new Date();
                                                    Date newDate = new Date(date.getTime() + (604800000L * 2) + (24 * 60 * 60));
                                                    SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd");
                                                    String stringDate = dt.format(newDate);
                                                    threadMsg(stringDate);
                                                } catch (Throwable t) {
                                                    //Muere en el thread-background
                                                    Log.i("Animation", "Thread  exception " + t);
                                                }
                                            }

                                            private void threadMsg(String msg) {
                                                if (!msg.equals(null) && !msg.equals("")) {
                                                    Message msgObj = handler.obtainMessage();
                                                    Bundle b = new Bundle();
                                                    b.putString("message", msg);
                                                    msgObj.setData(b);
                                                    handler.sendMessage(msgObj);
                                                }
                                            }

                                            private final Handler handler = new Handler() {
                                                public void handleMessage(Message msg) {

                                                    String aResponse = msg.getData().getString("message");

                                                    if ((null != aResponse)) {
                                                        Toast.makeText(getBaseContext(), "Fecha de Creacion "+aResponse, Toast.LENGTH_SHORT).show();
                                                    }
                                                    else
                                                    {
                                                        Toast.makeText(getBaseContext(), "No se configuro Tiempo.", Toast.LENGTH_SHORT).show();
                                                    }

                                                }
                                            };

                                        });
                                        background.start();
                                        if(gps.canGetLocation()){
                                            FirebaseUser user = auth.getCurrentUser();
                                            String userId = user.getUid();
                                            latitude = gps.getLatitude();
                                            longitude = gps.getLongitude();
                                            Restaurant restaurant = new Restaurant(email,"hola",date,longitude,latitude);
                                            mDatabaseRestaurant.child(userId).setValue(restaurant);
                                            //mDatabaseRestaurantOffers.child("creationDate").setValue(ServerValue.TIMESTAMP);
                                            Toast.makeText(SignupActivity.this, latitude+" - "+ longitude, Toast.LENGTH_SHORT).show();
                                        }else{
                                            gps.showSettingsAlert();
                                        }
                                        startActivity(new Intent(SignupActivity.this, RestaurantActivity.class));
                                        finish();
                                    } else {
                                        FirebaseUser user = auth.getCurrentUser();
                                        String userId = user.getUid();
                                        Cliente cliente = new Cliente(email,ClientType.CLIENTE);
                                        mDatabaseCliente.child(userId).setValue(cliente);
                                        startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                        finish();
                                    }
                                }
                            }
                        });

            }
        });
    }

    private boolean runtime_permission() {
        if(Build.VERSION.SDK_INT>=23 && ContextCompat.checkSelfPermission(SignupActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED&& ContextCompat.checkSelfPermission(SignupActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},123);
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==123){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED && grantResults[1]==PackageManager.PERMISSION_GRANTED){
                //enable_button();
                String l ="ds";
            }else{
                runtime_permission();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        progressBar.setVisibility(View.GONE);
    }
}
