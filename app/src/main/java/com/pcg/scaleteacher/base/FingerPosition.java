package com.pcg.scaleteacher.base;

public class FingerPosition {
    private int x, y;

    public boolean isValid;     //指示是否x和y有效（FingerPosition失效时不会置为null，以避免意想不到的错误）

    public FingerPosition() {
        this.x = 0;
        this.y = 0;
        isValid = false;
    }

    public FingerPosition(float _x, float _y) {
        this.x = (int)_x;
        this.y = (int)_y;
        isValid = true;
    }

    public void update(float _x, float _y) {
        this.x = (int)_x;
        this.y = (int)_y;
        isValid = true;
    }

    public float calPhysicDistance(FingerPosition other, int dpi) {
        int pixelCount = (int) Math.sqrt( (this.x - other.x) * (this.x - other.x) + (this.y -other.y) * (this.y - other.y) );
        return (float)pixelCount / (float)dpi * 2.54f;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }
}
