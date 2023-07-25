package Helpers;

import java.util.concurrent.atomic.AtomicReference;

public class SingleUse<T> {
	private AtomicReference<T> value = new AtomicReference<>();

	public SingleUse(T val) {
		this.value.set(val);
	}

	public T get() throws IllegalStateException {
		T val = value.getAndSet(null);

		if(value == null) {
			throw new IllegalStateException("Value has already been accessed!");
		}

		return val;
	}
}
