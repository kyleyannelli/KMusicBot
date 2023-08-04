package dev.kmfg.helpers;

import dev.kmfg.exceptions.AlreadyAccessedException;

import java.util.concurrent.atomic.AtomicReference;

public class SingleUse<T> {
	private AtomicReference<T> value = new AtomicReference<>();

	public SingleUse(T val) {
		this.value.set(val);
	}

	public T get() throws AlreadyAccessedException {
		T val = value.getAndSet(null);

		if(value == null) {
			throw new AlreadyAccessedException();
		}

		return val;
	}
}
