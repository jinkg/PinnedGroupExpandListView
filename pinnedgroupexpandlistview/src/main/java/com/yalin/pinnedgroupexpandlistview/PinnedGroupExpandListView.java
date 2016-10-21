package com.yalin.pinnedgroupexpandlistview;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

/**
 * 作者：YaLin
 * 日期：2016/8/2.
 */
public class PinnedGroupExpandListView extends ExpandableListView {
    static class PinnedSection {
        View view;
        int position;
        long id;
    }

    private final Rect mTouchRect = new Rect();
    private final PointF mTouchPoint = new PointF();
    private int mTouchSlop;
    private View mTouchTarget;
    private MotionEvent mDownEvent;

    private GradientDrawable mShadowDrawable;
    private int mSectionDistanceY;
    private int mShadowHeight;

    OnScrollListener mDelegateOnScrollListener;

    PinnedSection mRecycleSection;

    PinnedSection mPinnedSection;

    int mTranslateY;

    private final OnScrollListener mOnScrollListener = new OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (mDelegateOnScrollListener != null) {
                mDelegateOnScrollListener.onScrollStateChanged(view, scrollState);
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (mDelegateOnScrollListener != null) {
                mDelegateOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
            }

            ExpandableListAdapter adapter = getExpandableListAdapter();
            if (adapter == null || visibleItemCount == 0) {
                return;
            }
            final boolean isFirstVisibleItemSection =
                    isItemViewTypePinned(firstVisibleItem);

            if (isFirstVisibleItemSection) {
                View sectionView = getChildAt(0);
                if (sectionView.getTop() == getPaddingTop()) {
                    destroyPinnedShadow();
                } else {
                    ensureShadowForPosition(firstVisibleItem, firstVisibleItem, visibleItemCount);
                }
            } else {
                int flatSectionPosition = findCurrentSectionPosition(firstVisibleItem);
                if (flatSectionPosition > -1) {
                    ensureShadowForPosition(flatSectionPosition, firstVisibleItem, visibleItemCount);
                } else {
                    destroyPinnedShadow();
                }
            }

        }
    };

    private final DataSetObserver mDataSetObserver = new DataSetObserver() {
        @Override
        public void onInvalidated() {
            recreatePinnedShadow();
        }

        @Override
        public void onChanged() {
            recreatePinnedShadow();
        }
    };


    public PinnedGroupExpandListView(Context context) {
        super(context);
        initView();
    }

    public PinnedGroupExpandListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public PinnedGroupExpandListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    private void initView() {
        setOnScrollListener(mOnScrollListener);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        initShadow(false);
    }

    public void setShadowVisible(boolean visible) {
        initShadow(visible);
        if (mPinnedSection != null) {
            View v = mPinnedSection.view;
            invalidate(v.getLeft(), v.getTop(), v.getRight(), v.getBottom() + mShadowHeight);
        }
    }

    private void initShadow(boolean visible) {
        if (visible) {
            if (mShadowDrawable == null) {
                mShadowDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{Color.parseColor("#ffa0a0a0"),
                                Color.parseColor("#50a0a0a0"),
                                Color.parseColor("#00a0a0a0")});
                mShadowHeight = (int) (8 * getResources().getDisplayMetrics().density);
            }
        } else {
            if (mShadowDrawable != null) {
                mShadowDrawable = null;
                mShadowHeight = 0;
            }
        }
    }

    private void createPinnedShadow(int flatGroupPosition) {
        PinnedSection pinnedShadow = mRecycleSection;
        mRecycleSection = null;

        if (pinnedShadow == null) {
            pinnedShadow = new PinnedSection();
        }

        long packagedPosition = getExpandableListPosition(flatGroupPosition);
        int groupPosition = getPackedPositionGroup(packagedPosition);
        boolean expanded = isGroupExpanded(groupPosition);
        View pinnedView = getExpandableListAdapter().getGroupView(groupPosition, expanded,
                pinnedShadow.view, PinnedGroupExpandListView.this);

        ViewGroup.LayoutParams layoutParams = pinnedView.getLayoutParams();
        if (layoutParams == null) {
            layoutParams = generateDefaultLayoutParams();
            pinnedView.setLayoutParams(layoutParams);
        }

        int heightMode = MeasureSpec.getMode(layoutParams.height);
        int heightSize = MeasureSpec.getSize(layoutParams.height);

        if (heightMode == MeasureSpec.UNSPECIFIED) {
            heightMode = MeasureSpec.EXACTLY;
        }

        int maxHeight = getHeight() - getListPaddingTop() - getListPaddingBottom();
        if (heightSize > maxHeight) {
            heightSize = maxHeight;
        }

        int ws = MeasureSpec.makeMeasureSpec(getWidth() - getListPaddingLeft() - getListPaddingRight()
                , MeasureSpec.EXACTLY);
        int hs = MeasureSpec.makeMeasureSpec(heightSize, heightMode);
        pinnedView.measure(ws, hs);
        pinnedView.layout(0, 0, pinnedView.getMeasuredWidth(), pinnedView.getMeasuredHeight());
        mTranslateY = 0;

        pinnedShadow.view = pinnedView;
        pinnedShadow.position = groupPosition;
        pinnedShadow.id = getExpandableListAdapter().getGroupId(flatGroupPosition);

        mPinnedSection = pinnedShadow;
    }

    private void destroyPinnedShadow() {
        if (mPinnedSection != null) {
            mRecycleSection = mPinnedSection;
            mPinnedSection = null;
        }
    }

    private void ensureShadowForPosition(int flatSectionPosition, int firstVisibleItem, int visibleItemCount) {
        if (visibleItemCount < 2) {
            destroyPinnedShadow();
            return;
        }

        long packagedPosition = getExpandableListPosition(flatSectionPosition);
        int groupPosition = getPackedPositionGroup(packagedPosition);
        if (mPinnedSection != null
                && mPinnedSection.position != groupPosition) {
            destroyPinnedShadow();
        }

        if (mPinnedSection == null) {
            createPinnedShadow(flatSectionPosition);
        }

        int nextPosition = flatSectionPosition + 1;
        if (nextPosition < getCount()) {
            int nextSectionPosition = findFirstVisibleSectionPosition(nextPosition,
                    visibleItemCount - (nextPosition - firstVisibleItem));
            if (nextSectionPosition > -1) {
                View nextSectionView = getChildAt(nextSectionPosition - firstVisibleItem);
                final int bottom = mPinnedSection.view.getBottom() + getPaddingTop();
                mSectionDistanceY = nextSectionView.getTop() - bottom;
                if (mSectionDistanceY < 0) {
                    mTranslateY = mSectionDistanceY;
                } else {
                    mTranslateY = 0;
                }
            } else {
                mTranslateY = 0;
                mSectionDistanceY = Integer.MAX_VALUE;
            }
        }
    }

    private int findFirstVisibleSectionPosition(int firstVisibleItem, int visibleItemCount) {
        int adapterDataCount = getCount();
        if (getLastVisiblePosition() >= adapterDataCount) {
            return -1;
        }

        if (firstVisibleItem + visibleItemCount >= adapterDataCount) {
            visibleItemCount = adapterDataCount - firstVisibleItem;
        }

        for (int childIndex = 0; childIndex < visibleItemCount; childIndex++) {
            int position = firstVisibleItem + childIndex;
            if (isItemViewTypePinned(position)) {
                return position;
            }
        }
        return -1;
    }

    private int findCurrentSectionPosition(int fromPosition) {
        long packagedPosition = getExpandableListPosition(fromPosition);
        int groupPosition = getPackedPositionGroup(packagedPosition);
        long packagedGroupPosition = getPackedPositionForGroup(groupPosition);
        return getFlatListPosition(packagedGroupPosition);
    }

    private void recreatePinnedShadow() {
        destroyPinnedShadow();
        if (getCount() > 0) {
            int firstVisiblePosition = getFirstVisiblePosition();
            int sectionPosition = findCurrentSectionPosition(firstVisiblePosition);
            if (sectionPosition == -1) {
                return;
            }
            ensureShadowForPosition(sectionPosition,
                    firstVisiblePosition, getLastVisiblePosition() - firstVisiblePosition);
        }
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        if (l == mOnScrollListener) {
            super.setOnScrollListener(l);
        } else {
            mDelegateOnScrollListener = l;
        }
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);
        post(new Runnable() {
            @Override
            public void run() {
                recreatePinnedShadow();
            }
        });
    }

    @Override
    public void setAdapter(ExpandableListAdapter adapter) {

        ExpandableListAdapter oldAdapter = getExpandableListAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        if (adapter != null) {
            adapter.registerDataSetObserver(mDataSetObserver);
        }
        if (oldAdapter != adapter) {
            destroyPinnedShadow();
        }

        super.setAdapter(adapter);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (mPinnedSection != null) {
            int parentWidth = r - l - getPaddingLeft() - getPaddingRight();
            int shadowWidth = mPinnedSection.view.getWidth();
            if (parentWidth != shadowWidth) {
                recreatePinnedShadow();
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);

        if (mPinnedSection != null) {
            int pLeft = getListPaddingLeft();
            int pTop = getListPaddingTop();
            View view = mPinnedSection.view;

            canvas.save();

            int clipHeight = view.getHeight() +
                    (mShadowDrawable == null ? 0 : Math.min(mShadowHeight, mSectionDistanceY));
            canvas.clipRect(pLeft, pTop, pLeft + view.getWidth(), pTop + clipHeight);

            canvas.translate(pLeft, pTop + mTranslateY);
            drawChild(canvas, mPinnedSection.view, getDrawingTime());

            if (mShadowDrawable != null && mSectionDistanceY > 0) {
                mShadowDrawable.setBounds(mPinnedSection.view.getLeft(),
                        mPinnedSection.view.getBottom(),
                        mPinnedSection.view.getRight(),
                        mPinnedSection.view.getBottom() + mShadowHeight);
                mShadowDrawable.draw(canvas);
            }

            canvas.restore();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        final float x = ev.getX();
        final float y = ev.getY();
        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN
                && mTouchTarget == null
                && mPinnedSection != null
                && isPinnedViewTouched(mPinnedSection.view, x, y)) {

            mTouchTarget = mPinnedSection.view;
            mTouchPoint.x = x;
            mTouchPoint.y = y;

            mDownEvent = MotionEvent.obtain(ev);
        }

        if (mDownEvent != null) {
            if (isPinnedViewTouched(mTouchTarget, x, y)) {
                mTouchTarget.dispatchTouchEvent(ev);
            }

            if (action == MotionEvent.ACTION_UP) {
                super.dispatchTouchEvent(ev);
                performPinnedItemClick();
                clearTouchTarget();

            } else if (action == MotionEvent.ACTION_CANCEL) {
                clearTouchTarget();

            } else if (action == MotionEvent.ACTION_MOVE) {
                if (Math.abs(y - mTouchPoint.y) > mTouchSlop) {

                    MotionEvent event = MotionEvent.obtain(ev);
                    event.setAction(MotionEvent.ACTION_CANCEL);
                    mTouchTarget.dispatchTouchEvent(event);
                    event.recycle();

                    super.dispatchTouchEvent(mDownEvent);
                    super.dispatchTouchEvent(ev);
                    clearTouchTarget();
                }
            }
            return true;
        }

        return super.dispatchTouchEvent(ev);
    }

    private OnGroupClickListener mOnGroupClickListener;

    @Override
    public void setOnGroupClickListener(OnGroupClickListener onGroupClickListener) {
        mOnGroupClickListener = onGroupClickListener;
        super.setOnGroupClickListener(onGroupClickListener);
    }

    private boolean isPinnedViewTouched(View view, float x, float y) {
        view.getHitRect(mTouchRect);

        mTouchRect.top += mTranslateY;
        mTouchRect.bottom += mTranslateY + getPaddingTop();
        mTouchRect.left += getPaddingLeft();
        mTouchRect.right -= getPaddingRight();
        return mTouchRect.contains((int) x, (int) y);
    }

    private void clearTouchTarget() {
        mTouchTarget = null;
        if (mDownEvent != null) {
            mDownEvent.recycle();
            mDownEvent = null;
        }
    }

    private boolean performPinnedItemClick() {
        if (mPinnedSection == null) {
            return false;
        }

        if (mOnGroupClickListener != null) {
            View view = mPinnedSection.view;
            playSoundEffect(SoundEffectConstants.CLICK);
            if (view != null) {
                view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
            }
            mOnGroupClickListener.onGroupClick(this, view, mPinnedSection.position, mPinnedSection.id);
            return true;
        }
        return false;
    }

    private boolean isItemViewTypePinned(int flatPosition) {
        long expandablePosition = getExpandableListPosition(flatPosition);
        int childPosition = getPackedPositionChild(expandablePosition);
        return childPosition == -1;
    }
}
