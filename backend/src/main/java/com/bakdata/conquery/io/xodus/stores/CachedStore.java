package com.bakdata.conquery.io.xodus.stores;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.bakdata.conquery.io.jackson.Injectable;
import com.bakdata.conquery.io.jackson.serializer.IdReferenceResolvingException;
import com.bakdata.conquery.models.exceptions.JSONException;
import com.bakdata.conquery.util.io.ProgressBar;
import com.google.common.base.Stopwatch;
import com.jakewharton.byteunits.BinaryByteUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor @Slf4j
public class CachedStore<KEY, VALUE> implements Store<KEY, VALUE> {

	private static final  ProgressBar PROGRESS_BAR = new ProgressBar(0, System.out);
	
	private ConcurrentHashMap<KEY, VALUE> cache = new ConcurrentHashMap<>();
	private final Store<KEY, VALUE> store;
	
	@Override
	public void close() throws IOException {
	}

	@Override
	public void add(KEY key, VALUE value) throws JSONException {
		if(cache.putIfAbsent(key, value)!=null) {
			throw new IllegalStateException("The id "+key+" is already part of this store");
		}
		store.add(key, value);
	}

	@Override
	public VALUE get(KEY key) {
		// TODO: 08.01.2020 fk: This assumes that all values have been read at some point!
		return cache.get(key);
	}

	@Override
	public void forEach(StoreEntryConsumer<KEY, VALUE> consumer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void update(KEY key, VALUE value) throws JSONException {
		cache.put(key, value);
		store.update(key, value);
	}

	@Override
	public void remove(KEY key) {
		cache.remove(key);
		store.remove(key);
	}
	
	@Override
	public int count() {
		if(cache.isEmpty()) {
			return store.count();
		}
		else {
			return cache.size();
		}
	}

	@Override
	public void fillCache() {
		log.debug("Filling cache for {}[{}]", this.getClass().getSimpleName(), this);


		AtomicLong totalSize = new AtomicLong(0);
		int count = count();
		cache = new ConcurrentHashMap<KEY, VALUE>(count);
		final ProgressBar bar;
		Stopwatch timer = Stopwatch.createStarted();

		if (count > 100) {
			bar = new ProgressBar(count, System.out);
			log.info("\tloading store {}", this);
		}
		else {
			bar = null;
		}
		
		store.forEach((key, value, size) -> {
			try {
				totalSize.addAndGet(size);
				cache.put(key, value);
			}
			catch (RuntimeException e) {
				if (e.getCause() != null && e.getCause() instanceof IdReferenceResolvingException) {
					log.warn("Probably failed to read id '{}' because it is not yet present, skipping",
						((IdReferenceResolvingException) e.getCause()).getValue(),
						e
					);
				}
				else {
					throw  e;
				}
			}
			finally {
				if (bar != null) {
					bar.addCurrentValue(1);
				}
			}
		});
		log.info(
				"\tloaded store {}: {} entries, {} within {}",
				this,
				cache.values().size(),
				BinaryByteUnit.format(totalSize.get()),
				timer.stop()
		);
	}

	@Override
	public Collection<VALUE> getAll() {
		return cache.values();
	}

	@Override
	public void inject(Injectable injectable) {
		store.inject(injectable);
	}
	
	@Override
	public String toString() {
		return "cached "+store.toString();
	}

	@Override
	public Collection<KEY> getAllKeys() {
		return cache.keySet();
	}
}
