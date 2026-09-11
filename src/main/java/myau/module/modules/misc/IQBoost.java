package myau.module.modules;

import myau.module.Module;
import myau.property.properties.FloatProperty;

public class IQBoost extends Module {

    public final FloatProperty iq = new FloatProperty("IQ", 6767.67F, -100000.0F, 100000.0F);

    public IQBoost() {
        super("IQBoost", true, false);
    }

    @Override
    public String[] getSuffix() {
        return new String[]{String.valueOf(iq.getValue())};
    }
}