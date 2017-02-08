package com.siyueyihao.refresh.lib;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义 双圆形环绕 加载动画
 * @author zhujinlong@ichoice.com
 */

public class JLPointerHeader extends View implements JLRefreshLayout.JLRefreshHeader {

    private Paint paint;
    private ValueAnimator animator;
    private boolean refreshing;
    private List<float[]> points;
    private List<float[]> circles;

    public Paint getPaint() {
        if(paint == null){
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xfff25555);
            paint.setStrokeWidth(5);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
        return paint;
    }

    public JLPointerHeader(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(
                (int) (60 * getContext().getResources().getDisplayMetrics().density + 0.5f),MeasureSpec.EXACTLY));
    }

    private void initPoints(float progress){
        if(animator != null && animator.isRunning()){
            animator.cancel();
        }
        if(points == null){
            points = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                points.add(new float[2]);
            }
        }
        float r = 10 * getContext().getResources().getDisplayMetrics().density + 0.5f;
        float r2 = r * 0.5f;
        float x = getMeasuredWidth() * 0.5f;
        float y = getMeasuredHeight() * 0.5f + (1 - progress * 2) * r;
        float sin = (float) Math.sin(progress * 2 * Math.PI);
        float cos = (float) Math.cos(progress * 2 * Math.PI);
        points.get(0)[0] = x - r * sin;
        points.get(0)[1] = y + r * cos;
        points.get(1)[0] = x + r * sin;
        points.get(1)[1] = y - r * cos;
        points.get(2)[0] = points.get(0)[0]  + r2 * (float) Math.cos((45 - progress * 360) * Math.PI / 180);
        points.get(2)[1] = points.get(0)[1]  - r2 * (float) Math.sin((45 - progress * 360) * Math.PI / 180);
        points.get(3)[0] = points.get(0)[0]  - r2 * (float) Math.sin((45 - progress * 360) * Math.PI / 180);
        points.get(3)[1] = points.get(0)[1]  - r2 * (float) Math.cos((45 - progress * 360) * Math.PI / 180);
        postInvalidate();
    }

    private void initProgress(float progress){
        if(circles == null){
            circles = new ArrayList<>();
            circles.add(new float[3]);
            circles.add(new float[3]);
        }
        float x = getMeasuredWidth() * 0.5f;
        float y = getMeasuredHeight() * 0.5f;
        float r = 10 * getContext().getResources().getDisplayMetrics().density + 0.5f;
        float sin = (float) Math.sin(progress * 2 * Math.PI);
        float cos = (float) Math.cos(progress * 2 * Math.PI);
        if(progress <= 0.5f){
            progress *= 2;
        }else {
            progress = 2 - 2 * progress;
        }
        circles.get(0)[0] = x - r * sin * (1.1f - progress);
        circles.get(0)[1] = y + r * cos * (1.1f - progress);
        circles.get(0)[2] = r * progress;
        circles.get(1)[0] = x + r * sin * (0.1f + progress);
        circles.get(1)[1] = y - r * cos * (0.1f + progress);
        circles.get(1)[2] = r * (1 - progress);
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(refreshing){
            if(circles != null){
                for (int i = 0; i < circles.size(); i++) {
                    canvas.drawCircle(circles.get(i)[0], circles.get(i)[1], circles.get(i)[2],getPaint());
                }
            }
        }else {
            if(points != null){
                for (int i = 1; i < points.size(); i++) {
                    canvas.drawLine(points.get(i)[0],points.get(i)[1],points.get(0)[0],points.get(0)[1],getPaint());
                }
            }
        }
    }

    @Override
    public void reset() {
        refreshing = false;
        initPoints(1);
    }

    @Override
    public void moving(int offset, int headerHeight, boolean activated) {
        initPoints(Math.max(0, Math.min(1f, (2f * offset - headerHeight) / headerHeight)) * 0.5f);
        //initPoints(Math.max(0, Math.min(1f, offset/ headerHeight)));
    }

    @Override
    public void refreshing() {
        refreshing = true;
        if(animator == null){
            animator = ValueAnimator.ofFloat(0,1);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.setDuration(2500);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if(getContext() != null){
                        initProgress((float) animation.getAnimatedValue());
                    }
                }
            });
        }
        animator.start();
    }

    @Override
    public void refreshOver(boolean success) {
        refreshing = false;
        initPoints(1);
    }
}