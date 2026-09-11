package myau.property.properties;

import com.google.gson.JsonObject;
import myau.property.Property;

import java.util.function.BooleanSupplier;

public class FloatProperty extends Property<Float> {

    private final Float minimum;
    private final Float maximum;

    public FloatProperty(String name, Float value, Float minimum, Float maximum) {
        this(name, value, minimum, maximum, null);
    }

    public FloatProperty(
            String name,
            Float value,
            Float minimum,
            Float maximum,
            BooleanSupplier check
    ) {
        super(name, value, v -> v >= minimum && v <= maximum, check);
        this.minimum = minimum;
        this.maximum = maximum;
    }

    @Override
    public String getValuePrompt() {
        return String.format("%s-%s", this.minimum, this.maximum);
    }

@Override
public String formatValue() {
    return String.format("&6%.2f", this.getValue());
}

    @Override
    public boolean parseString(String string) {
        try {
            return this.setValue(Float.parseFloat(string));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        if (!jsonObject.has(this.getName())) {
            return false;
        }

        return this.setValue(
                jsonObject.get(this.getName()).getAsFloat()
        );
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), this.getValue());
    }

    public Float getMinimum() {
        return minimum;
    }

    public Float getMaximum() {
        return maximum;
    }
}