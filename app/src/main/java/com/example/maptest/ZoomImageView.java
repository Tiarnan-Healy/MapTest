package com.example.maptest;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.appcompat.widget.AppCompatImageView;

public class ZoomImageView extends AppCompatImageView {
    private Matrix matrix = new Matrix();
    private float[] matrixValues = new float[9];

    private float scale = 1f;
    private float minScale = 0.25f;
    private float maxScale = 5f;

    private PointF lastTouch = new PointF();
    private PointF midPoint = new PointF();
    private int mode = NONE;

    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        matrix.setTranslate(0, 0);
        setImageMatrix(matrix);

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        gestureDetector = new GestureDetector(getContext(), new GestureListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        PointF currentTouch = new PointF(event.getX(), event.getY());

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                lastTouch.set(currentTouch);
                mode = DRAG;
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                midPoint = getMidPoint(event);
                mode = ZOOM;
                break;

            case MotionEvent.ACTION_MOVE:
                if (mode == DRAG) {
                    float dx = currentTouch.x - lastTouch.x;
                    float dy = currentTouch.y - lastTouch.y;

                    matrix.postTranslate(dx, dy);
                    limitDragBounds();  // Restrict scrolling to image bounds

                    lastTouch.set(currentTouch);
                }
                setImageMatrix(matrix);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mode = NONE;
                break;
        }
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float newScale = scale * scaleFactor;

            if (newScale >= minScale && newScale <= maxScale) {
                scale *= scaleFactor;
                matrix.postScale(scaleFactor, scaleFactor, midPoint.x, midPoint.y);
                limitDragBounds();  // Ensure the image remains within bounds after zooming
                setImageMatrix(matrix);
            }
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            matrix.postTranslate(-distanceX, -distanceY);
            limitDragBounds();  // Restrict movement beyond image edges
            setImageMatrix(matrix);
            return true;
        }
    }

    private PointF getMidPoint(MotionEvent event) {
        float x = (event.getX(0) + event.getX(1)) / 2;
        float y = (event.getY(0) + event.getY(1)) / 2;
        return new PointF(x, y);
    }

    private void limitDragBounds() {
        RectF bounds = getImageBounds();
        float deltaX = 0, deltaY = 0;

        // Check horizontal bounds
        if (bounds.left > 0) {
            deltaX = -bounds.left; // Pull left side back into place
        } else if (bounds.right < getWidth()) {
            deltaX = getWidth() - bounds.right; // Pull right side back
        }

        // Check vertical bounds
        if (bounds.top > 0) {
            deltaY = -bounds.top; // Pull top back
        } else if (bounds.bottom < getHeight()) {
            deltaY = getHeight() - bounds.bottom; // Pull bottom back
        }

        matrix.postTranslate(deltaX, deltaY);
    }

    private RectF getImageBounds() {
        Drawable drawable = getDrawable();
        if (drawable == null) return new RectF(0, 0, getWidth(), getHeight());

        matrix.getValues(matrixValues);
        float scaleX = matrixValues[Matrix.MSCALE_X];
        float scaleY = matrixValues[Matrix.MSCALE_Y];
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];

        float imageWidth = drawable.getIntrinsicWidth() * scaleX;
        float imageHeight = drawable.getIntrinsicHeight() * scaleY;

        float left = transX;
        float top = transY;
        float right = left + imageWidth;
        float bottom = top + imageHeight;

        return new RectF(left, top, right, bottom);
    }
    public void zoomTo(float newScale) {
        float scaleFactor = newScale / scale;  // Calculate scale factor
        scale = newScale;

        // Apply zoom towards the center
        matrix.postScale(scaleFactor, scaleFactor, getWidth() / 2, getHeight() / 2);

        limitDragBounds(); // Keep image within bounds
        setImageMatrix(matrix);
    }

    public void snapToPlayerLocation(float playerX, float playerY) {
        matrix.getValues(matrixValues);
        float scaleX = matrixValues[Matrix.MSCALE_X];
        float scaleY = matrixValues[Matrix.MSCALE_Y];

        // Convert player game coordinates to image pixels
        float playerXInPixels = playerX * scaleX;
        float playerYInPixels = playerY * scaleY;

        // Calculate translation needed to center on the player
        float dx = -playerXInPixels + getWidth() / 2;
        float dy = -playerYInPixels + getHeight() / 2;

        // Apply the transformation
        matrix.postTranslate(dx, dy);
        limitDragBounds();
        setImageMatrix(matrix);
    }

}
