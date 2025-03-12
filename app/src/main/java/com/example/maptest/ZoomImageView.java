package com.example.maptest;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import androidx.appcompat.widget.AppCompatImageView;

import java.util.ArrayList;
import java.util.List;

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

    private List<Node> nodes = new ArrayList<>(); // List of nodes to be drawn

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void init() {
        setScaleType(ScaleType.MATRIX);
        matrix.setTranslate(0, 0);
        setImageMatrix(matrix);

        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());
        gestureDetector = new GestureDetector(getContext(), new GestureListener());

        // Hardcoded test nodes (Replace this with actual database loading later)
        addNode(new Node(1, 291, 62));
        addNode(new Node(2, 745, 31));
        addNode(new Node(3, 1033, 39));
        addNode(new Node(4, 1209, 23));
        addNode(new Node(5, 1917, 46));
        addNode(new Node(6, 2136, 45));
        addNode(new Node(7, 2358, 56));

        // Add test nodes at extreme points
        addNode(new Node(8,1, 1));             // Top-left corner of the original map
        addNode(new Node(9,1000, 1));          // Top-right
        addNode(new Node(10, 1, 1000));          // Bottom-left
        addNode(new Node(11, 1000, 1000));       // Bottom-right
        addNode(new Node(12, 500, 500));         // Center (if the original map is 1000x1000)


        Log.d("ZoomImageView", "Nodes added: " + nodes.size());  // Debugging

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Drawable drawable = getDrawable();
        if (drawable == null) return;



        // Get the original image dimensions
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();

        Log.d("ZoomImageView", "Image IntrinsicWidth=" + intrinsicWidth + "Image InstrinsicHeight=" + intrinsicHeight);

        // Get the transformed image bounds
        RectF imageBounds = getImageBounds();

        Log.d("ZoomImageView", "Image Bounds: Left=" + imageBounds.left +
                ", Top=" + imageBounds.top +
                ", Width=" + imageBounds.width() +
                ", Height=" + imageBounds.height());

        float imageLeft = imageBounds.left;
        float imageTop = imageBounds.top;
        float imageWidth = 2517;
        float imageHeight = 1895;

        // Scaling factors based on the displayed image
        float scaleX = intrinsicWidth / imageWidth;
        float scaleY = intrinsicHeight / imageHeight;

        // Paint for transparent circles
        Paint transparentPaint = new Paint();
        transparentPaint.setColor(Color.BLACK);
        transparentPaint.setStyle(Paint.Style.FILL);
        transparentPaint.setAlpha(80);

        // Paint for node numbers
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(70);
        textPaint.setTextAlign(Paint.Align.CENTER);

        for (Node node : nodes) {
            // Correctly scale and position the nodes
            float transformedX = imageLeft + (node.getX() * scaleX);
            float transformedY = imageTop + (node.getY() * scaleY);

            // Debugging logs
            Log.d("ZoomImageView", "Node " + node.getNumber() + " -> Map(" + node.getX() + "," + node.getY() +
                    ") -> Image(" + transformedX + "," + transformedY + ")");

            // Draw transparent node
            canvas.drawCircle(transformedX - 124, transformedY, 100, transparentPaint);

            // Draw text above node
            canvas.drawText(String.valueOf(node.getNumber()), transformedX - 124, transformedY + 20, textPaint);
        }
    }






    // Method to add a node
    public void addNode(Node node) {
        nodes.add(node);
        invalidate(); // Redraw the view
    }

    // Method to remove a node
    public void removeNode(Node node) {
        nodes.remove(node);
        invalidate(); // Redraw the view
    }

    // Existing code to handle zooming, dragging, and other functionality...
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
