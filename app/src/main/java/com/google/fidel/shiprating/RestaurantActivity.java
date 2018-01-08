package com.google.fidel.shiprating;

/**
 * Created by root on 12/25/17.
 */

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
public class RestaurantActivity extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyC95AVKfWzzSbwj86iwOOl0IGteZCzscfQ";
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;
    private static final int PICK_IMAGE_REQUEST = 234;

    private final String data_base = "RestaurantOffer";
    private final String date = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
    private StorageReference mStorageRef;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseAuth auth;
    private LinearLayout ingredientList;
    private Button signOut;
    private Button addIngredient;
    private ImageView foodImage;
    private TextView uploadImage;
    private EditText foodName;
    private EditText ingredientInput;
    private final String database_name = "Restaurants";
    private DatabaseReference mDatabaseRestaurantOffers;
    private int positionText;
    private List<String> ingredientListUpload;
    private FirebaseUser user;

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
        setContentView(R.layout.activity_restaurant);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        auth = FirebaseAuth.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();
        mDatabaseRestaurantOffers = FirebaseDatabase.getInstance().getReference(data_base);
        ingredientListUpload = new ArrayList<String>();
        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    startActivity(new Intent(RestaurantActivity.this, LoginActivity.class));
                    finish();
                }
            }
        };
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(RestaurantActivity.this);
                builder
                        .setMessage("Ingrese imagen del plato")
                        .setPositiveButton("Mis imagenes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                iniciarGaleria();
                            }
                        })
                        .setNegativeButton("Camara", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                iniciarCamara();
                            }
                        });
                builder.create().show();
            }
        });
        setTitle(user.getEmail());
        uploadImage = (TextView) findViewById(R.id.upload_image);
        foodName = (EditText) findViewById(R.id.food_name);
        ingredientInput = (EditText) findViewById(R.id.ingredient_input);
        addIngredient = (Button) findViewById(R.id.add_ingredient);
        ingredientList = (LinearLayout) findViewById(R.id.ingredients);
        positionText = 5;
        addIngredient.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                positionText = agregarIngrediente() + 1;
                ingredientInput.setHint("Ingrediente "+ (positionText-4));
                ingredientInput.setText("");
            }
        });
        foodImage = (ImageView) findViewById(R.id.food_image);

    }

    private int agregarIngrediente(){
        String linea = ingredientInput.getText().toString();
        if(linea.isEmpty())return positionText-1;
        TextView textView1 = new TextView(RestaurantActivity.this);
        textView1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        textView1.setText(Integer.toString(positionText-5) + ".- "+linea);
        ingredientListUpload.add(linea);
        textView1.setTextSize(20);
        textView1.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        textView1.setPadding(1, 1, 5, 1);// in pixels (left, top, right, bottom)
        ingredientList.addView(textView1,positionText);
        return positionText;
    }

    private void subirIngredientesFirebase(List<String> lista){
        for(String ing: lista){
            TextView textView1 = new TextView(RestaurantActivity.this);
            textView1.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT));
            textView1.setText(Integer.toString(positionText-5) + ".- "+ing);
            ingredientListUpload.add(ing);
            textView1.setTextSize(20);
            textView1.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
            textView1.setPadding(1, 1, 5, 1);// in pixels (left, top, right, bottom)
            ingredientList.addView(textView1,positionText);
            positionText++;
        }
        String name = foodName.getText().toString();
        if(!name.isEmpty()) {
            RestaurantOffer newFood = new RestaurantOffer(ingredientListUpload, ServerValue.TIMESTAMP);
            mDatabaseRestaurantOffers.child(user.getUid()).child(date).child(name).setValue(newFood, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError != null){
                        Toast.makeText(RestaurantActivity.this, "Error", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(RestaurantActivity.this, "Guardado Exitosamente", Toast.LENGTH_SHORT).show();
                    }
                }
            });
            uploadImage.setText("Se ha guardado Exitosamente");
        }else{
            Toast.makeText(this, "Ingrese nombre del plato", Toast.LENGTH_SHORT).show();
        }
    }
    public void signOut() {
        auth.signOut();
        startActivity(new Intent(RestaurantActivity.this, LoginActivity.class));
        finish();
    }
    public void cambiarPassword() {
        auth.signOut();
        startActivity(new Intent(RestaurantActivity.this, ResetPasswordActivity.class));
        Toast.makeText(this, "Regresa Pronto", Toast.LENGTH_SHORT).show();
        finish();
    }
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authListener);
    }
    public void iniciarGaleria() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Selecciona una foto"), GALLERY_IMAGE_REQUEST);
        }
    }

    public void iniciarCamara() {
        if (PermissionUtils.requestPermission(this, CAMERA_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getArchivosCamara());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getArchivosCamara() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri file = data.getData();
        StorageReference imagen = mStorageRef.child("platos").child(file.getLastPathSegment());
        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            subirImagen(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getArchivosCamara());
            subirImagen(photoUri);
        }
        imagen.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        int h = 4;
                    }
                });
    }

    public void subirImagen(Uri uri) {
        if (uri != null) {
            try {
                Bitmap bitmap = escalarABitMap(MediaStore.Images.Media.getBitmap(getContentResolver(), uri), 1200);
                llamarCloudVision(bitmap);
                foodImage.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Error : " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Sin imagen");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    public Bitmap escalarABitMap(Bitmap bitmap, int maxDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private void llamarCloudVision(final Bitmap bitmap) throws IOException {
        uploadImage.setText("Guardando Ingredientes...");

        new AsyncTask<Object, Void, List<String> >() {
            @Override
            protected List<String> doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                                @Override
                                protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                                        throws IOException {
                                    super.initializeVisionRequest(visionRequest);

                                    String packageName = getPackageName();
                                    visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                                    String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                                    visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                                }
                            };

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);

                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        Image base64EncodedImage = new Image();
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                            Feature labelDetection = new Feature();
                            labelDetection.setType("LABEL_DETECTION");
                            labelDetection.setMaxResults(10);
                            add(labelDetection);
                        }});

                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchAnnotateImagesRequest);
                    annotateRequest.setDisableGZipContent(true);
                    Log.d(TAG, "Enviando peticion");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return respuestaGoogleVisionCloud(response);

                } catch (GoogleJsonResponseException e) {
                    Log.d(TAG, "Error en la API " + e.getContent());
                } catch (IOException e) {
                    Log.d(TAG, "Error en la consulta " +
                            e.getMessage());
                }
                return null;
            }

            protected void onPostExecute(List<String> result) {
                subirIngredientesFirebase(result);
            }
        }.execute();
    }

    private List<String> respuestaGoogleVisionCloud(BatchAnnotateImagesResponse response) {
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        ArrayList<String> descripcionGoogle = new ArrayList<String>();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                String desc = label.getDescription().toString();
                descripcionGoogle.add(desc);
                ingredientListUpload.add(desc);
            }
        } else {
            System.out.println("Ocurrio un error");
        }
        return descripcionGoogle;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    iniciarCamara();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    iniciarGaleria();
                }
                break;
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