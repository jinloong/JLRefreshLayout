package com.siyueyihao.refresh.lib;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * 自定义 progress 加载动画
 * @author zhujinlong@ichoice.com
 */

public class JLProgressHeader extends View implements JLRefreshLayout.JLRefreshHeader{

    private Paint paint;
    private RectF rectW, rectN;
    private float x, y, angleW, angleN, startN, pX1, pX2, pY;
    private int currentState, alphaW;
    private ValueAnimator animator;

    public Paint getPaint() {
        if(paint == null){
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize((int) (12 * getContext().getResources().getDisplayMetrics().scaledDensity + 0.5f));
            paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC));
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
        return paint;
    }

    public JLProgressHeader(Context context) {
        super(context);
        setBackgroundColor(0xfffff000);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(
                (int) (65 * getContext().getResources().getDisplayMetrics().density + 0.5f),MeasureSpec.EXACTLY));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.x = w * 0.5f;
        if(y == 0){
            Paint.FontMetrics fontMetrics = getPaint().getFontMetrics();
            y = (h - fontMetrics.ascent - fontMetrics.descent) * 0.5f;
        }
        if(rectW == null){
            rectW = new RectF(w * 0.25f - h * 0.2f, h * 0.3f, w * 0.25f + h * 0.2f, h * 0.7f);
        }
        if(rectN == null){
            rectN = new RectF(w * 0.25f - h * 0.12f, h * 0.38f, w * 0.25f + h * 0.12f, h * 0.62f);
        }
        pX1 = w * 0.25f - h * 0.1f;
        pX2 = w * 0.25f + h * 0.1f;
        pY =  h * 0.4f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        getPaint().setColor(0xff808080);
        paint.setStyle(Paint.Style.FILL);
        paint.setAlpha(255);
        canvas.drawText(currentState == 4 ? "加载完毕" : (currentState == 3 ? "正在加载" : ( currentState == 2 ? "松开刷新" : "下拉刷新")), x, y, paint);
        if(currentState != 4){
            paint.setStrokeWidth(5);
            paint.setColor(0xffff4081);
            paint.setAlpha(alphaW);
            canvas.drawArc(rectW, -90, angleW, true, paint);

            paint.setColor(0xffffffff);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAlpha(255);
            canvas.drawArc(rectN, startN, angleN, false, paint);
        }else {
            paint.setColor(0xffff4081);
            paint.setStrokeWidth(10);
            canvas.drawPoint(pX1,pY,paint);
            canvas.drawPoint(pX2,pY,paint);

            paint.setStrokeWidth(5);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawArc(rectN, 0, 180, false, paint);
        }
    }

    @Override
    public void reset() {
        if(animator != null && animator.isRunning()){
            animator.cancel();
        }
        alphaW = 0;
        angleW = 0;
        angleN = 0;
        startN = -90;
        currentState = 0;
        postInvalidate();
    }

    @Override
    public void moving(int offset, int headerHeight, boolean activated) {
        float pw = offset * 1f / (headerHeight * 0.8f);
        pw = Math.max(0.5f, Math.min(1, pw));
        alphaW = (int) (pw * 510 - 255);
        angleW = pw * 720 - 360;

        float pn = Math.max(0, offset * 5f / headerHeight - 4);
        angleN = Math.min(180, pn * 360);
        startN = 90 * pn - 90;

        currentState = activated ? 2 : 1;
        postInvalidate();
    }

    @Override
    public void refreshing() {
        currentState = 3;
        if(animator == null){
            animator = ValueAnimator.ofFloat(0);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.setDuration(600);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    startN = (float) animation.getAnimatedValue();
                    postInvalidate();
                }
            });
        }
        animator.setFloatValues(startN, startN - 360);
        animator.start();
    }

    @Override
    public void refreshOver(boolean success) {
        if(animator != null && animator.isRunning()){
            animator.cancel();
        }
        currentState = 4;
        postInvalidate();
    }
}