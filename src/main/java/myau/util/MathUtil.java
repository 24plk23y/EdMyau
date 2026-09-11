package myau.util;

import net.minecraft.util.Vec3;

import java.math.BigDecimal;
import java.math.MathContext;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

public class MathUtil {
    public static final float PI = (float) Math.PI;

    public static double interporate(float partialTicks, double old, double current) {
        return old + (current - old) * partialTicks;
    }

    public static Vec3 interpolateVec3(Vec3 old, Vec3 current, float partialTicks) {
        return new Vec3(
                old.xCoord + (current.xCoord - old.xCoord) * partialTicks,
                old.yCoord + (current.yCoord - old.yCoord) * partialTicks,
                old.zCoord + (current.zCoord - old.zCoord) * partialTicks
        );
    }

    public static double interpolateDouble(double old, double current, float partialTicks) {
        return old + (current - old) * partialTicks;
    }

    public static float normalize(float value, float min, float max) {
        return (value - min) / (max - min);
    }

    public static double roundToHalf(double d) {
        return Math.round(d * 2) / 2.0;
    }

    public static double incValue(double val, double inc) {
        double one = 1.0 / inc;
        return Math.round(val * one) / one;
    }

    public static double roundToDecimalPlace(double value, double inc) {
        double halfOfInc = inc / 2.0;
        double floored = StrictMath.floor(value / inc) * inc;

        if (value >= floored + halfOfInc) {
            return new BigDecimal(
                    StrictMath.ceil(value / inc) * inc,
                    MathContext.DECIMAL64
            ).stripTrailingZeros().doubleValue();
        }

        return new BigDecimal(
                floored,
                MathContext.DECIMAL64
        ).stripTrailingZeros().doubleValue();
    }

    public static double interpolate(double old, double now, float partialTicks) {
        return old + (now - old) * partialTicks;
    }

    public static double interpolate(double oldValue, double newValue, double interpolationValue) {
        return oldValue + (newValue - oldValue) * interpolationValue;
    }

    public static float interpolate(float old, float now, float partialTicks) {
        return old + (now - old) * partialTicks;
    }

    public static Vec3 interpolate(Vec3 end, Vec3 start, float multiple) {
        return new Vec3(
                interpolate(end.xCoord, start.xCoord, multiple),
                interpolate(end.yCoord, start.yCoord, multiple),
                interpolate(end.zCoord, start.zCoord, multiple)
        );
    }

    /**
     * 将 value 限制在 [floor, cap] 范围内。
     */
    public static float clampValue(float value, float floor, float cap) {
        if (value < floor) {
            return floor;
        }

        if (value > cap) {
            return cap;
        }

        return value;
    }

    /**
     * double 版本 clamp。
     */
    public static double clampValue(double value, double floor, double cap) {
        if (value < floor) {
            return floor;
        }

        if (value > cap) {
            return cap;
        }

        return value;
    }

    /**
     * int 版本 clamp。
     */
    public static int clampValue(int value, int floor, int cap) {
        if (value < floor) {
            return floor;
        }

        if (value > cap) {
            return cap;
        }

        return value;
    }

    public static int getRandom(int min, int max) {
        if (min == max) {
            return min;
        }

        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }

        return ThreadLocalRandom.current().nextInt(min, max);
    }

    public static double getRandom(double min, double max) {
        if (min == max) {
            return min;
        }

        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    public static float nextSecureFloat(double origin, double bound) {
        if (origin == bound) {
            return (float) origin;
        }

        SecureRandom secureRandom = new SecureRandom();
        float difference = (float) (bound - origin);

        return (float) (
                origin + secureRandom.nextFloat() * difference
        );
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double PI = Math.PI;

        double output = 1.0 / Math.sqrt(
                2.0 * PI * (sigma * sigma)
        );

        return (float) (
                output * Math.exp(
                        -(x * x) / (2.0 * (sigma * sigma))
                )
        );
    }

    public static double lerp(double pct, double start, double end) {
        return start + pct * (end - start);
    }

    public static float lerp(float min, float max, float delta) {
        return min + (max - min) * delta;
    }

    public static float scaleByPercent(float value, float percent) {
        return value * clampValue(
                percent / 100.0F,
                0.0F,
                1.0F
        );
    }

    public static int nextInt(int min, int max) {
        if (min == max || max - min <= 0) {
            return min;
        }

        return (int) (
                min + ((max - min) * Math.random())
        );
    }

    public static double nextDouble(double min, double max) {
        if (min == max || max - min <= 0) {
            return min;
        }

        return min + ((max - min) * Math.random());
    }

    public static float nextFloat(float startInclusive, float endInclusive) {
        if (startInclusive == endInclusive ||
                endInclusive - startInclusive <= 0F) {
            return startInclusive;
        }

        return (float) (
                startInclusive +
                        ((endInclusive - startInclusive) * Math.random())
        );
    }

    public static boolean inBetween(double min, double max, double value) {
        return value >= min && value <= max;
    }

    public static double wrappedDifference(double number1, double number2) {
        return Math.min(
                Math.abs(number1 - number2),
                Math.min(
                        Math.abs(number1 - 360) - Math.abs(number2 - 0),
                        Math.abs(number2 - 360) - Math.abs(number1 - 0)
                )
        );
    }
}