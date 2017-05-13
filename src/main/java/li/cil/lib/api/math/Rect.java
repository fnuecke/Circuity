package li.cil.lib.api.math;

public final class Rect {
    public static final Rect ZERO = new Rect(0, 0, 0, 0);

    // --------------------------------------------------------------------- //

    public final float xMin, xMax, yMin, yMax;

    // --------------------------------------------------------------------- //

    public Rect(final float xMin, final float xMax, final float yMin, final float yMax) {
        this.xMin = xMin;
        this.xMax = xMax;
        this.yMin = yMin;
        this.yMax = yMax;
    }

    public Rect(final Vector2 from, final Vector2 to) {
        xMin = Math.min(from.x, to.x);
        xMax = Math.max(from.x, to.x);
        yMin = Math.min(from.y, to.y);
        yMax = Math.max(from.y, to.y);
    }

    // --------------------------------------------------------------------- //

    public Vector2 getMin() {
        return new Vector2(xMin, yMin);
    }

    public Vector2 getMax() {
        return new Vector2(xMax, yMax);
    }

    public float getWidth() {
        return xMax - xMin;
    }

    public float getHeight() {
        return yMax - yMin;
    }

    public Vector2 getCenter() {
        return new Vector2((xMin + xMax) * 0.5f, (yMin + yMax) * 0.5f);
    }

    public float getArea() {
        return getWidth() * getHeight();
    }

    public boolean contains(final Vector2 v) {
        return xMin <= v.x && xMax >= v.x && yMin <= v.y && yMax >= v.y;
    }

    public boolean intersects(final Rect rect) {
        return rect.xMin < xMax && rect.xMax > xMin &&
               rect.yMin < yMax && rect.yMax > yMin;
    }

    public Rect offset(final Vector2 v) {
        return offset(v.x, v.y);
    }

    public Rect offset(final float x, final float y) {
        return new Rect(xMin + x, xMax + x, yMin + y, yMax + y);
    }

    public Rect expand(final Vector2 v) {
        return expand(v.x, v.y);
    }

    public Rect expand(final float x, final float y) {
        return new Rect(xMin - x, xMax + x, yMin - y, yMax + y);
    }

    public Rect union(final Rect rect) {
        return new Rect(Math.min(xMin, rect.xMin), Math.max(xMax, rect.xMax), Math.min(yMin, rect.yMin), Math.max(yMax, rect.yMax));
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Rect rect = (Rect) o;

        return Float.compare(rect.xMin, xMin) == 0 &&
               Float.compare(rect.xMax, xMax) == 0 &&
               Float.compare(rect.yMin, yMin) == 0 &&
               Float.compare(rect.yMax, yMax) == 0;
    }

    @Override
    public int hashCode() {
        int result = (xMin != +0.0f ? Float.floatToIntBits(xMin) : 0);
        result = 31 * result + (xMax != +0.0f ? Float.floatToIntBits(xMax) : 0);
        result = 31 * result + (yMin != +0.0f ? Float.floatToIntBits(yMin) : 0);
        result = 31 * result + (yMax != +0.0f ? Float.floatToIntBits(yMax) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "{" + getMin() + ", " + getMax() + "}";
    }
}
