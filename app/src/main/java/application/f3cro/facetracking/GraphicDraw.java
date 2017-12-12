package application.f3cro.facetracking;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.gms.vision.CameraSource;

import java.util.HashSet;
import java.util.Set;

/**
 * Widok, który renderuje serię niestandardowych grafik, które mają zostać nałożone na skojarzony podgląd
 * (tj. podgląd kamery). Twórca może dodawać obiekty graficzne, aktualizować obiekty i usuwać
 * je, uruchamiając odpowiedni rysunek i unieważnienie w widoku.
 *
 * Obsługuje skalowanie i dublowanie grafiki w odniesieniu do właściwości podglądu kamery. The
 * Pomysł polega na tym, że elementy wykrywające są wyrażane w postaci rozmiaru podglądu, ale muszą być skalowane
 * do pełnego rozmiaru widoku, a także do lustrzanego odbicia w przypadku kamery przedniej.
 *
 * Powiązane elementy {@link Graphic} powinny używać następujących metod do konwersji, aby zobaczyć współrzędne
 * dla rysowanych grafik:
 *
 * {@ link Graphic # scaleX (float)} i {@link Graphic # scaleY (float)} dostosowują rozmiar
 * podana wartość ze skali podglądu do skali widoku.
 * <li> {@ link Graphic # translateX (float)} i {@link Graphic # translateY (float)} dopasuj współrzędne
 * z układu współrzędnych podglądu do układu współrzędnych widoku.
 *
 */
public class GraphicDraw extends View {
    private final Object mLock = new Object();
    private int mPreviewWidth;
    private float mWidthScaleFactor = 1.0f;
    private int mPreviewHeight;
    private float mHeightScaleFactor = 1.0f;
    private int mFacing = CameraSource.CAMERA_FACING_BACK;
    private Set<Graphic> mGraphics = new HashSet<>();

    /**
     * Klasa podstawowa dla niestandardowego obiektu graficznego, który ma być renderowany w nakładce graficznej. Podklasa
     * zaimplementuje metodę {@link Graphic # draw (Canvas)}, aby zdefiniować
     * element graficzny. Dodaj wystąpienia do nakładki za pomocą {@link GraphicDraw # add (Graphic)}.
     */
    public static abstract class Graphic {
        private GraphicDraw mOverlay;

        public Graphic(GraphicDraw overlay) {
            mOverlay = overlay;
        }

        /**
         * Narysuj grafikę na dostarczonym obrazie.
         */
        public abstract void draw(Canvas canvas);

        /**
         * Dostosowuje poziomą wartość
         */
        public float scaleX(float horizontal) {
            return horizontal * mOverlay.mWidthScaleFactor;
        }

        /**
         * Dostosowuje pionowa wartosc
         */
        public float scaleY(float vertical) {
            return vertical * mOverlay.mHeightScaleFactor;
        }

        /**
         * Dostosowuje współrzędną x z układu współrzędnych podglądu do współrzędnych widoku
         * system.

         */
        public float translateX(float x) {
            if (mOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                return mOverlay.getWidth() - scaleX(x);
            } else {
                return scaleX(x);
            }
        }

        /**
         * Dostosowuje współrzędną y z układu współrzędnych podglądu do współrzędnych widoku
         * system.
         */
        public float translateY(float y) {
            return scaleY(y);
        }

        public void postInvalidate() {
            mOverlay.postInvalidate();
        }
    }

    public GraphicDraw(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Usuwa całą grafikę.
     */
    public void clear() {
        synchronized (mLock) {
            mGraphics.clear();
        }
        postInvalidate();
    }

    /**
     * Dodaj grafikę.
     */
    public void add(Graphic graphic) {
        synchronized (mLock) {
            mGraphics.add(graphic);
        }
        postInvalidate();
    }

    /**
     * Usuń grafikę.
     */
    public void remove(Graphic graphic) {
        synchronized (mLock) {
            mGraphics.remove(graphic);
        }
        postInvalidate();
    }




    /**
     * Ustawia atrybuty kamery dla rozmiaru i kierunku przewijania, co informuje, jak przekształcić
     * współrzędne obrazu...
     */
    public void setCameraInfo(int previewWidth, int previewHeight, int facing) {
        synchronized (mLock) {
            mPreviewWidth = previewWidth;
            mPreviewHeight = previewHeight;
            mFacing = facing;
        }
        postInvalidate();
    }

    /**
     * Rysuje nakładkę z powiązanymi z nią obiektami graficznymi.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        synchronized (mLock) {
            if ((mPreviewWidth != 0) && (mPreviewHeight != 0)) {
                mWidthScaleFactor = (float) canvas.getWidth() / (float) mPreviewWidth;
                mHeightScaleFactor = (float) canvas.getHeight() / (float) mPreviewHeight;
            }

            for (Graphic graphic : mGraphics) {
                graphic.draw(canvas);
            }
        }
    }
}
