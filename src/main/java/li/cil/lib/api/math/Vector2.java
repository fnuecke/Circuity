package li.cil.lib.api.math;

import net.minecraft.util.math.MathHelper;

public final class Vector2 {
    public static final Vector2 ZERO = new Vector2(0, 0);
    public static final Vector2 ONE = new Vector2(1, 1);

    // --------------------------------------------------------------------- //

    public final float x, y;

    // --------------------------------------------------------------------- //

    public Vector2(final float x, final float y) {
        this.x = x;
        this.y = y;
    }

    // --------------------------------------------------------------------- //

    public float get(final int i) {
        switch (i) {
            case 0:
                return x;
            case 1:
                return y;
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public Vector2 set(final int i, final float value) {
        switch (i) {
            case 0:
                return new Vector2(value, y);
            case 1:
                return new Vector2(x, value);
            default:
                throw new IndexOutOfBoundsException();
        }
    }

    public float sqrMagnitude() {
        return x * x + y * y;
    }

    public float magnitude() {
        return MathHelper.sqrt(sqrMagnitude());
    }

    public Vector2 normalized() {
        final float magnitude = magnitude();
        return new Vector2(x / magnitude, y / magnitude);
    }

    public Vector2 add(final Vector2 v) {
        return add(v.x, v.y);
    }

    public Vector2 add(final float x, final float y) {
        return new Vector2(this.x + x, this.y + y);
    }

    public Vector2 sub(final Vector2 v) {
        return sub(v.x, v.y);
    }

    public Vector2 sub(final float x, final float y) {
        return new Vector2(this.x - x, this.y - y);
    }

    public Vector2 mul(final float s) {
        return new Vector2(x * s, y * s);
    }

    public Vector2 div(final float s) {
        return mul(1.0f / s);
    }

    public Vector2 scale(final Vector2 v) {
        return scale(v.x, v.y);
    }

    public Vector2 scale(final float x, final float y) {
        return new Vector2(this.x * x, this.y * y);
    }

    public float dot(final Vector2 v) {
        return x * v.x + y * v.y;
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Vector2 v = (Vector2) o;

        return Float.compare(v.x, x) == 0 &&
                Float.compare(v.y, y) == 0;
    }

    @Override
    public int hashCode() {
        int result = (x != +0.0f ? Float.floatToIntBits(x) : 0);
        result = 31 * result + (y != +0.0f ? Float.floatToIntBits(y) : 0);
        return result;
    }

    @Override
    public String toString() {
        return "{" + x + ", " + y + "}";
    }
}
