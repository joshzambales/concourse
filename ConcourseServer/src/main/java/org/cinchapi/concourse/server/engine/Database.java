/*
 * The MIT License (MIT)
 * 
 * Copyright (c) 2013 Jeff Nelson, Cinchapi Software Collective
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.cinchapi.concourse.server.engine;

import java.util.Map;
import java.util.Set;

import org.cinchapi.common.io.Byteable;
import org.cinchapi.common.tools.Transformers;
import org.cinchapi.concourse.thrift.Operator;
import org.cinchapi.concourse.thrift.TObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.cinchapi.concourse.server.engine.PrimaryRecord.loadPrimaryRecord;
import static org.cinchapi.concourse.server.engine.SecondaryIndex.loadSecondaryIndex;
import static org.cinchapi.concourse.server.engine.SearchIndex.loadSearchIndex;

/**
 * The {@code Database} is the permanent {@link Destination} for {@link Write}
 * objects that are initially stored in a {@link Buffer}.
 * <p>
 * When the Database accepts a write, it creates relevant indexes for efficient
 * retrieval, query and search reads.
 * </p>
 * 
 * @author jnelson
 */
public class Database implements Readable, Destination {

	/**
	 * Return a {@link Runnable} that will execute the appropriate write
	 * function in {@code record} to store {@code write} in the {@link Database}
	 * .
	 * 
	 * @param record
	 * @param write
	 * @return the appropriate Runnable
	 */
	public static <L extends Byteable, K extends Byteable, V extends Storable> Runnable getWriteRunnable(
			final Record<L, K, V> record, final Write write) {
		return new Runnable() {

			@Override
			public void run() {
				log.debug("Writing {} to {}...", write, record);
				if(record instanceof PrimaryRecord) {
					if(write.getType() == WriteType.ADD) {
						((PrimaryRecord) record).add(write.getKey(),
								write.getValue());
					}
					else if(write.getType() == WriteType.REMOVE) {
						((PrimaryRecord) record).remove(write.getKey(),
								write.getValue());
					}
					else {
						throw new IllegalArgumentException();
					}
				}
				else if(record instanceof SecondaryIndex) {
					if(write.getType() == WriteType.ADD) {
						((SecondaryIndex) record).add(write.getValue(),
								write.getRecord());
					}
					else if(write.getType() == WriteType.REMOVE) {
						((SecondaryIndex) record).remove(write.getValue(),
								write.getRecord());
					}
					else {
						throw new IllegalArgumentException();
					}
				}
				else if(record instanceof SearchIndex) {
					if(write.getType() == WriteType.ADD) {
						((SearchIndex) record).add(write.getValue(),
								write.getRecord());
					}
					else if(write.getType() == WriteType.REMOVE) {
						((SearchIndex) record).remove(write.getValue(),
								write.getRecord());
					}
					else {
						throw new IllegalArgumentException();
					}
				}
				else {
					throw new IllegalArgumentException();
				}
				record.fsync();
			}

		};
	}

	private static final String threadNamePrefix = "database-write-thread";

	private static final Logger log = LoggerFactory.getLogger(Database.class);

	@Override
	public void accept(Write write) {
		Threads.executeAndAwaitTermination(threadNamePrefix, Database
				.getWriteRunnable(loadPrimaryRecord(write.getRecord()), write),
				Database.getWriteRunnable(loadSecondaryIndex(write.getKey()),
						write), Database.getWriteRunnable(
						loadSearchIndex(write.getKey()), write));
	}

	@Override
	public Map<Long, String> audit(long record) {
		return loadPrimaryRecord(PrimaryKey.notForStorage(record)).audit();
	}

	@Override
	public Map<Long, String> audit(String key, long record) {
		return loadPrimaryRecord(PrimaryKey.notForStorage(record)).audit(
				Text.fromString(key));
	}

	@Override
	public Set<String> describe(long record) {
		return Transformers.transformSet(
				loadPrimaryRecord(PrimaryKey.notForStorage(record)).describe(),
				Functions.TEXT_TO_STRING);
	}

	@Override
	public Set<String> describe(long record, long timestamp) {
		return Transformers.transformSet(
				loadPrimaryRecord(PrimaryKey.notForStorage(record)).describe(
						timestamp), Functions.TEXT_TO_STRING);
	}

	@Override
	public Set<TObject> fetch(String key, long record) {
		return Transformers.transformSet(
				loadPrimaryRecord(PrimaryKey.notForStorage(record)).fetch(
						Text.fromString(key)), Functions.VALUE_TO_TOBJECT);
	}

	@Override
	public Set<TObject> fetch(String key, long record, long timestamp) {
		return Transformers.transformSet(
				loadPrimaryRecord(PrimaryKey.notForStorage(record)).fetch(
						Text.fromString(key), timestamp),
				Functions.VALUE_TO_TOBJECT);
	}

	@Override
	public Set<Long> find(long timestamp, String key, Operator operator,
			TObject... values) {
		return Transformers.transformSet(
				loadSecondaryIndex(Text.fromString(key)).find(
						timestamp,
						operator,
						Transformers.transformArray(values,
								Functions.TOBJECT_TO_VALUE, Value.class)),
				Functions.PRIMARY_KEY_TO_LONG);
	}

	@Override
	public Set<Long> find(String key, Operator operator, TObject... values) {
		return Transformers.transformSet(
				loadSecondaryIndex(Text.fromString(key)).find(
						operator,
						Transformers.transformArray(values,
								Functions.TOBJECT_TO_VALUE, Value.class)),
				Functions.PRIMARY_KEY_TO_LONG);
	}

	@Override
	public boolean ping(long record) {
		return loadPrimaryRecord(PrimaryKey.notForStorage(record)).ping();
	}

	@Override
	public Set<Long> search(String key, String query) {
		return Transformers.transformSet(loadSearchIndex(Text.fromString(key))
				.search(Text.fromString(query)), Functions.PRIMARY_KEY_TO_LONG);
	}

	@Override
	public boolean verify(String key, TObject value, long record) {
		return loadPrimaryRecord(PrimaryKey.notForStorage(record)).verify(
				Text.fromString(key), Value.notForStorage(value));
	}

	@Override
	public boolean verify(String key, TObject value, long record, long timestamp) {
		return loadPrimaryRecord(PrimaryKey.notForStorage(record)).verify(
				Text.fromString(key), Value.notForStorage(value), timestamp);
	}
}
