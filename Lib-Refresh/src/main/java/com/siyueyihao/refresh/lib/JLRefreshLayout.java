package com.siyueyihao.refresh.lib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * 下拉刷新(扩展 RecyclerView 分页)
 * @author zhujinlong@ichoice.com
 */
public class JLRefreshLayout extends FrameLayout {

    // 头部动画的默认时间
    public static final int DEFAULT_DURATION = 300;
    // 隐藏的状态
    private static final int HIDE = 0;
    // 下拉刷新的状态
    private static final int PULL_TO_REFRESH = 1;
    // 松开刷新的状态
    private static final int RELEASE_TO_REFRESH = 2;
    // 正在刷新的状态
    private static final int REFRESHING = 3;
    // 刷新结束,准备隐藏的状态
    private static final int REFRESH_FINISHED = 4;
    // 当前状态
    private int mCurrentState = HIDE;

    private JLRefreshHeader refreshHeader;
    private View refreshContent;
    // 头部高度
    private int headerHeight;
    // 最小滑动响应距离
    private int mScaledTouchSlop;
    // 记录上次的X, Y坐标
    private float mLastMotionX;
    private float mLastMotionY;
    // 记录一开始的X, Y坐标
    private float mInitDownX;
    private float mInitDownY;
    // 响应的手指
    private int mActivePointerId;
    // 是否在处理头部
    private boolean mIsHeaderHandling;
    private boolean mIsMoveHappened;
    // 用于判断是否响应在刷新的状态下的点击事件
    private boolean mIsDragBeyondLimit;
    private int mInitActivePointerId;
    // 值动画，由于头部显示隐藏
    private ValueAnimator refreshHeaderAnimator;
    // 刷新的监听器
    private OnRefreshListener mOnRefreshListener;

    public JLRefreshLayout(Context context) {
        this(context, null);
    }

    public JLRefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JLRefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        View headView = getHeader(context);
        if(headView instanceof JLRefreshHeader){
            refreshHeader = (JLRefreshHeader) headView;
            addView(headView, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }
        initAnimator();
    }

    /**
     * 获取下拉刷新头部
     */
    protected View getHeader(Context context){
        return new JLPointerHeader(context);
    }

    private void initAnimator() {
        if(refreshHeaderAnimator == null){
            MyAnimatorListener listener = new MyAnimatorListener();
            refreshHeaderAnimator = ValueAnimator.ofInt(0);
            refreshHeaderAnimator.setInterpolator(new DecelerateInterpolator());
            refreshHeaderAnimator.addUpdateListener(listener);
            refreshHeaderAnimator.addListener(listener);
        }
    }

    private void ensureTarget(){
        if(refreshContent == null && getChildCount() == 2){
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if(!child.equals(refreshHeader)){
                    refreshContent = child;
                    break;
                }
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        ensureTarget();
        if (refreshContent == null || refreshHeader == null) {
            return;
        }
        final int width = getMeasuredWidth();
        final int height = getMeasuredHeight();
        int headerHeight = ((View)refreshHeader).getMeasuredHeight();
        if(this.headerHeight <= 0 && headerHeight > 0){
            this.headerHeight = headerHeight;
        }
        int top = ((View)refreshHeader).getPaddingTop();
        final int childLeft = getPaddingLeft();
        final int childTop = getPaddingTop();
        final int childWidth = width - getPaddingLeft() - getPaddingRight();
        final int childHeight = height - getPaddingTop() - getPaddingBottom();
        refreshContent.layout(childLeft, top + childTop, childLeft + childWidth, top + childTop + childHeight);
        ((View)refreshHeader).layout(childLeft, top - headerHeight, childLeft + childWidth, top);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 设置长点击或者短点击都能消耗事件，要不这样做，若子 view 都不消耗，最终点击事件会被它的上级消耗掉，后面一系列的事件都只给它的上级处理了
        setLongClickable(true);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (refreshHeaderAnimator != null && refreshHeaderAnimator.isRunning()) {
            refreshHeaderAnimator.removeAllUpdateListeners();
            refreshHeaderAnimator.removeAllListeners();
            refreshHeaderAnimator.cancel();
        }
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent event) {
        ensureTarget();
        if(mOnRefreshListener == null || !mOnRefreshListener.checkCanRefresh() || headerHeight <= 0 || refreshHeaderAnimator == null){
            // 禁止下拉刷新，直接把事件分发
            return super.dispatchTouchEvent(event);
        }
        if (!isEnabled() || refreshContent == null || refreshHeader == null || refreshHeaderAnimator.isRunning()) {
            // 不处理, 不分发
            return true;
        }
        // 支持多指触控
        int actionMasked = MotionEventCompat.getActionMasked(event);
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                // 记录响应的手指
                mActivePointerId = event.getPointerId(0);
                // 记录初始Y坐标
                mInitDownY = mLastMotionY = event.getY(0);
                mInitDownX = mLastMotionX = event.getX(0);
                mInitActivePointerId = mActivePointerId;
            }
            break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                // 另外一根手指按下，切换到这个手指响应
                int pointerDownIndex = MotionEventCompat.getActionIndex(event);
                if (pointerDownIndex < 0) {
                    Log.e("RefreshLayout", "Got ACTION_POINTER_DOWN event but have an invalid action index.");
                    return super.dispatchTouchEvent(event);
                }
                mActivePointerId = event.getPointerId(pointerDownIndex);
                mLastMotionY = event.getY(pointerDownIndex);
                mLastMotionX = event.getX(pointerDownIndex);
            }
            break;
            case MotionEvent.ACTION_POINTER_UP: {
                // 另外一根手指抬起，切换回其他手指响应
                final int pointerUpIndex = MotionEventCompat.getActionIndex(event);
                final int pointerId = event.getPointerId(pointerUpIndex);
                if (pointerId == mActivePointerId) {
                    // 抬起手指就是之前控制滑动手指，切换其他手指响应
                    final int newPointerIndex = pointerUpIndex == 0 ? 1 : 0;
                    mActivePointerId = event.getPointerId(newPointerIndex);
                } else if (mInitActivePointerId == pointerId && mIsDragBeyondLimit) {
                    // 防止第一个手指抬起时候响应点击事件
                    event.setAction(MotionEvent.ACTION_CANCEL);
                }
                try {
                    mLastMotionY = event.getY(event.findPointerIndex(mActivePointerId));
                    mLastMotionX = event.getX(event.findPointerIndex(mActivePointerId));
                } catch (IllegalArgumentException e) {
                    Log.e("RefreshLayout", e.getMessage());
                    mActivePointerId = event.getPointerId(MotionEventCompat.getActionIndex(event));
                    mLastMotionY = event.getY(event.findPointerIndex(mActivePointerId));
                    mLastMotionX = event.getX(event.findPointerIndex(mActivePointerId));
                }
            }
            break;
            case MotionEvent.ACTION_MOVE: {
                // 移动事件
                if (mActivePointerId == ViewDragHelper.INVALID_POINTER) {
                    Log.e("RefreshLayout","Got ACTION_MOVE event but don't have an active pointer id.");
                    return super.dispatchTouchEvent(event);
                }
                float y;
                float x;
                try {
                    y = event.getY(event.findPointerIndex(mActivePointerId));
                    x = event.getX(event.findPointerIndex(mActivePointerId));
                } catch (IllegalArgumentException e) {
                    Log.e("RefreshLayout", e.getMessage());
                    mActivePointerId = event.getPointerId(MotionEventCompat.getActionIndex(event));
                    mLastMotionY = event.getY(event.findPointerIndex(mActivePointerId));
                    mLastMotionX = event.getX(event.findPointerIndex(mActivePointerId));
                    y = event.getY(event.findPointerIndex(mActivePointerId));
                    x = event.getX(event.findPointerIndex(mActivePointerId));
                }
                // 移动的偏移量
                float yDiff = y - mLastMotionY;
                float xDiff = x - mLastMotionX;
                mLastMotionY = y;
                mLastMotionX = x;
                // 下拉,且 content 到顶了; 或者 上拉,且头部可见
                if ((!canChildScrollUp() && yDiff > 0) || (yDiff < 0 && ((View)refreshHeader).getPaddingTop() > 0)) {
                    // 滑动的总距离
                    float totalDistanceY = y - mInitDownY;
                    if (Math.abs(totalDistanceY) >= mScaledTouchSlop * 3) {
                        mIsDragBeyondLimit = true;
                    }
                    if (Math.abs(xDiff) * 2 > Math.abs(yDiff) || (yDiff > 0 && totalDistanceY <= mScaledTouchSlop)) {
                        // 下拉时，优化滑动逻辑，不要稍微一点位移就响应
                        return super.dispatchTouchEvent(event);
                    }
                    // 处理下拉头部 计算出移动后的头部位置(阻尼3)
                    moveSpinner(Math.max((int) (yDiff * 0.5f + ((View)refreshHeader).getPaddingTop()), 0));
                    // 正在处理事件
                    mIsHeaderHandling = true;
                    mIsMoveHappened = true;
                    break;
                } else if (mIsHeaderHandling && yDiff < 0) {
                    // 在头部隐藏的那一瞬间的事件特殊处理
                    event.setAction(MotionEvent.ACTION_DOWN);
                    mIsHeaderHandling = false;
                }
            }
            break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                // 处理手指抬起或取消事件
                if(actionMasked == MotionEvent.ACTION_UP){
                    finishSpinner();
                }
                if (mIsMoveHappened && actionMasked == MotionEvent.ACTION_UP) {
                    if (mCurrentState != REFRESHING || mIsDragBeyondLimit) {
                        event.setAction(MotionEvent.ACTION_CANCEL);
                    } else {
                        // 正在刷新的时候一个事件序列，并且拖动没超过范围，可认为点击产生了
                        // 由于之前有取消事件，这里模拟点击
                        event.setAction(MotionEvent.ACTION_DOWN);
                        super.dispatchTouchEvent(event);
                        event.setAction(MotionEvent.ACTION_UP);
                        return super.dispatchTouchEvent(event);
                    }
                }
                mInitActivePointerId = mActivePointerId = ViewDragHelper.INVALID_POINTER;
                mIsMoveHappened = false;
                mIsHeaderHandling = false;
                mIsDragBeyondLimit = false;
                mInitDownY = 0;
                mInitDownX = 0;
            }
            break;
            default:
                break;
        }
        if (((View)refreshHeader).getPaddingTop() > 0 && !refreshHeaderAnimator.isRunning() && actionMasked == MotionEvent.ACTION_MOVE) {
            // 头部在显示， 并且不是在执行动画，并且是移动事件，取消
            event.setAction(MotionEvent.ACTION_CANCEL);
        }
        return super.dispatchTouchEvent(event);
    }

    private boolean canChildScrollUp() {
        if (mOnRefreshListener != null && mOnRefreshListener.getRealScrollView() != null) {
            return ViewCompat.canScrollVertically(mOnRefreshListener.getRealScrollView(), -1);
        }
        return ViewCompat.canScrollVertically(refreshContent, -1);
    }

    /**
     * 开始刷新
     */
    public void startRefresh(){
        ensureTarget();
        if(mOnRefreshListener == null || refreshHeaderAnimator == null
                || !isEnabled() || refreshContent == null || refreshHeader == null){
            return ;
        }
        if(mCurrentState == HIDE && mOnRefreshListener.checkCanRefresh() && !refreshHeaderAnimator.isRunning()){
            if(headerHeight <= 0){
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(headerHeight > 0){
                            mCurrentState = RELEASE_TO_REFRESH;
                            startAnimator(((View)refreshHeader).getPaddingTop(), headerHeight, 400);
                        }
                    }
                },100);
            }else {
                mCurrentState = RELEASE_TO_REFRESH;
                startAnimator(((View)refreshHeader).getPaddingTop(), headerHeight, 400);
            }
        }
    }

    /**
     * 结束刷新
     * @param success 成功或失败
     */
    public void stopRefresh(boolean success) {
        ensureTarget();
        if(mOnRefreshListener == null || refreshHeaderAnimator == null
                || !isEnabled() || refreshContent == null || refreshHeader == null){
            return ;
        }
        if(mCurrentState == REFRESHING){
            if(((View)refreshHeader).getPaddingTop() > 0){
                mCurrentState = REFRESH_FINISHED;
                refreshHeader.refreshOver(success);
                startAnimator(((View)refreshHeader).getPaddingTop(), 0, DEFAULT_DURATION);
            }else {
                mCurrentState = HIDE;
                refreshHeader.reset();
            }
        }
    }

    private void moveSpinner(int offset) {
        ((View)refreshHeader).setPadding(0, offset, 0, 0);
        if(mCurrentState == REFRESHING || mCurrentState == REFRESH_FINISHED){
            return;
        }
        int top = ((View)refreshHeader).getPaddingTop();
        if(top >= headerHeight){
            mCurrentState = RELEASE_TO_REFRESH;
            refreshHeader.moving(top,headerHeight,true);
        }else if(top > 0){
            mCurrentState = PULL_TO_REFRESH;
            refreshHeader.moving(top,headerHeight,false);
        }else if(mCurrentState != HIDE){
            mCurrentState = HIDE;
            refreshHeader.reset();
        }
    }

    private void finishSpinner() {
        int top = ((View)refreshHeader).getPaddingTop();
        if(mCurrentState == RELEASE_TO_REFRESH){
            startAnimator(top, headerHeight, DEFAULT_DURATION);
        }else if(mCurrentState == PULL_TO_REFRESH){
            startAnimator(top, 0, DEFAULT_DURATION * top / headerHeight);
        }else if(mCurrentState == REFRESHING){
            if(top > headerHeight){
                startAnimator(top, headerHeight, DEFAULT_DURATION);
            }else if(top < headerHeight){
                startAnimator(top, 0, DEFAULT_DURATION * top / headerHeight);
            }
        }
    }

    private void startAnimator(int fromValue, int toValue, long duration){
        initAnimator();
        refreshHeaderAnimator.setIntValues(fromValue,toValue);
        refreshHeaderAnimator.setDuration(duration);
        refreshHeaderAnimator.start();
    }

    private class MyAnimatorListener extends AnimatorListenerAdapter implements ValueAnimator.AnimatorUpdateListener{

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (getContext() != null) {
                // 若是退出Activity了，动画结束不必执行头部动作
                moveSpinner((Integer) animation.getAnimatedValue());
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            if (getContext() != null) {
                // 若是退出Activity了，动画结束不必执行头部动作
                if(mCurrentState == RELEASE_TO_REFRESH){
                    mCurrentState = REFRESHING;
                    refreshHeader.refreshing();
                    if (mOnRefreshListener != null) {
                        mOnRefreshListener.onRefresh();
                    }
                }else if(mCurrentState == REFRESH_FINISHED){
                    mCurrentState = HIDE;
                    refreshHeader.reset();
                }
            }
        }
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }

    public abstract static class OnRefreshListener {

        protected abstract void onRefresh();

        protected View getRealScrollView(){
            return null;
        }

        protected boolean checkCanRefresh(){
            return true;
        }

        protected void loadMore() {}
    }

    /**
     * 将下拉刷新头部操作定义为接口,方便自定义
     */
    public interface JLRefreshHeader{

        void reset();

        void moving(int offset, int headerHeight, boolean activated);

        void refreshing();

        void refreshOver(boolean success);
    }

    /******************************以下是为 Recyclerview 扩展的分页功能*******************************/

    private Adapter adapter;

    /**
     * 加载完毕
     * @param hasMore 是否还有下一页
     */
    public void loadComplete(boolean hasMore){
        if(adapter == null){
            throw new RuntimeException("must call method setAdapter to bind data");
        }
        adapter.setState(hasMore ? Adapter.STATE_MORE : Adapter.STATE_END);
    }

    /**
     * 加载出错
     */
    public void loadError(){
        if(adapter == null){
            throw new RuntimeException("must call method setAdapter to bind data");
        }
        adapter.setState(Adapter.STATE_ERROR);
    }

    public boolean checkRefreshing(){
        return mCurrentState == HIDE || (mCurrentState == REFRESH_FINISHED && refreshHeaderAnimator != null && !refreshHeaderAnimator.isRunning());
    }

    /**
     * 需要通过此方法设置适配器, 才支持 RecyclerView 分页功能,
     */
    public void setAdapter(@NonNull RecyclerView recyclerView, @NonNull Adapter mAdapter) {
        adapter = mAdapter;
        recyclerView.setAdapter(adapter);
        adapter.setHandler(mOnRefreshListener);
        adapter.setRefreshLayout(this);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (mOnRefreshListener != null
                        && checkRefreshing()
                        && (adapter.getState() == Adapter.STATE_MORE || adapter.getState() == Adapter.STATE_ERROR)
                        && newState == RecyclerView.SCROLL_STATE_IDLE
                        && !ViewCompat.canScrollVertically(recyclerView, 1)
                        ) {
                    adapter.setState(Adapter.STATE_LOAIND);
                    mOnRefreshListener.loadMore();
                }
            }
        });
    }

    /**
     * 支持加载更多的代理适配器
     */
    public static abstract class Adapter extends RecyclerView.Adapter {

        static final int STATE_MORE = 0, STATE_LOAIND = 1, STATE_END = 2, STATE_ERROR = 3;
        int state = STATE_MORE;
        OnRefreshListener handler;
        JLRefreshLayout refreshLayout;

        public void setHandler(OnRefreshListener handler) {
            if(this.handler != null && handler != null){
                this.handler = handler;
            }
        }

        public void setRefreshLayout(JLRefreshLayout refreshLayout) {
            this.refreshLayout = refreshLayout;
        }

        public void setState(int state) {
            if (this.state != state) {
                this.state = state;
                notifyItemChanged(getItemCount() - 1);
            }
        }

        public int getState() {
            return state;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == getItemCount() - 1) {
                return -99;
            }
            return getItemType(position);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == -99) {
                // 分页控件
                return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.loadmore_default_footer, parent, false)) {
                };
            } else {
                return onCreateItemHolder(parent, viewType);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (getItemViewType(position) == -99) {
                LinearLayout layoutContent = (LinearLayout) holder.itemView.findViewById(R.id.layout_content);
                ProgressBar progressBar = (ProgressBar) holder.itemView.findViewById(R.id.loadmore_default_footer_progressbar);
                TextView textView = (TextView) holder.itemView.findViewById(R.id.loadmore_default_footer_tv);
                if (state == STATE_END) {
                    layoutContent.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    textView.setText("没有更多了");
                } else if (state == STATE_MORE) {
                    layoutContent.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    textView.setText("点击加载");
                } else if (state == STATE_LOAIND) {
                    layoutContent.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);
                    textView.setText("加载中...");
                } else if (state == STATE_ERROR) {
                    layoutContent.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                    textView.setText("加载失败,点击重新加载");
                }
                holder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (handler != null && refreshLayout != null && refreshLayout.checkRefreshing() && (state == STATE_MORE || state == STATE_ERROR)) {
                            setState(STATE_LOAIND);
                            handler.loadMore();
                        }
                    }
                });
            } else {
                onBindItemHolder(holder,position);
            }
        }

        @Override
        public int getItemCount() {
            return getCount() == 0 ? 0 : getCount() + 1;
        }

        public int getItemType(int position){
            return super.getItemViewType(position);
        }

        public abstract RecyclerView.ViewHolder onCreateItemHolder(ViewGroup parent, int viewType);

        public abstract void onBindItemHolder(RecyclerView.ViewHolder holder, int position);

        public abstract int getCount();

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            // 处理瀑布流模式 最后的 item 占整行
            if (holder.getLayoutPosition() == getItemCount() - 1) {
                ViewGroup.LayoutParams lp = holder.itemView.getLayoutParams();
                if (lp != null && lp instanceof StaggeredGridLayoutManager.LayoutParams) {
                    StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) lp;
                    p.setFullSpan(true);
                }
            }
        }

        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            // 处理网格布局模式 最后的 item 占整行
            final RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof GridLayoutManager) {
                GridLayoutManager gridManager = ((GridLayoutManager) layoutManager);
                final GridLayoutManager.SpanSizeLookup spanSizeLookup = gridManager.getSpanSizeLookup();
                final int lastSpanCount = gridManager.getSpanCount();
                gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        return position == getItemCount() - 1 ? lastSpanCount :
                                (spanSizeLookup == null ? 1 : spanSizeLookup.getSpanSize(position));
                    }
                });
            }
        }
    }
}