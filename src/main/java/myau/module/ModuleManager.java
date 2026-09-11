package myau.module;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.modules.GuiModule;
import myau.module.modules.HUD;
import myau.util.ChatUtil;
import myau.util.SoundUtil;
import java.io.File;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Comparator;
import java.util.stream.Stream;
import myau.property.Property;
import java.util.LinkedHashMap;
import myau.property.PropertyManager;
import myau.event.EventManager;

public class ModuleManager {
    private boolean sound = false;
    private final PropertyManager propertyManager;
    
    public ModuleManager(PropertyManager propertyManager) {
    this.propertyManager = propertyManager;
    }
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();

    public Module getModule(String string) {
        return this.modules.values().stream().filter(mD -> mD.getName().equalsIgnoreCase(string)).findFirst().orElse(null);
    }

    public Module getModule(Class<?> clazz){
        return this.modules.get(clazz);
    }

    public void playSound() {
        this.sound = true;
    }
    
    private List<Class<? extends Module>> scanPackageForModules(String packageName) {
        Set<Class<? extends Module>> result = new LinkedHashSet<>();
        String path = packageName.replace('.', '/');

        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if ("jar".equals(url.getProtocol())) {
                    JarURLConnection connection = (JarURLConnection) url.openConnection();
                    try (JarFile jar = connection.getJarFile()) {
                        Enumeration<JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            addModuleClass(entries.nextElement().getName(), path, result);
                        }
                    }
                } else if ("file".equals(url.getProtocol())) {
                    Path directory = new File(url.toURI()).toPath();
                    if (Files.isDirectory(directory)) {
                        try (Stream<Path> files = Files.walk(directory)) {
                            files.filter(Files::isRegularFile)
                                    .filter(file -> file.getFileName().toString().endsWith(".class"))
                                    .forEach(file -> {
                                        String relativeName = directory.relativize(file).toString()
                                                .replace(File.separatorChar, '/');
                                        addModuleClass(path + "/" + relativeName, path, result);
                                    });
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>(result);
    }
    
    @SuppressWarnings("unchecked")
private void addModuleClass(
        String entryName,
        String packagePath,
        Set<Class<? extends Module>> result) {

    if (!entryName.startsWith(packagePath + "/")
            || !entryName.endsWith(".class")
            || entryName.endsWith("module-info.class")
            || entryName.contains("$")) {
        return;
    }

    String className = entryName
            .substring(0, entryName.length() - 6)
            .replace('/', '.');

    try {
        Class<?> clazz = Class.forName(className);

        if (Module.class.isAssignableFrom(clazz)
                && clazz != Module.class
                && !clazz.isInterface()
                && !Modifier.isAbstract(clazz.getModifiers())
                && clazz.getEnclosingClass() == null) {

            result.add((Class<? extends Module>) clazz);
        }

    } catch (ClassNotFoundException ignored) {
    }
}
public void init() {
    autoRegisterModules();
}

private void autoRegisterModules() {
    List<Class<? extends Module>> moduleClasses =
            scanPackageForModules("myau.module");

    moduleClasses.sort(
            Comparator.comparing(Class::getSimpleName)
    );

    for (Class<? extends Module> clazz : moduleClasses) {
        try {
            Module module = clazz.getDeclaredConstructor().newInstance();

            modules.put(clazz, module);

            registerModule(module);

            System.out.println(
                    "Registered module: " + clazz.getName()
            );

        } catch (Exception e) {
            System.err.println(
                    "Failed to instantiate module: " + clazz.getName()
            );
            e.printStackTrace();
        }
    }
}


private void registerModule(Module module) {
    ArrayList<Property<?>> properties = new ArrayList<>();

    for (Field field : module.getClass().getDeclaredFields()) {
        field.setAccessible(true);

        try {
            Object obj = field.get(module);

            if (obj instanceof Property<?>) {
                Property<?> property = (Property<?>) obj;

                property.setOwner(module);
                properties.add(property);
            }

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    propertyManager.properties.put(
            module.getClass(),
            properties
    );

    EventManager.register(module);
}
    @EventTarget
    public void onKey(KeyEvent event) {
        for (Module module : this.modules.values()) {
            if (module.getKey() != event.getKey()) {
                continue;
            }
            boolean shouldNotify = module.toggle();
            HUD hud = (HUD) this.modules.get(HUD.class);
            if (hud != null && shouldNotify) {
                shouldNotify = hud.toggleAlerts.getValue();
            }
            if(module instanceof GuiModule){
                shouldNotify = false;
            }
            if (shouldNotify) {
                String status = module.isEnabled() ? "&a&lON" : "&c&lOFF";
                String message = String.format("%s%s: %s&r", Myau.clientName, module.getName(), status);
                ChatUtil.sendFormatted(message);
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.sound) {
                this.sound = false;
                SoundUtil.playSound("random.click");
            }
        }
    }
}
