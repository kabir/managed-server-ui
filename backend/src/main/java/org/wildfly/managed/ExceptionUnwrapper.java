package org.wildfly.managed;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ExceptionUnwrapper<T extends Throwable> {
    Map<Class<T>, Supplier<ServerException>> throwers = new LinkedHashMap<>();

    private ExceptionUnwrapper() {
        this.throwers = throwers;
    }

    static <T extends Throwable> ExceptionUnwrapper create(Class<T> exceptionClass, Supplier<ServerException> thrower) {
        ExceptionUnwrapper unwrapper = new ExceptionUnwrapper();
        unwrapper.add(exceptionClass, thrower);
        return unwrapper;
    }

    ExceptionUnwrapper add(Class<T> exceptionClass, Supplier<ServerException> thrower) {
        throwers.put(exceptionClass, thrower);
        return this;
    }

    ServerException throwServerException(Throwable t) {
        Throwable cause = t;
        while (cause != null) {
            for (Map.Entry<Class<T>, Supplier<ServerException>> entry : throwers.entrySet()) {
                System.out.println("--->" + entry.getKey());
                if (cause.getClass().isAssignableFrom(entry.getKey())) {
                    throw entry.getValue().get();
                }
            }
            cause = cause.getCause();
        }
        if (t instanceof RuntimeException) {
            throw (RuntimeException)t;
        }
        if (t instanceof Error) {
            throw (Error)t;
        }
        throw new RuntimeException(t);
    }
}
