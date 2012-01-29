package ru.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.*;
import android.widget.Scroller;

/**
 * This class allows to switch between multiple screens (layouts) in the same
 * way as the Android home screen (Launcher application) both vertically and horizontally.
 * <p/>
 */
public class TwoDirectionsViewSwitcher extends ViewGroup {
    private int mXDiff;
    private int mYDiff;
    private boolean isXMove = false;
    private boolean isYMove = false;
    private boolean isMoveBegin = false;

    public static interface OnScreenSwitchListener {
        void onScreenSwitched(int screen);
    }

    private int mRows = 1;
    private static final int SNAP_VELOCITY = 1000;
    private static final int INVALID_SCREEN = -1;

    private Scroller mScroller;
    private VelocityTracker mVelocityTracker;

    private final static int TOUCH_STATE_REST = 0;
    private final static int TOUCH_STATE_SCROLLING = 1;

    private int mTouchState = TOUCH_STATE_REST;

    private float mLastMotionX;
    private float mLastMotionY;
    private int mTouchSlop;
    private int mMaximumVelocity;
    private int mCurrentScreen;
    private int mNextScreen = INVALID_SCREEN;

    private boolean mFirstLayout = true;

    private OnScreenSwitchListener mOnScreenSwitchListener;

    public TwoDirectionsViewSwitcher(Context context) {
        super(context);
        init();

    }

    public TwoDirectionsViewSwitcher(Context context, int rows) {
        super(context);
        mRows = rows;
        init();
    }

    public TwoDirectionsViewSwitcher(Context context, int rows, int currentScreen) {
        super(context);
        mCurrentScreen = currentScreen;
        mRows = rows;
        init();
    }

    public TwoDirectionsViewSwitcher(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        mScroller = new Scroller(getContext());

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException();
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException();
        }

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }

        if (mFirstLayout) {
            int row = mCurrentScreen / (getChildCount() / mRows);
            int cell = mCurrentScreen % (getChildCount() / mRows);
            scrollTo(cell * width, row * height);
            mFirstLayout = false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int childLeft = 0;
        int childWidth = 0;
        final int count = getChildCount();

        int currentViewPosition = 0;
        for (int j = 0; j < mRows; j++) {
            for (int i = 0; i < count / mRows; i++) {
                final View child = getChildAt(currentViewPosition);
                if (child.getVisibility() != View.GONE) {
                    childWidth = child.getMeasuredWidth();
                    final int childHeight = child.getMeasuredHeight();
                    child.layout(childLeft, j * childHeight, childLeft + childWidth, child.getMeasuredHeight() + j * childHeight);
                    currentViewPosition++;
                }
                childLeft += childWidth;
            }
            childLeft = 0;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();
        final float x = ev.getX();
        final float y = ev.getY();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isMoveBegin = true;

                /*
                * If being flinged and user touches, stop the fling. isFinished will be false if being flinged.
                */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mLastMotionX = x;
                mLastMotionY = y;

                mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;

                break;

            case MotionEvent.ACTION_MOVE:
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                final int yDiff = (int) Math.abs(y - mLastMotionY);
                mXDiff = xDiff;
                mYDiff = yDiff;

                boolean xMoved = xDiff > mTouchSlop;
                boolean yMoved = yDiff > mTouchSlop;

                if (xMoved || yMoved) {
                    // Scroll if the user moved far enough along the X axis
                    mTouchState = TOUCH_STATE_SCROLLING;
                }

                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    // Scroll to follow the motion event
                    final int deltaX = (int) (mLastMotionX - x);
                    final int deltaY = (int) (mLastMotionY - y);
                    mLastMotionX = x;
                    mLastMotionY = y;

                    final int scrollX = getScrollX();
                    final int scrollY = getScrollY();
                    if (Math.abs(deltaX) > Math.abs(deltaY) && isMoveBegin) {
                        isMoveBegin = false;
                        isXMove = true;
                        isYMove = false;
                    } else if (Math.abs(deltaX) < Math.abs(deltaY) && isMoveBegin) {
                        isMoveBegin = false;
                        isXMove = false;
                        isYMove = true;
                    }


                    if (isXMove) {
                        if (deltaX < 0) {
                            if (scrollX > 0) {
                                scrollBy(Math.max(-scrollX, deltaX), 0);
                            }
                        } else if (deltaX > 0) {
                            final int availableToScroll = getChildAt(getChildCount() / mRows + 2).getRight() - scrollX - getWidth();
                            if (availableToScroll > 0) {
                                scrollBy(Math.min(availableToScroll, deltaX), 0);
                            }
                        }
                    } else if (isYMove) {
                        if (deltaY < 0) {
                            if (scrollY > 0) {
                                scrollBy(0, Math.max(-scrollY, deltaY));
                            }
                        } else if (deltaY > 0) {
                            final int availableToScroll = getChildAt(getChildCount() - 1).getBottom() - scrollY - getHeight();
                            if (availableToScroll > 0) {
                                scrollBy(0, Math.min(availableToScroll, deltaY));
                            }
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                isXMove = false;
                isYMove = false;
                isMoveBegin = false;
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int velocityX = (int) velocityTracker.getXVelocity();
                    int velocityY = (int) velocityTracker.getYVelocity();

                    if (Math.abs(mXDiff) > Math.abs(mYDiff)) {
                        if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
                            // Fling hard enough to move left
                            if ((mCurrentScreen + 1) % (getChildCount() / mRows) != 1)
                                snapToXScreen(mCurrentScreen - 1);
                            else
                                snapToXScreen(mCurrentScreen);
                        } else if (velocityX < -SNAP_VELOCITY && mCurrentScreen < getChildCount() - 1) {
                            // Fling hard enough to move right
                            if ((mCurrentScreen + 1) % (getChildCount() / mRows) != 0)
                                snapToXScreen(mCurrentScreen + 1);
                            else
                                snapToXScreen(mCurrentScreen);
                        } else {
                            snapToXDestination();
                        }
                    } else {
                        if (velocityY > SNAP_VELOCITY && mCurrentScreen > 0) {
                            // Fling hard enough to move UP
                            if ((mCurrentScreen - 1) - (getChildCount() / mRows) >= 0)
                                snapToYScreen(mCurrentScreen - 1);
                            else {
                                snapToYScreen(mCurrentScreen);
                            }
                        } else if (velocityY < -SNAP_VELOCITY && mCurrentScreen < getChildCount() - 1) {
                            // Fling hard enough to move down
                            if ((mCurrentScreen + 1) + (getChildCount() / mRows) < getChildCount()) {
                                snapToYScreen(mCurrentScreen + 1);
                            } else {
                                snapToYScreen(mCurrentScreen);
                            }
                        } else {
                            snapToYDestination();
                        }
                    }

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }

                mTouchState = TOUCH_STATE_REST;

                break;
            case MotionEvent.ACTION_CANCEL:
                mTouchState = TOUCH_STATE_REST;
        }

        return true;
    }

    private void snapToXDestination() {
        final int screenHeight = getHeight();
        final int screenWidth = getWidth();
        final int whichScreenX = (getScrollX() + (screenWidth / 2)) / screenWidth;
        final int whichScreenY = (getScrollY() + (screenHeight / 2)) / screenHeight;

        snapToXScreen(whichScreenY * (getChildCount() / mRows) + whichScreenX);
    }

    private void snapToYDestination() {
        final int screenHeight = getHeight();
        final int screenWidth = getWidth();
        final int whichScreenX = (getScrollX() + (screenWidth / 2)) / screenWidth;
        final int whichScreenY = (getScrollY() + (screenHeight / 2)) / screenHeight;

        snapToYScreen(whichScreenY * (getChildCount() / mRows) + whichScreenX);
    }

    private void snapToXScreen(int whichScreen) {
        if (!mScroller.isFinished())
            return;
        mNextScreen = whichScreen;

        int row = whichScreen / (getChildCount() / mRows);
        int cell = whichScreen % (getChildCount() / mRows);
        final int newX = cell * getWidth();
        final int delta = newX - getScrollX();
        mScroller.startScroll(getScrollX(), row * getHeight(), delta, 0, Math.abs(delta) * 2);
        invalidate();
    }

    private void snapToYScreen(int whichScreen) {
        if (!mScroller.isFinished()) return;
        int cell = whichScreen % (getChildCount() / mRows);
        int row = whichScreen / (getChildCount() / mRows);
        mNextScreen = whichScreen;

        final int newY = row * getHeight();
        final int delta = newY - getScrollY();
        mScroller.startScroll(cell * getWidth(), getScrollY(), 0, delta, Math.abs(delta) * 2);
        invalidate();
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getFinalX(), mScroller.getFinalY());
            invalidate();
            postInvalidate();
        } else if (mNextScreen != INVALID_SCREEN) {
            mCurrentScreen = Math.max(0, Math.min(mNextScreen, getChildCount() - 1));

            if (mOnScreenSwitchListener != null)
                mOnScreenSwitchListener.onScreenSwitched(mCurrentScreen);

            mNextScreen = INVALID_SCREEN;
        }
    }

    public void setCurrentScreen(int currentScreen) {
        int row = currentScreen / (getChildCount() / mRows);
        int cell = currentScreen % (getChildCount() / mRows);
        mCurrentScreen = Math.max(0, Math.min(currentScreen, getChildCount() - 1));
        scrollTo(cell * getWidth(), row * getHeight());
        invalidate();
    }

    public void setOnScreenSwitchListener(OnScreenSwitchListener onScreenSwitchListener) {
        mOnScreenSwitchListener = onScreenSwitchListener;
    }

}

