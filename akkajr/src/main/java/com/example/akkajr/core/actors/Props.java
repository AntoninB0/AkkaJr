package com.example.akkajr.core.actors;

import java.lang.reflect.Constructor;
import java.util.Arrays;

public final class Props {
    private final Class<? extends Actor> actorClass;
    private final Object[] args;

    private Props(Class<? extends Actor> actorClass, Object[] args) {
        this.actorClass = actorClass;
        this.args = args == null ? new Object[0] : Arrays.copyOf(args, args.length);
    }

    public static Props create(Class<? extends Actor> actorClass, Object... args) {
        if (actorClass == null) {
            throw new IllegalArgumentException("Actor class cannot be null");
        }
        return new Props(actorClass, args);
    }

    Actor instantiate() {
        try {
            if (args.length == 0) {
                return actorClass.getDeclaredConstructor().newInstance();
            }
            for (Constructor<?> ctor : actorClass.getDeclaredConstructors()) {
                Class<?>[] types = ctor.getParameterTypes();
                if (types.length != args.length) {
                    continue;
                }
                boolean match = true;
                for (int i = 0; i < types.length; i++) {
                    if (args[i] != null && !types[i].isAssignableFrom(args[i].getClass())) {
                        match = false;
                        break;
                    }
                }
                if (!match) {
                    continue;
                }
                ctor.setAccessible(true);
                return (Actor) ctor.newInstance(args);
            }
            throw new IllegalArgumentException("No matching constructor found for " + actorClass.getSimpleName());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate actor " + actorClass.getSimpleName(), e);
        }
    }
}
