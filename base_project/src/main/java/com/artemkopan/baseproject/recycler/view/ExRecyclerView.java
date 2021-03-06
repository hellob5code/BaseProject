package com.artemkopan.baseproject.recycler.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.ProgressBar;

import com.artemkopan.baseproject.R;
import com.artemkopan.baseproject.utils.Log;
import com.artemkopan.baseproject.recycler.listeners.OnRecyclerPaginationListener;
import com.artemkopan.baseproject.recycler.listeners.OnRecyclerPaginationListener.OnRecyclerPaginationResult;
import com.artemkopan.baseproject.utils.ObjectUtils;
import com.artemkopan.baseproject.utils.ViewUtils;
import com.artemkopan.baseproject.widget.drawable.CircularProgressDrawable;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class ExRecyclerView extends RecyclerView {

    private static final int NO_VALUE = -1;

    private StaticLayout mTextLayout;
    private TextPaint mTextPaint;
    private CircularProgressDrawable mProgressDrawable;
    private Drawable mBackgroundDrawable;
    private OnRecyclerPaginationListener mPaginationListener;
    private Disposable mErrorTimer;
    private String mTextDefault;
    private int mProgressSize = NO_VALUE;
    private int mTextPadding = NO_VALUE;
    private boolean mDrawText, mDrawProgress;

    public ExRecyclerView(Context context) {
        this(context, null, 0);
    }

    public ExRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        int borderWidth = NO_VALUE, progressColor = NO_VALUE;
        int textSize = NO_VALUE, textColor = Color.BLACK;

        if (attrs != null) {
            TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.ExRecyclerView);
            try {
                mProgressSize = array.getDimensionPixelSize(
                        R.styleable.ExRecyclerView_erv_progressSize,
                        context.getResources().getDimensionPixelSize(R.dimen.base_progress_size));
                borderWidth = array.getDimensionPixelSize(
                        R.styleable.ExRecyclerView_erv_progressBorderWidth,
                        context.getResources()
                               .getDimensionPixelSize(R.dimen.base_progress_border_width));
                progressColor = array.getColor(R.styleable.ExRecyclerView_erv_progressColor,
                                               NO_VALUE);

                textSize = array.getDimensionPixelSize(R.styleable.ExRecyclerView_erv_textSize,
                                                       textSize);
                textColor = array.getColor(R.styleable.ExRecyclerView_erv_textColor, textColor);
                mTextDefault = array.getString(R.styleable.ExRecyclerView_erv_textDefault);
                mTextPadding = array.getDimensionPixelSize(
                        R.styleable.ExRecyclerView_erv_textPadding, mTextPadding);

                mBackgroundDrawable = array.getDrawable(
                        R.styleable.ExRecyclerView_erv_backgroundDrawable);
            } finally {
                array.recycle();
            }
        } else {
            mProgressSize = context.getResources()
                                   .getDimensionPixelSize(R.dimen.base_progress_size);
            borderWidth = context.getResources()
                                 .getDimensionPixelSize(R.dimen.base_progress_border_width);
        }

        if (progressColor == NO_VALUE) {
            progressColor = getColorPrimary();
        }

        if (textSize == NO_VALUE) {
            textSize = getContext().getResources()
                                   .getDimensionPixelSize(R.dimen.base_recycler_text_size);
        }
        if (mTextPadding == NO_VALUE) {
            mTextPadding = context.getResources()
                                  .getDimensionPixelSize(R.dimen.base_recycler_text_padding);
        }

        if (mBackgroundDrawable == null) {
            mBackgroundDrawable = new ColorDrawable(getThemeBackgroundColor());
        }

        if (TextUtils.isEmpty(mTextDefault)) {
            mTextDefault = context.getString(R.string.base_info_items_not_found);
        }

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(textColor);
        mTextPaint.setTextSize(textSize);

        mProgressDrawable = new CircularProgressDrawable(progressColor, borderWidth);
        mProgressDrawable.setCallback(this);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBackgroundDrawable.setBounds(0, 0, w, h);
        mProgressDrawable.setBounds(
                w / 2 - mProgressSize / 2, h / 2 - mProgressSize / 2,
                w / 2 + mProgressSize / 2, h / 2 + mProgressSize / 2);
    }

    public OnRecyclerPaginationListener createPaginationListener(OnRecyclerPaginationResult listener) {
        setPaginationListener(new OnRecyclerPaginationListener(getLayoutManager(),
                                                               OnRecyclerPaginationListener.VERTICAL,
                                                               listener));
        return mPaginationListener;
    }

    public void setPaginationListener(OnRecyclerPaginationListener scrollListener) {
        if (mPaginationListener != null) removeOnScrollListener(mPaginationListener);
        mPaginationListener = scrollListener;
        addOnScrollListener(scrollListener);
    }

    public void setPaginationState(boolean isEnable) {
        if (isEnable) {
            enablePagination();
        } else {
            disablePagination();
        }
    }

    public void enablePagination() {
        if (mPaginationListener != null) {
            mPaginationListener.enablePagination();
        }
    }

    public void disablePagination() {
        if (mPaginationListener != null) {
            mPaginationListener.disablePagination();
        }
    }

    public void setProgressColor(@ColorInt int color) {
        mProgressDrawable.setColor(color);
    }

    public void setTextPadding(int textPadding) {
        mTextPadding = textPadding;
    }

    public void showText(@StringRes int textRes, Object... arguments) {
        showText(getContext().getString(textRes, arguments));
    }

    public void showText(@StringRes int textRes) {
        showText(getContext().getString(textRes));
    }

    public void showText(final String text) {
        if (ViewUtils.checkSize(this)) {
            createTextLayout(text);
            Log.w("showText: Recycler not init yet");
        } else {
            ViewUtils.preDrawListener(this, new Runnable() {
                @Override
                public void run() {
                    if (!ViewUtils.checkSize(ExRecyclerView.this)) {
                        Log.e("showText: show text forbidden, because width or height == 0");
                        return;
                    }
                    createTextLayout(text);
                    showText(text);
                }
            });
            return;
        }

        mDrawText = true;
        mDrawProgress = false;

        postInvalidate();

        if (getAdapter() != null && getAdapter().getItemCount() != 0) {
            if (mErrorTimer != null) mErrorTimer.dispose();
            mErrorTimer = Observable.timer(1_500, TimeUnit.MILLISECONDS).subscribe(new Consumer<Long>() {
                @Override
                public void accept(Long aLong) throws Exception {
                    hideText();
                }
            });
        }
    }

    public void hideText() {
        mDrawText = false;
        postInvalidate();
    }

    public void showProgress() {
        mDrawProgress = true;
        mDrawText = false;
        mProgressDrawable.start();
        postInvalidate();
    }

    public void hideProgress() {
        mDrawProgress = false;
        mProgressDrawable.stop();
        postInvalidate();
    }

    public void hideAll() {
        mDrawProgress = false;
        mDrawText = false;
        postInvalidate();
    }

    /**
     * Create StaticLayout {@link StaticLayout}
     */
    private void createTextLayout(CharSequence text) {
        mTextLayout = new StaticLayout(
                text,
                mTextPaint,
                getWidth() - mTextPadding * 2,
                Layout.Alignment.ALIGN_CENTER,
                1,
                0,
                true);
    }

    @Override
    public void draw(Canvas c) {
        super.draw(c);

        if ((mDrawProgress || mDrawText)
            && getAdapter() != null && getAdapter().getItemCount() > 0) {

            final int restore = c.save();
            mBackgroundDrawable.draw(c);
            c.restoreToCount(restore);
        }

        if (mDrawProgress) {
            mProgressDrawable.draw(c);
        }

        if (mDrawText && mTextLayout != null) {
            final int restore = c.save();
            c.translate(
                    (c.getWidth() / 2) - (mTextLayout.getWidth() / 2),
                    (c.getHeight() / 2) - ((mTextLayout.getHeight() / 2)));
            mTextLayout.draw(c);
            c.restoreToCount(restore);
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return who == mProgressDrawable || super.verifyDrawable(who);
    }

    @Override
    public boolean canScrollVertically(int direction) {
        // check if scrolling up
        if (direction < 1) {
            boolean original = super.canScrollVertically(direction);
            return !original && getChildAt(0) != null && getChildAt(0).getTop() < 0 || original;
        }
        return super.canScrollVertically(direction);

    }

    private int getThemeBackgroundColor() {
        TypedValue a = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.windowBackground, a, true);
        if (a.type >= TypedValue.TYPE_FIRST_COLOR_INT && a.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            // windowBackground is a color
            return a.data;
        } else {
            return NO_VALUE;
        }
    }

    private int getColorPrimary() {
        TypedValue typedValue = new TypedValue();
        TypedArray a = getContext().obtainStyledAttributes(
                typedValue.data, new int[]{R.attr.colorPrimary});

        int progressColor;

        try {
            progressColor = a.getColor(0, Color.BLACK);
        } finally {
            a.recycle();
        }

        return progressColor;
    }
}
