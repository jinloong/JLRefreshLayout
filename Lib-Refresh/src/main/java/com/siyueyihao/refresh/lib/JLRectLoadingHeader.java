package com.siyueyihao.refresh.lib;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义矩阵加载动画
 * @author zhujinlong@ichoice.com
 */

public class JLRectLoadingHeader extends View implements JLRefreshLayout.JLRefreshHeader{

    private List<RectModel> rectModels;
    private Paint paint;
    private float progress, r;
    private ValueAnimator animator;

    public Paint getPaint() {
        if(paint == null){
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
        return paint;
    }

    public JLRectLoadingHeader(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(
                (int) (75 * getContext().getResources().getDisplayMetrics().density + 0.5f),MeasureSpec.EXACTLY));
    }

    private void initRectModels(float progress){
        this.progress = progress;
        if(rectModels == null){
            this.r = 4 * getContext().getResources().getDisplayMetrics().density + 0.5f;
            float x = getMeasuredWidth() * 0.5f;
            float y = getMeasuredHeight() * 0.5f;
            rectModels = new ArrayList<>();
            rectModels.add(new RectModel(x - 2.1f * r, y - 2.1f * r, 0));
            rectModels.add(new RectModel(x, y - 2.1f * r, 1));
            rectModels.add(new RectModel(x - 2.1f * r, y, 1));
            rectModels.add(new RectModel(x + 2.1f * r, y - 2.1f * r, 2));
            rectModels.add(new RectModel(x,y, 2));
            rectModels.add(new RectModel(x - 2.1f * r, y + 2.1f * r, 2));
            rectModels.add(new RectModel(x + 2.1f * r, y, 3));
            rectModels.add(new RectModel(x, y + 2.1f * r, 3));
            rectModels.add(new RectModel(x + 2.1f * r, y + 2.1f * r, 4));
        }
        for (int i = 0; i < rectModels.size(); i++) {
            rectModels.get(i).setProgress(progress);
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(rectModels != null){
            for (int i = 0; i < rectModels.size(); i++) {
                getPaint().setAlpha((int) (rectModels.get(i).getProgress() * 255));
                canvas.drawPath(rectModels.get(i).getPath(),paint);
            }
        }
    }

    @Override
    public void reset() {
        if(animator != null && animator.isRunning()){
            animator.cancel();
        }
        getPaint().setColor(0xff795548);
        initRectModels(0);
    }

    @Override
    public void moving(int offset, int headerHeight, boolean activated) {
        initRectModels(Math.max(0, Math.min(1, (2 * offset - headerHeight * 1f) / headerHeight)));
    }

    @Override
    public void refreshing() {
        if(animator == null){
            animator = ValueAnimator.ofFloat(0);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.setDuration(800);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if(getContext() != null){
                        initRectModels((float) animation.getAnimatedValue() % 1);
                    }
                }
            });
        }
        animator.setFloatValues(progress, progress + 1);
        animator.start();
    }

    @Override
    public void refreshOver(boolean success) {
        if(animator != null && animator.isRunning()){
            animator.cancel();
        }
        getPaint().setColor(success ? 0xff009688 : 0xffe51c23);
        initRectModels(1);
    }

    private class RectModel{

        private float px , py, progress;
        private Path path;
        private int index;

        RectModel(float px, float py, int index) {
            this.px = px;
            this.py = py;
            this.index = index;
        }

        void setProgress(float progress) {
            this.progress = Math.min(1, Math.max(0, 5 * progress - index));
        }

        float getProgress() {
            return progress;
        }

        Path getPath() {
            if(path == null){
                path = new Path();
            }
            path.reset();
            float x = px - r * progress;
            float y = py - r * progress;
            path.moveTo(x,y);
            x = px + r * progress;
            y = py - r * progress;
            path.lineTo(x,y);
            x = px + r * progress;
            y = py + r * progress;
            path.lineTo(x,y);
            x = px - r * progress;
            y = py + r * progress;
            path.lineTo(x,y);
            path.close();
            return path;
        }
    }
}