package com.bakdata.conquery.io.storage.xodus.stores;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.bakdata.conquery.io.storage.IStoreInfo;
import com.google.common.primitives.Ints;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XodusStore {
	@Getter
	private final Store store;
	@Getter
	private final Environment environment;
	private final long timeoutHalfMillis; // milliseconds

	private final Consumer<XodusStore> storeClosed;
	private final Consumer<XodusStore> storeRemoved;

	public XodusStore(Environment env, IStoreInfo storeInfo, Consumer<XodusStore> storeClosed, Consumer<XodusStore> storeRemoved) {
		// Arbitrary duration that is strictly shorter than the timeout to not get interrupted by StuckTxMonitor
		this.timeoutHalfMillis = env.getEnvironmentConfig().getEnvMonitorTxnsTimeout()/2;
		this.environment = env;
		this.storeClosed = storeClosed;
		this.storeRemoved = storeRemoved;

		this.store = env.computeInTransaction(
				t -> env.openStore(storeInfo.getName(), StoreConfig.WITHOUT_DUPLICATES_WITH_PREFIXING, t)
		);
	}
	
	public boolean add(ByteIterable key, ByteIterable value) {
		return environment.computeInTransaction(t -> store.add(t, key, value));
	}

	public ByteIterable get(ByteIterable key) {
		return environment.computeInReadonlyTransaction(t -> store.get(t, key));
	}

	/**
	 * Iterate over all key-value pairs in a consistent manner.
	 * The transaction is read only!
	 * @param consumer function called for-each key-value pair.
	 */
	public void forEach(BiConsumer<ByteIterable, ByteIterable> consumer) {
		AtomicReference<ByteIterable> lastKey = new AtomicReference<>();
		AtomicBoolean done = new AtomicBoolean(false);
		while(!done.get()) {
			environment.executeInReadonlyTransaction(t -> {
				try(Cursor c = store.openCursor(t)) {
					//try to load everything in the same transaction
					//but keep within half of the timeout time
					long start = System.currentTimeMillis();
					//search where we left of
					if(lastKey.get() != null) {
						c.getSearchKey(lastKey.get());
					}
					while(System.currentTimeMillis()-start < timeoutHalfMillis) {
						if(!c.getNext()) {
							done.set(true);
							return;
						}
						lastKey.set(c.getKey());
						consumer.accept(lastKey.get(), c.getValue());
					}
				}
			});
		}
	}

	public boolean update(ByteIterable key, ByteIterable value) {
		return environment.computeInTransaction(t -> store.put(t, key, value));
	}
	
	public boolean remove(ByteIterable key) {
		return environment.computeInTransaction(t -> store.delete(t, key));
	}

	public int count() {
		return Ints.checkedCast(environment.computeInReadonlyTransaction(store::count));
	}


	public void clear() {
		// TODO implement, unused at the moment
	}

	public void remove() {
		log.debug("Removing store {} from environment {}", store, environment.getLocation());
		storeRemoved.accept(this);
	}

	public void close() {
		log.info("Closing XodusStore: {}", this);
		storeClosed.accept(this);
	}

	@Override
	public String toString() {
		return "XodusStore[" + environment.getLocation() + ":" +store.getName() +"}";
	}
}
