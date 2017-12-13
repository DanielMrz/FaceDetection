package application.f3cro.facetracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.google.android.gms.common.images.Size;
import com.google.android.gms.vision.CameraSource;

import java.io.IOException;

public class CameraSourceView extends ViewGroup {
    private static final String TAG = "CameraSourcePreview";

    private Context mContext;
    private SurfaceView mSurfaceView;
    private boolean mStartRequested;
    private boolean mSurfaceAvailable;
    private CameraSource mCameraSource;

    private GraphicDraw mOverlay;

    public CameraSourceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mStartRequested = false;
        mSurfaceAvailable = false;

        mSurfaceView = new SurfaceView(context);
        mSurfaceView.getHolder().addCallback(new SurfaceCallback());
        addView(mSurfaceView);
    }

    // Metody startujace kamere
    public void start(CameraSource cameraSource) throws IOException {
        // Zatrzymaj jesli nie istnieje kamera
        if (cameraSource == null) {
            stop();
        }

        mCameraSource = cameraSource;

        // Startuj jesli istnieje kamera
        if (mCameraSource != null) {
            mStartRequested = true;
            startIfReady();
        }
    }

    public void start(CameraSource cameraSource, GraphicDraw overlay) throws IOException {
        mOverlay = overlay;
        start(cameraSource);
    }

    // Metoda zatrzymujaca kamere
    public void stop() {
        if (mCameraSource != null) {
            mCameraSource.stop();
        }
    }

    // Metoda zatrzymujaca kamere oraz uwalniajaca zasoby kamery
    public void release() {

        // Jesli istnieje kamera
        if (mCameraSource != null) {
            mCameraSource.release();
            mCameraSource = null;
        }
    }

    // Nie ostrzegaj o braku zezwolenia
    @SuppressLint("MissingPermission")
    private void startIfReady() throws IOException {

        // Jesli wywolano z metody start oraz wczesniej poprawnie utworzono powierzchnie
        if (mStartRequested && mSurfaceAvailable) {

            // Otworz kamere i zacznij wysylac klatki
            mCameraSource.start(mSurfaceView.getHolder());

            if (mOverlay != null) {
                Size size = mCameraSource.getPreviewSize();

                // Pobierz szerokosc i wysokosc
                int min = Math.min(size.getWidth(), size.getHeight());
                int max = Math.max(size.getWidth(), size.getHeight());

                // Jesli jestesmy w trybie portretowym
                if (isPortraitMode()) {
                    // Zamien szerokosc i wysokosc
                    mOverlay.setCameraInfo(min, max, mCameraSource.getCameraFacing());
                } else {
                    mOverlay.setCameraInfo(max, min, mCameraSource.getCameraFacing());
                }

                // Wyczysc wszystkie grafiki
                mOverlay.clear();
            }

            mStartRequested = false;
        }
    }

    // Interfejs implementowany dla otrzymywania informacji o zmianach powierzchni
    private class SurfaceCallback implements SurfaceHolder.Callback {

        // Metoda wywolywana zaraz po utworzeniu powierzchni
        @Override
        public void surfaceCreated(SurfaceHolder surface) {
            mSurfaceAvailable = true;
            try {
                startIfReady();
            } catch (IOException e) {
                Log.e(TAG, "Nie udało się pobrać źródła obrazu.", e);
            }
        }

        // Metoda wywolywana po zniszczeniu powierzchni
        @Override
        public void surfaceDestroyed(SurfaceHolder surface) {
            mSurfaceAvailable = false;
        }

        // Metoda wywolywana zaraz po zmianie formatu lub wielkosci powierzchni
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        }
    }

    // Metoda wywolywana przy przydzielaniu wielkosci i pozycji
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = 320;
        int height = 240;

        // Jesli istnieje kamera
        if (mCameraSource != null) {
            Size size = mCameraSource.getPreviewSize();

            // I ma przypisane wymiary
            if (size != null) {
                width = size.getWidth();
                height = size.getHeight();
            }
        }

        // Zamien szerokosc i wysokosc z uwagi na obrocenie o 90 stopni
        if (isPortraitMode()) {
            int tmp = width;
            width = height;
            height = tmp;
        }

        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        // Oblicz wysokosc i szerokosc, aby potencjalnie dopasowac szerokosc.
        int childWidth = layoutWidth;
        int childHeight = (int)(((float) layoutWidth / (float) width) * height);

        // Jesli wysokosc jest zbyt wysoka przy uzyciu dopasowanej szerokosci, to dopasuj wysokosc
        if (childHeight > layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int)(((float) layoutHeight / (float) height) * width);
        }

        // Przydziel obliczone wartosci wysokosci i szerokosci
        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(0, 0, childWidth, childHeight);
        }

        try {
            startIfReady();
        } catch (IOException e) {
            Log.e(TAG, "Nie udało się porbać źródła obrazu.", e);
        }
    }

    // Metoda sprawdzajaca, czy jestesmy w trybie portretowym
    private boolean isPortraitMode() {
        int orientation = mContext.getResources().getConfiguration().orientation;

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        Log.d(TAG, "Tryb portretu domyślnie zwraca fałsz");
        return false;
    }
}
