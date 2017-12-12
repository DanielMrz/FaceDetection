package application.f3cro.facetracking;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.IOException;

/**
 * Główna aktywność aplikacji, która odpowiada za śledzenie twarzy. Ta aplikacja wykrywa twarze za pomocą tylnej kamery i rysuje
 * nakładki graficzne wskazujące pozycję, rozmiar i identyfikator każdej twarzy.
 */
public final class FaceTrackerMain extends AppCompatActivity {
    private static final String TAG = "Detektor Twarzy";

    private CameraSource mCameraSource = null;

    private CameraSourceView mPreview;
    private GraphicDraw mGraphicDraw;
    private TextView mUpdates;

    private static final int RC_HANDLE_GMS = 9001;
    // kody żądań uprawnień muszą być < 256
    private static final int RC_HANDLE_CAMERA_PERM = 2;


    /**
     * Inicjuje interfejs użytkownika i inicjuje tworzenie detektora twarzy.
     */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_face_tracker);

        mPreview = (CameraSourceView) findViewById(R.id.preview);
        mGraphicDraw = (GraphicDraw) findViewById(R.id.faceOverlay);
        //mUpdates = (TextView) findViewById(R.id.faceUpdates);




        // Sprawdź uprawnienia kamery przed uzyskaniem dostępu do kamery. Jeśli
        // uprawnienia nie zostały jeszcze przyznane, prośba o pozwolenie.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            createCameraSource();
        } else {
            requestCameraPermission();
        }
    }

    /**
     * Obsługuje żądanie zgody kamery.
     * Pokazuje komunikat "Snackbar" tym, którzy nie zezwolili na dostęp, dlaczego potrzebne jest zezwolenie
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};

        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, permissions, RC_HANDLE_CAMERA_PERM);
            return;
        }

        final Activity thisActivity = this;

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.requestPermissions(thisActivity, permissions,
                        RC_HANDLE_CAMERA_PERM);
            }
        };

        Snackbar.make(mGraphicDraw, R.string.permission_camera_rationale,
                Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok, listener)
                .show();
    }

    /**
     * Tworzenie i uruchomienie kamery
     */
    private void createCameraSource() {

        Context context = getApplicationContext();
        FaceDetector detector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .build();

        detector.setProcessor(
                new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory())
                        .build());

        if (!detector.isOperational()) {
            new AlertDialog.Builder(this)
                    .setMessage("Detektor twarzy nie jest jeszcze gotowy.")
                    .show();

            Log.w(TAG, "Detektor twarzy nie jest jeszcze gotowy.");
            return;
        }

        mCameraSource = new CameraSource.Builder(context, detector)
                .setRequestedPreviewSize(1024, 720)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(30.0f)
                .setAutoFocusEnabled(true)
                .build();
    }

    /**
     * Restart kamery.
     */
    @Override
    protected void onResume() {
        super.onResume();

        startCameraSource();
    }

    /**
     * Zatrzymanie kamery.
     */
    @Override
    protected void onPause() {
        super.onPause();
        mPreview.stop();
    }

    /**
     * Zwolnienie zasobów związanych ze źródłem kamery, powiązanego detektora i
     * informacji przetwarzania    */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mCameraSource != null) {
            mCameraSource.release();
        }
    }

    /**
     * Callback
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode != RC_HANDLE_CAMERA_PERM) {
            Log.d(TAG, "Przyznanie uprawień zwróciło nieoczekiwany wynik: " + requestCode);
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Przyznano uprawnienia do kamery");
            // we have permission, so create the camerasource
            createCameraSource();
            return;
        }

        Log.e(TAG, "Nie przyznano uprawnień = " + grantResults.length +
                " kod = " + (grantResults.length > 0 ? grantResults[0] : "(pusty)"));

        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Detektor Twarzy")
                .setMessage(R.string.no_camera_permission)
                .setPositiveButton(R.string.ok, listener)
                .show();
    }


    /**
     * Rozpoczyna lub uruchamia źródło kamery, jeśli istnieje. Jeśli źródło kamery jeszcze nie istnieje
     * (np. ponieważ onResume został wywołany przed utworzeniem źródła kamery), zostanie to wywołane
     * ponownie, gdy źródło kamery zostanie utworzone.
     */
    private void startCameraSource() {

        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dlg.show();
        }

        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicDraw);
            } catch (IOException e) {
                Log.e(TAG, "Nie udało się pobrać obrazu ze źródła.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }


    /*
     * Tworzenie modułu śledzenia twarzy, który zostanie powiązany z nową twarzą. Multiprocesor
     * wykorzystuje tę "factory" do tworzenia modułów do śledzenia twarzy w razie potrzeby - po jednym dla każdej osoby.
     */
    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicDraw,FaceTrackerMain.this);
        }
    }

    /**
     * Face tracker dla każdej wykrytej osoby. To utrzymuje grafikę twarzy w aplikacji
     * powiązana nakładka na twarz.
     */
    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicDraw mOverlay;
        private FaceModel mFaceModel;

        GraphicFaceTracker(GraphicDraw overlay, Context context) {
            mOverlay = overlay;
            mFaceModel = new FaceModel(overlay,context);
        }

        /**
         * Rozpocznij śledzenie wykrytej instancji twarzy w nakładce na twarz.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            mFaceModel.setId(faceId);
        }

        /**
         * Zaktualizuj pozycję / cechy twarzy w nakładce.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceModel);
            mFaceModel.updateFace(face);

        }

        /**
         * Ukryj grafikę, gdy odpowiednia twarz nie została wykryta. To może się zdarzyć...
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mOverlay.remove(mFaceModel);
        }

        /**

         * URuchamia się, gdy zakłada się, że twarz zniknęła na dobre. Usuń adnotację graficzną z
         * nakładka.
         */
        @Override
        public void onDone() {
            mOverlay.remove(mFaceModel);
        }
    }
}
