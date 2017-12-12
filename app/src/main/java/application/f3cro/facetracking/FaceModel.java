package application.f3cro.facetracking;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.vision.face.Face;

/**
 * Instancja graficzna do renderowania położenia twarzy, orientacji i punktów orientacyjnych w powiązanym obiekcie
 * widoku nakładki graficznej
 */
class FaceModel extends GraphicDraw.Graphic {
    private static final float FACE_POSITION_RADIUS = 10.0f;
    private static final float ID_TEXT_SIZE = 40.0f;
    private static final float ID_Y_OFFSET = 50.0f;
    private static final float ID_X_OFFSET = -50.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;

    private static final double SMILING_PROB_THRESHOLD = .15;
    private static final double EYE_OPEN_PROB_THRESHOLD = .5;

    private static final int COLOR_CHOICES[] = {
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.MAGENTA,
        Color.RED,
        Color.WHITE,
        Color.YELLOW
    };
    private static int mCurrentColorIndex = 0;

    private Paint mFacePositionPaint;
    private Paint mIdPaint;
    private Paint mBoxPaint;

    private volatile Face mFace;
    private int mFaceId;
    private Context mContext;

    FaceModel(GraphicDraw overlay, Context context) {
        super(overlay);

        mContext=context;
        mCurrentColorIndex = (mCurrentColorIndex + 1) % COLOR_CHOICES.length;
        final int selectedColor = COLOR_CHOICES[mCurrentColorIndex];

        mFacePositionPaint = new Paint();
        mFacePositionPaint.setColor(selectedColor);

        mIdPaint = new Paint();
        mIdPaint.setColor(selectedColor);
        mIdPaint.setTextSize(ID_TEXT_SIZE);

        mBoxPaint = new Paint();
        mBoxPaint.setColor(selectedColor);
        mBoxPaint.setStyle(Paint.Style.STROKE);
        mBoxPaint.setStrokeWidth(BOX_STROKE_WIDTH);

    }

    void setId(int id) {
        mFaceId = id;
    }


    /**
     * Aktualizuje instancję twarzy po wykryciu najnowszej ramki. Unieważnia atrybut
     * odpowiednie części nakładki, aby uruchomić przerysowanie.
     */
    void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }


    /**
     * Rysuje adnotacje twarzy dla pozycji na widoku.
     */
    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if (face == null) {
            return;
        }

        // Rysuje okrąg w miejscu wykrytej twarzy, z identyfikatorem ścieżki twarzy poniżej
        float x = translateX(face.getPosition().x + face.getWidth() / 2);
        float y = translateY(face.getPosition().y + face.getHeight() / 2);

        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, mFacePositionPaint);
        canvas.drawText("Numer: " + mFaceId, x + ID_X_OFFSET, y + ID_Y_OFFSET, mIdPaint);
        canvas.drawText("Prawdopodobieństwo wystąpienia uśmiechu: " + String.format("%.2f", face.getIsSmilingProbability()), x - ID_X_OFFSET, y - ID_Y_OFFSET, mIdPaint);

        String prediction = getPrediction(face.getEulerY(),face.getEulerZ());
        canvas.drawText("Kierunek odchylenia twarzy: "+prediction,x-ID_X_OFFSET,y-ID_Y_OFFSET+3*ID_TEXT_SIZE,mIdPaint);
        // Rysuje obwiednię wokół twarzy
        float xOffset = scaleX(face.getWidth() / 2.0f);
        float yOffset = scaleY(face.getHeight() / 2.0f);
        float left = x - xOffset;
        float top = y - yOffset;
        float right = x + xOffset;
        float bottom = y + yOffset;
        canvas.drawRect(left, top, right, bottom, mBoxPaint);

        final ScrollView mScrollView=(ScrollView)((Activity)mContext).findViewById(R.id.scrollView);

        mScrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        }, 600);

    }

    private String getPrediction(float eulerY, float eulerZ) {
        String feature="";
        if(eulerZ<5f && eulerZ >=0f){
            if(eulerY>0f && eulerY<60f){
                feature="Twarz skierowana przed siebie";
            }else{
                feature="Brak odchylenia";
            }
        }else if(eulerZ>5f && eulerZ<45f){
            if(eulerY>0f && eulerY<=60f){
                feature="Twarz skierowana lekko w górę";
            }else {
                feature="Twarz lekko przechylona w prawo";
            }
        }else if(eulerZ>45f){
            if(eulerY>60f && eulerY!=0){
                feature="Twarz skierowana w górę";
            }else{
                feature="Twarz przechylona w prawo";
            }
        }else if(eulerZ<0f && eulerZ >-5f){
            if(eulerY>-60f && eulerY!=0){
                feature="Twarz skierowana w prawo";
            }else{
                feature="Brak odchylenia";
            }
        }else if(eulerZ<-5f && eulerZ>-45f){
            if(eulerY>-60f && eulerY!=0){
                feature="Twarz skierowana w górę";
            }else{
                feature="Twarz przechylona lekko w lewo";
            }
        }else{
            if(eulerY>-6f && eulerY!=0){
                feature="Twarz skierowana w górę";
            }else{
                feature="Twarz przechylona w lewo";
            }
        }

        return feature;
    }

    private String getUpdates(){
        String update;
        boolean smiling = mFace.getIsSmilingProbability() > SMILING_PROB_THRESHOLD;

        boolean leftEyeClosed = mFace.getIsLeftEyeOpenProbability() < EYE_OPEN_PROB_THRESHOLD;
        boolean rightEyeClosed = mFace.getIsRightEyeOpenProbability() < EYE_OPEN_PROB_THRESHOLD;
        if(smiling) {
            if (leftEyeClosed && !rightEyeClosed) {
                update="Left Wink";
            }  else if(rightEyeClosed && !leftEyeClosed){
                update = "Right WInk";
            } else if (leftEyeClosed){
                update = "Closed Eye Smile";
            } else {
                update = "Uśmiech";
            }
        } else {
            if (leftEyeClosed && !rightEyeClosed) {
                update = "Left Wink Frawn";
            }  else if(rightEyeClosed && !leftEyeClosed){
                update = "Right Wink Frawn";
            } else if (leftEyeClosed){
                update = "Closed Eye Frawn";
            } else {
                update = "Frawn";
            }
        }

        return update;

    }
}
