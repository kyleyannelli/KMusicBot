package dev.kmfg.musicbot.core.util.sessions;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe queue that holds a limited number of elements.
 * When an attempt is made to add an element beyond its capacity, 
 * 	the oldest element in the queue is automatically removed to 
 * 	make space for the new element.
 * 
 * @param <E> the type of elements held in this queue
 */
public class LimitedQueue<E> extends LinkedBlockingDeque<E> {
	private final ReentrantLock lock;

	public LimitedQueue(int capacity) {
		super(capacity);

		lock = new ReentrantLock(true);
	}

	@Override
	public boolean add(E e) {
		final ReentrantLock lock = this.lock;
		lock.lock();

		try {
			// while the queue is full remove last item (FIFO)
			//  on true that means item went in, since ! its false
			while(!super.offer(e)) {
				super.remove();
			}
		}
		finally {
			lock.unlock();
		}

		return true;
	}
}

