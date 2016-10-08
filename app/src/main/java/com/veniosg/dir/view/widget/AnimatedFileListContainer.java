/*
 * Copyright (C) 2014 George Venios
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.veniosg.dir.view.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;

import com.veniosg.dir.R;
import com.veniosg.dir.util.Logger;
import com.veniosg.dir.view.Themer;

import static android.graphics.PorterDuff.Mode.SRC_OVER;
import static android.util.Log.DEBUG;
import static com.veniosg.dir.AnimationConstants.ANIM_DURATION;
import static com.veniosg.dir.AnimationConstants.ANIM_START_DELAY;
import static com.veniosg.dir.AnimationConstants.IN_INTERPOLATOR;
import static com.veniosg.dir.AnimationConstants.OUT_INTERPOLATOR;
import static com.veniosg.dir.FileManagerApplication.enqueueAnimator;
import static com.veniosg.dir.util.Logger.TAG_ANIMATION;
import static com.veniosg.dir.view.Themer.getThemedColor;
import static com.veniosg.dir.view.Themer.getThemedResourceId;

/**
 * @author George Venios
 */
public class AnimatedFileListContainer extends FrameLayout {
    private static final int INDEX_CONTENT = 0;

    private BitmapDrawable mScreenshot;
    private BitmapDrawable mHeroshot;
    private float mDimAmount = 0;
    private int mDimColor = getResources().getColor(
            getThemedResourceId(getContext(), R.attr.colorFadeCovered));
    private float mHeroTop = 0;
    private float mHeroLeft = 0;
    private float mHeroRight = 0;
    private final Drawable mBackground;

    public AnimatedFileListContainer(Context context) {
        super(context);
        mBackground = getBackground();
    }

    public AnimatedFileListContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        mBackground = getBackground();
    }

    public AnimatedFileListContainer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mBackground = getBackground();
    }

    /**
     * Call this before changing the content! <br/>
     * @param direction 1 for forward, -1 for backward, 0 is undefined.
     * @param hero The hero element, aka the one causing the transition. Can be null.
     */
    public void setupAnimations(int direction, final View hero) {
        View oldView = getChildAt(INDEX_CONTENT);

        Bitmap screenshotCache = Bitmap.createBitmap(oldView.getWidth(), oldView.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas screenshotCanvas = new Canvas(screenshotCache);
        if (direction < 0)
            mBackground.draw(screenshotCanvas);
        oldView.draw(screenshotCanvas);

        mScreenshot = new BitmapDrawable(getResources(), screenshotCache);
        mScreenshot.setBounds(0, 0, screenshotCanvas.getWidth(), screenshotCanvas.getHeight());

        if (hero != null) {
            int leftPadding = findViewById(android.R.id.list).getPaddingLeft();
            int listWidth = findViewById(android.R.id.list).getWidth();

            Bitmap heroCache = Bitmap.createBitmap(listWidth, hero.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas heroCanvas = new Canvas(heroCache);
            heroCanvas.save();
                heroCanvas.translate(leftPadding, 0);
                heroCanvas.translate(hero.getLeft(), 0);
                hero.draw(heroCanvas);
            heroCanvas.restore();
            mHeroshot = new BitmapDrawable(getResources(), heroCache);
            mHeroshot.setBounds(0, 0, heroCanvas.getWidth(), heroCanvas.getHeight());

            mHeroTop = hero.getY();
            mHeroLeft = hero.getLeft();
            mHeroRight = hero.getRight();
        }

        Logger.log(DEBUG, TAG_ANIMATION, "Recorded drawable");
    }

    public void animateFwd() {
        if (mScreenshot == null) {
            return;
        }

        final View newContent = getChildAt(INDEX_CONTENT);
        final RippleEnabledDrawableView oldContent = new RippleEnabledDrawableView(getContext());
        final RippleEnabledDrawableView hero = new RippleEnabledDrawableView(getContext());
        hero.setY(mHeroTop);
        hero.setInitialBackgroundPosition((int) mHeroLeft, (int) mHeroRight);
        AnimatorSet contentAnim = new AnimatorSet();
        AnimatorSet heroAnim = new AnimatorSet();

        // Add and show last list state
        oldContent.setDrawable(mScreenshot);
        hero.setDrawable(mHeroshot);
        addView(hero, 0);
        addView(oldContent, 0);
        newContent.setBackgroundDrawable(mBackground.mutate());
        Logger.log(DEBUG, TAG_ANIMATION, "Added new views. New count: " + getChildCount());

        // Init animations
        contentAnim = forwardContentAnimation(newContent, oldContent, hero);
        heroAnim = forwardHeroAnimation(hero);

        // Play animations
        AnimatorSet scene = new AnimatorSet();
        setupSceneAnimation(oldContent, hero, contentAnim, heroAnim, IN_INTERPOLATOR, scene);
        Logger.log(DEBUG, TAG_ANIMATION, "Starting animation");
        enqueueAnimator(scene);
    }

    public void animateBwd() {
        if(mScreenshot == null) {
            return;
        }

        final View newContent = getChildAt(INDEX_CONTENT);
        final RippleEnabledDrawableView oldContent = new RippleEnabledDrawableView(getContext());
        final RippleEnabledDrawableView hero = new RippleEnabledDrawableView(getContext());
        hero.setY(mHeroTop);
        hero.setInitialBackgroundPosition((int) mHeroLeft, (int) mHeroRight);
        AnimatorSet contentAnim = new AnimatorSet();
        AnimatorSet heroAnim = new AnimatorSet();

        // Add and show last list state
        oldContent.setDrawable(mScreenshot);
        hero.setDrawable(mHeroshot);
        addView(oldContent);
        addView(hero);    // Since this is the last child, it'll be overlaid on top of the others
        Logger.log(DEBUG, TAG_ANIMATION, "Added new views. New count: " + getChildCount());

        // Init animations
        contentAnim = backwardContentAnimation(newContent, oldContent);
        heroAnim = backwardHeroAnimation(hero);

        // Play animations
        AnimatorSet scene = new AnimatorSet();
        setupSceneAnimation(oldContent, hero, contentAnim, heroAnim, OUT_INTERPOLATOR, scene);
        Logger.log(DEBUG, TAG_ANIMATION, "Starting animation");
        enqueueAnimator(scene);
    }

    private AnimatorSet forwardHeroAnimation(final View hero) {
        AnimatorSet heroAnim = new AnimatorSet();
        ObjectAnimator anim = ObjectAnimator.ofFloat(hero, "translationX", 0, -getWidth());
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(hero, "translationZ", 0F, 15F);
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(hero, "backgroundProgress", 0F, 1F);

        heroAnim.addListener(resetTranslationZOnEndListener(hero));
        heroAnim.playTogether(anim, anim2, anim3);
        return heroAnim;
    }

    private AnimatorSet forwardContentAnimation(final View newContent, final View oldContent, final RippleEnabledDrawableView hero) {
        int heroHeight = hero.getDrawableHeight();
        if (heroHeight > 0) {
            oldContent.setPivotY(hero.getY() + heroHeight / 2);
        }

        AnimatorSet contentAnim = new AnimatorSet();
        ObjectAnimator anim = ObjectAnimator.ofFloat(oldContent, "scaleX", 1F, 0.9F);
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(oldContent, "scaleY", 1F, 0.9F);
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(oldContent, "saturation", 1F, -3F);
        ObjectAnimator anim4 = ObjectAnimator.ofFloat(oldContent, "translationX", 0F, -getWidth()/2);
        ObjectAnimator anim5 = ObjectAnimator.ofFloat(newContent, "translationZ", 0F, 15F);
        ObjectAnimator anim6 = ObjectAnimator.ofFloat(newContent, "translationX", getWidth(), 0F);
        ObjectAnimator anim7 = ObjectAnimator.ofFloat(this, "backgroundDim", 0F, 0.5F);

        contentAnim.addListener(resetTranslationZOnEndListener(newContent));
        contentAnim.playTogether(
                anim,
                anim2,
                anim3,
                anim4,
                anim5,
                anim6,
                anim7
        );

        return contentAnim;
    }

    private AnimatorSet backwardHeroAnimation(final View hero) {
        AnimatorSet heroAnim = new AnimatorSet();
        ObjectAnimator anim = ObjectAnimator.ofFloat(hero, "translationX", -getWidth(), 0);
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(hero, "translationZ", 15F, 0F);
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(hero, "backgroundProgress", 1F, 0F);

        heroAnim.playTogether(anim, anim2, anim3);
        return heroAnim;
    }

    private AnimatorSet backwardContentAnimation(final View newContent, final View oldContent) {
        AnimatorSet contentAnim = new AnimatorSet();
        ObjectAnimator anim = ObjectAnimator.ofFloat(newContent, "scaleX", 0.9F, 1F);
        ObjectAnimator anim2 = ObjectAnimator.ofFloat(newContent, "scaleY", 0.9F, 1F);
        ObjectAnimator anim3 = ObjectAnimator.ofFloat(newContent, "translationX", -getWidth()/2, 0F);
        ObjectAnimator anim4 = ObjectAnimator.ofFloat(oldContent, "translationZ", 15F, 0F);
        ObjectAnimator anim5 = ObjectAnimator.ofFloat(oldContent, "translationX", 0F, getWidth());
        ObjectAnimator anim6 = ObjectAnimator.ofFloat(this, "backgroundDim", 0.5F, 0F);

        contentAnim.playTogether(
                anim,
                anim2,
                anim3,
                anim4,
                anim5,
                anim6
        );

        return contentAnim;
    }

    private void setupSceneAnimation(final RippleEnabledDrawableView oldContent, final RippleEnabledDrawableView hero, AnimatorSet contentAnim,
                                     AnimatorSet heroAnim, Interpolator interpolator, AnimatorSet scene) {
        scene.setDuration(ANIM_DURATION);
        scene.setInterpolator(interpolator);
        scene.setStartDelay(ANIM_START_DELAY);
        scene.playTogether(contentAnim, heroAnim);
        scene.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                Logger.log(DEBUG, TAG_ANIMATION, "Started animation");

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animation.removeListener(this);
                finishAnimation(oldContent, hero);
                Logger.log(DEBUG, TAG_ANIMATION, "Finished animation");
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animation.removeListener(this);
                finishAnimation(oldContent, hero);
                Logger.log(DEBUG, TAG_ANIMATION, "Cancelled animation");
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
    }

    private void finishAnimation(RippleEnabledDrawableView oldContent, RippleEnabledDrawableView hero) {
        try {
            removeView(oldContent);
            removeView(hero);
            Logger.log(DEBUG, TAG_ANIMATION, "Removed view. New count: " + getChildCount());

            clearAnimations();
        } catch (Throwable t) {
            Logger.log(t);
        }
    }

    public void clearAnimations() {
        mScreenshot = null;
        mHeroshot = null;
        setBackgroundDim(0);
        getChildAt(INDEX_CONTENT).setBackgroundDrawable(null);
        Logger.log(DEBUG, TAG_ANIMATION, "Cleared animations");
    }

    private Animator.AnimatorListener resetTranslationZOnEndListener(final View target) {
        return new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {
                animator.removeListener(this);
                target.setTranslationZ(0);
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        };
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        boolean result = super.drawChild(canvas, child, drawingTime);

        if (mDimAmount > 0 && getChildCount() > 1) {
            if (indexOfChild(child) == 0) {
                canvas.drawColor(computeDarkenColor(), SRC_OVER);
            }
        }

        return result;
    }

    public void setBackgroundDim(float dim) {
        mDimAmount = dim;
        invalidate();
    }

    public float getBackgroundDim() {
        return mDimAmount;
    }

    public int computeDarkenColor() {
        final int baseAlpha = (mDimColor & 0xff000000) >>> 24;
        int imag = (int) (baseAlpha * mDimAmount);
        int color = imag << 24 | (mDimColor & 0xffffff);
        return color;
    }
}