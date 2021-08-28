package com.surcumference.fingerprint.util.drawable;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;

public class XDrawable {

    public static Drawable defaultDrawable() {
        return new Builder().create();
    }

    public static class Builder {
        public int defaultColor = Color.TRANSPARENT;
        public int pressedColor = 0xFFE5E5E5;
        public int round = 0;

        public Drawable create() {
            StateListDrawable statesDrawable = new StateListDrawable();
            statesDrawable.addState(new int[]{android.R.attr.state_pressed}, round > 0 ? roundDrawable(round, pressedColor) : new ColorDrawable(pressedColor));
            statesDrawable.addState(new int[]{}, round > 0 ? roundDrawable(round, defaultColor) : new ColorDrawable(defaultColor));
            return statesDrawable;
        }

        public Builder defaultColor(int color) {
            this.defaultColor = color;
            return this;
        }

        public Builder pressedColor(int color) {
            this.pressedColor = color;
            return this;
        }

        /**
         * @param round pixel
         * @return
         */
        public Builder round(int round) {
            this.round = round;
            return this;
        }

        private static Drawable roundDrawable(int round, int color) {
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(round);
            shape.setColor(color);
            return shape;
        }
    }
}