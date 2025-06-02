package api;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Context {
    private static final ThreadLocal<Map<String, Object>> threadLocalContext =
            ThreadLocal.withInitial(ConcurrentHashMap::new);

    public void set(String key, Object value) {
        threadLocalContext.get().put(key, value);
    }

    public <T> T get(String key, Class<T> type) {
        return type.cast(threadLocalContext.get().get(key));
    }

    public void reset() {
        threadLocalContext.get().clear();
    }
}