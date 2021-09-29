package com.bakdata.conquery.io.storage;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;

import javax.validation.Validator;

import com.bakdata.conquery.models.config.StoreFactory;
import com.bakdata.conquery.models.identifiable.CentralRegistry;

public interface ConqueryStorage extends Closeable {

	CentralRegistry getCentralRegistry();

	void openStores(StoreFactory storageFactory);
	
	void loadData();

	/**
	 * Delete the storage's contents.
	 */
	void clear();

	/**
	 * Remove the storage.
	 */
	void removeStorage();
}
