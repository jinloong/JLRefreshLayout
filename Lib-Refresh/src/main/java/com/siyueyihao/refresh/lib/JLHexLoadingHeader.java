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
 * 仿守望先锋加载动画
 * @author zhujinlong@ichoice.com
 */

public class JLHexLoadingHeader extends View implements JLRefreshLayout.JLRefreshHeader{

    private List<HexModel> hexModels;
    private Paint paint;
    private float progress, r, sin30, sin60;
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

    public JLHexLoadingHeader(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(
                (int) (75 * getContext().getResources().getDisplayMetrics().density + 0.5f),MeasureSpec.EXACTLY));
    }

    private void initHexModels(float progress){
        this.progress = progress;
        if(hexModels == null){
            this.sin30 = (float) Math.sin(Math.PI / 6);
            this.sin60 = (float) Math.sin(Math.PI / 3);
            this.r = 6 * getContext().getResources().getDisplayMetrics().density + 0.5f;
            float x = getMeasuredWidth() * 0.5f;
            float y = getMeasuredHeight() * 0.5f;
            hexModels = new ArrayList<>();
            hexModels.add(new HexModel(x - r * (sin60 + 0.06f), y - r * (1.1f + sin30)));
            hexModels.add(new HexModel(x + r * (sin60 + 0.05f), y - r * (1.1f + sin30)));
            hexModels.add(new HexModel(x + r * (2 * sin60 + 0.1f), y));
            hexModels.add(new HexModel(x + r * (sin60 + 0.06f), y + r * (1.1f + sin30)));
            hexModels.add(new HexModel(x - r * (sin60 + 0.06f), y + r * (1.1f + sin30)));
            hexModels.add(new HexModel(x - r * (2 * sin60 + 0.1f), y));
            hexModels.add(new HexModel(x,y));
        }
        for (int i = 0; i < hexModels.size(); i++) {
            float p;
            if(progress < (1f + i) / 14){
                p = 14 * progress - i;
            }else if(progress < (7f + i) / 14){
                p = 1;
            }else if(progress < (8f + i) / 14){
                p = -14 * progress + 8f + i;
            }else {
                p = 0;
            }
            hexModels.get(i).setProgress(Math.min(1, Math.max(0, p)));
        }
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(hexModels != null){
            for (int i = 0; i < hexModels.size(); i++) {
                getPaint().setAlpha((int) (hexModels.get(i).getProgress() * 255));
                canvas.drawPath(hexModels.get(i).getPath(),paint);
            }
        }
    }

    @Override
    public void reset() {
        if(animator != null && animator.isRunning()){
            animator.cancel();
        }
        getPaint().setColor(0xff795548);
        initHexModels(0);
    }

    @Override
    public void moving(int offset, int headerHeight, boolean activated) {
        initHexModels(Math.max(0, Math.min(0.5f, (offset * 0.5f) / headerHeight)));
    }

    @Override
    public void refreshing() {
        if(animator == null){
            animator = ValueAnimator.ofFloat(0);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.RESTART);
            animator.setDuration(2000);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if(getContext() != null){
                        initHexModels((float) animation.getAnimatedValue() % 1);
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
        initHexModels(0.5f);
    }

    private class HexModel{

        private float px , py, progress;
        private Path path;

        HexModel(float px, float py) {
            this.px = px;
            this.py = py;
        }

        void setProgress(float progress) {
            this.progress = progress;
        }

        float getProgress() {
            return progress;
        }

        Path getPath() {
            if(path == null){
                path = new Path();
            }
            path.reset();
            float x = px - r * progress * sin60;
            float y = py - r * progress * sin30;
            path.moveTo(x,y);
            x = px;
            y = py - r * progress;
            path.lineTo(x,y);
            x = px + r * progress * sin60;
            y = py - r * progress * sin30;
            path.lineTo(x,y);
            x = px + r * progress * sin60;
            y = py + r * progress * sin30;
            path.lineTo(x,y);
            x = px;
            y = py + r * progress;
            path.lineTo(x,y);
            x = px - r * progress * sin60;
            y = py + r * progress * sin30;
            path.lineTo(x,y);
            path.close();
            return path;
        }
    }
}