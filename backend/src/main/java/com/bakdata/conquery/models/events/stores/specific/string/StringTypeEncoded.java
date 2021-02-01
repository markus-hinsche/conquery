package com.bakdata.conquery.models.events.stores.specific.string;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import javax.annotation.Nonnull;

import com.bakdata.conquery.io.cps.CPSType;
import com.bakdata.conquery.io.xodus.NamespacedStorage;
import com.bakdata.conquery.models.dictionary.Dictionary;
import com.bakdata.conquery.models.events.stores.ColumnStore;
import com.bakdata.conquery.models.identifiable.ids.specific.DictionaryId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.io.BaseEncoding;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 *
 */
@Getter
@Setter
@CPSType(base = ColumnStore.class, id = "STRING_ENCODED")
public class StringTypeEncoded extends StringType {

	@Nonnull
	protected StringTypeDictionary store;
	@NonNull
	private Encoding encoding;

	@JsonCreator
	public StringTypeEncoded(StringTypeDictionary store, Encoding encoding) {
		super();
		this.store = store;
		this.encoding = encoding;
	}

	@Override
	public String getElement(int value) {
		return encoding.encode(store.getElement(value));
	}

	@Override
	public String createScriptValue(Integer value) {
		return getElement(value);
	}

	@Override
	public void loadDictionaries(NamespacedStorage storage) {
		store.loadDictionaries(storage);
	}


	@Override
	public int size() {
		return store.size();
	}

	@Override
	public int getId(String value) {
		return store.getId(encoding.decode(value));
	}

	@Override
	public Iterator<String> iterator() {
		Iterator<byte[]> subIt = store.iterator();
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return subIt.hasNext();
			}

			@Override
			public String next() {
				return encoding.encode(subIt.next());
			}
		};
	}

	@Override
	public String toString() {
		return "StringTypeEncoded[encoding=" + encoding + ", subType=" + store + "]";
	}

	@Override
	public long estimateEventBits() {
		return store.estimateEventBits();
	}

	@Override
	public long estimateMemoryConsumptionBytes() {
		return store.estimateMemoryConsumptionBytes();
	}

	@Override
	public Dictionary getUnderlyingDictionary() {
		return store.getDictionary();
	}

	@Override
	public void setUnderlyingDictionary(DictionaryId newDict) {
		store.setUnderlyingDictionary(newDict);
	}

	@Override
	public void setIndexStore(ColumnStore<Long> newType) {
		store.setIndexStore(newType);
	}

	@Override
	public StringTypeEncoded select(int[] starts, int[] length) {
		return new StringTypeEncoded(store.select(starts, length), getEncoding());
	}

	@Override
	public void set(int event, Integer value) {
		store.set(event, value);
	}

	@Override
	public Integer get(int event) {
		return getString(event);
	}

	@Override
	public int getString(int event) {
		return store.getString(event);
	}

	@Override
	public boolean has(int event) {
		return store.has(event);
	}

	@Override
	public int getLines() {
		return store.getLines();
	}

	@RequiredArgsConstructor
	public static enum Encoding {
		// Order is for precedence, least specific encodings go last.
		Base16LowerCase(2, BaseEncoding.base16().lowerCase().omitPadding()),
		Base16UpperCase(2, BaseEncoding.base16().upperCase().omitPadding()),
		Base32LowerCase(8, BaseEncoding.base32().lowerCase().omitPadding()),
		Base32UpperCase(8, BaseEncoding.base32().upperCase().omitPadding()),
		Base32HexLowerCase(8, BaseEncoding.base32Hex().lowerCase().omitPadding()),
		Base32HexUpperCase(8, BaseEncoding.base32Hex().upperCase().omitPadding()),
		Base64(4, BaseEncoding.base64().omitPadding()),
		UTF8(1, null) {
			@Override
			public String encode(byte[] bytes) {
				return new String(bytes, StandardCharsets.UTF_8);
			}

			@Override
			public byte[] decode(String chars) {
				return chars.getBytes(StandardCharsets.UTF_8);
			}

			@Override
			public boolean canDecode(String chars) {
				return true;
			}
		};

		private final int requiredLengthBase;
		private final BaseEncoding encoding;

		public String encode(byte[] bytes) {
			return encoding.encode(bytes);
		}

		public boolean canDecode(String chars) {
			return encoding.canDecode(chars)
				   && chars.length() % requiredLengthBase == 0;
		}

		public byte[] decode(String chars) {
			return encoding.decode(chars);
		}

	}
}
