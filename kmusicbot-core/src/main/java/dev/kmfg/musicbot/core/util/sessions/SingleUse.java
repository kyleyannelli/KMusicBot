package dev.kmfg.musicbot.core.util.sessions;

import dev.kmfg.musicbot.core.exceptions.AlreadyAccessedException;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Thread safe container to ensure a value only gets accessed once.
 *
 * This class was created while I was learning and enjoying Rust.
 * I thought this container might help pin point misused passing/setting of
 * variables.
 *
 * There are no isPresent or isEmpty methods because the behavior of your
 * program should not be checking for this. This class is designed to
 * essentially assert behavior.
 */
public class SingleUse<T> {
    private AtomicReference<T> value = new AtomicReference<>();

    public SingleUse(T val) {
        this.value.set(val);
    }

    /**
     * Get the value. After getting, it is set to null. Consequently, attempting to
     * access again will throw an {@link AlreadyAccessedException}.
     */
    public T get() throws AlreadyAccessedException {
        T val = value.getAndSet(null);

        if (val == null) {
            throw new AlreadyAccessedException();
        }

        return val;
    }
}
