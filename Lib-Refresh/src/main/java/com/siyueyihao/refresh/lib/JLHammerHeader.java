package com.siyueyihao.refresh.lib;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.View;

/**
 * 仿 锤子 加载动画
 * @author zhujinlong@ichoice.com
 */

public class JLHammerHeader extends View implements JLRefreshLayout.JLRefreshHeader{

    private Paint paint;
    private float progress, r, sin30, sin60;
    private ValueAnimator animator;
    private float[][] params;

    public Paint getPaint() {
        if(paint == null){
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
        return paint;
    }

    public JLHammerHeader(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(
                (int) (75 * getContext().getResources().getDisplayMetrics().density + 0.5f),MeasureSpec.EXACTLY));
    }

    private void init(float progress){
        if(params == null){
            params = new float[8][2];
            for (int i = 0; i < params.length; i++) {
                params[i] = new float[2];
            }
        }
        float r = 6 * getContext().getResources().getDisplayMetrics().density + 0.5f;
        float r2 = 0.3f * r;
        float l = (float) (Math.PI / 1.5f * r);
        float w = getMeasuredWidth();
        float h = getMeasuredHeight();
        Log.e(">>>",progress +"_______________" + r +"______________" +l);
        if(progress <= 0.5f){
            params[0][0] = 0.25f * w - r;
            params[0][1] = 2 * progress * (0.5f * h - l);
            params[1][0] = params[0][0];
            params[1][1] = 2 * progress * (0.5f * h - l) + l;
            params[2][0] = (float) (params[1][0] - r2 * Math.sin(45 * Math.PI / 180));
            params[2][1] = (float) (params[1][1] - r2 * Math.sin(45 * Math.PI / 180));

            params[4][0] = 0.25f * w + r;
            params[4][1] = h - 2 * progress * (0.5f * h - l);
            params[5][0] = params[4][0];
            params[5][1] = h - l - 2 * progress * (0.5f * h - l);
            params[6][0] = (float) (params[5][0] + r2 * Math.sin(45 * Math.PI / 180));
            params[6][1] = (float) (params[5][1] + r2 * Math.sin(45 * Math.PI / 180));
        }else{
            params[3][0] = 330 - 300 * progress;
            params[7][0] = 150 - 300 * progress;

            progress = Math.min(1, progress);

            params[3][1] = 300 * progress - 150;
            params[7][1] = 300 * progress - 150;
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(params != null){
            canvas.drawLine(params[0][0],params[0][1],params[1][0],params[1][1],getPaint());
            canvas.drawLine(params[2][0],params[2][1],params[1][0],params[1][1],getPaint());
            canvas.drawLine(params[4][0],params[4][1],params[5][0],params[5][1],getPaint());
            canvas.drawLine(params[6][0],params[6][1],params[5][0],params[5][1],getPaint());
            //canvas.drawArc(params[3][0]);
        }
    }

    @Override
    public void reset() {

    }

    @Override
    public void moving(int offset, int headerHeight, boolean activated) {
        init(Math.max(0, offset * 1f / headerHeight));
    }

    @Override
    public void refreshing() {

    }

    @Override
    public void refreshOver(boolean success) {

    }
}