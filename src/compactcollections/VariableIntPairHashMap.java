// Copyright (c) 2013 Gratian Lup. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
// * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//
// * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following
// disclaimer in the documentation and/or other materials provided
// with the distribution.
//
// * The name "CompactCollections" must not be used to endorse or promote
// products derived from this software without prior written permission.
//
// * Products derived from this software may not be called "CompactCollections" nor
// may "CompactCollections" appear in their names without prior written
// permission of the author.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
package compactcollections;
import java.io.Serializable;
import java.util.*;

public class VariableIntPairHashMap extends AbstractMap<Map.Entry<Integer, Integer>, Integer> implements Serializable {
    public static class KeyEntry implements Map.Entry<Integer, Integer> {
        private int key;
        private int value;

        public KeyEntry(int key, int value) {
            this.key = key;
            this.value = value;
        }

        public Integer getKey() {
            return key;
        }

        public Integer getValue() {
            return value;
        }

        public Integer setValue(Integer newValue) {
            Integer oldValue = value;
            value = newValue;
            return oldValue;
        }

        public boolean equals(Object object) {
            if (!(object instanceof KeyEntry)) {
                return false;
            }

            KeyEntry other = (KeyEntry)object;
            return key ==  other.getKey() &&
                   value == other.getValue();
        }

        public int hashCode() {
            return key ^ value;
        }
    }


    public static class MapEntry implements Map.Entry<Map.Entry<Integer, Integer>, Integer> {
        private KeyEntry key;
        private Integer value;

        public MapEntry(KeyEntry key, Integer value) {
            this.key = key;
            this.value = value;
        }

        public Map.Entry<Integer, Integer> getKey() {
            return key;
        }

        public Integer getValue() {
            return value;
        }

        public Integer setValue(Integer newValue) {
            Integer oldValue = value;
            value = newValue;
            return oldValue;
        }

        public boolean equals(Object object) {
            if (!(object instanceof MapEntry)) {
                return false;
            }

            MapEntry other = (MapEntry)object;
            return ((key != null) && key.equals(other.getKey())) &&
                   ((value != null) && value.equals(other.getValue()));
        }

        public int hashCode() {
            return (key != null ? key.hashCode() : 0) ^
                   (value != null ? value.hashCode() : 0);
        }
    }


    // The size of the hash table. Each item in the table "points" to
    // (is the index of) the first value having that hash code.
    private static final int DEFAULT_TABLE_SIZE = 8;

    // The size of the table containing the buckets of the hash table.
    // Each element contains the key and "points" to the next entry in the bucket.
    // Note that all buckets are stored interleaved in the same array.
    private static final int DEFAULT_BUCKET_TABLE_SIZE = 32;

    // The size of the table containing the actual values.
    // The values are stored in the order they were added.
    private static final int DEFAULT_DATA_TABLE_SIZE = 32;

    // The load factor of the hash table (how much space is used).
    // Increasing the load factor reduces the memory consumption,
    // but increases search time, in worst case reaching linear search.
    // A value of 4 seems to be the best in most cases.
    private static final int LOAD_FACTOR = 4;

    private int count;                   // The total number of values in the map.
    private int[] table;                 // Start index of buckets.
    private int[] buckets;               // Next Table Index.
    private VariableIntArray firstKeys;  // The first keys, stored with variable-sizes integers.
    private VariableIntArray secondKeys; // The second keys, stored with variable-sizes integers.
    private VariableIntArray data;       // The values, stored with variable-sizes integers.

    public VariableIntPairHashMap() {
        resetToDefault();
    }

    public VariableIntPairHashMap(Set<Entry<Entry<Integer, Integer>, Integer>> values) {
        this();
        for(Entry<Entry<Integer, Integer>, Integer> pair : values) {
            put(pair.getKey(), pair.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Integer get(Object key) {
        if(!(key instanceof Entry)) {
            throw new IllegalArgumentException("Key is not a Map.Entry integer pair!");
        }

        Entry<Integer, Integer> entry = (Entry<Integer, Integer>)key;
        int value = get(entry.getKey(), entry.getValue());
        return value != Integer.MIN_VALUE ? value : null;
    }

    @Override
    public Integer put(Entry<Integer, Integer> key, Integer value) {
        int tempKey1 = key.getKey();
        int tempKey2 = key.getValue();
        int tempValue = (Integer)value;
        int previousValue = put(tempKey1, tempKey2, tempValue);
        return previousValue != Integer.MIN_VALUE ? previousValue : null;
    }

    @Override
    public void clear() {
        resetToDefault();
    }

    @Override
    public int size() {
        return count;
    }

    @Override
    public Set<Entry<Entry<Integer, Integer>, Integer>> entrySet() {
        return new AbstractSet<Entry<Entry<Integer, Integer>, Integer>>() {
            @Override
            public Iterator<Entry<Entry<Integer, Integer>, Integer>> iterator() {
                // Not the most efficient implementation, building
                // the pair list could be avoided by using a custom iterator.
                List<Entry<Entry<Integer, Integer>, Integer>> valuePairs =
                    new ArrayList<Entry<Entry<Integer, Integer>, Integer>>(count);



                return valuePairs.iterator();
            }

            @Override
            public int size() {
                return count;
            }
        };
    }

    private void resetToDefault() {
        table = new int[DEFAULT_TABLE_SIZE];
        Arrays.fill(table, -1);

        buckets = new int[DEFAULT_BUCKET_TABLE_SIZE];
        firstKeys = new VariableIntArray();
        secondKeys = new VariableIntArray();
        data = new VariableIntArray();
        count = 0;
    }

    private int getNextTableSize(int currentSize) {
        // Don't let the hash table grow beyond 32 bit indices.
        long nextSize = currentSize * 2;
        return nextSize < Integer.MAX_VALUE ? (int)nextSize : currentSize;
    }

    private void resizeTables(int requiredSize) {
        if(requiredSize >= buckets.length) {
            int[] newBuckets = new int[buckets.length * 2];
            System.arraycopy(buckets, 0, newBuckets, 0, buckets.length);
            buckets = newBuckets;
        }

        if((count / LOAD_FACTOR) >= table.length) {
            // Resize the table and rehash the start values.
            int newTableSize = getNextTableSize(table.length);

            if(newTableSize <= table.length) {
                return; // Table shouldn't grow further.
            }

            // Create a new table and rehash the bucket start keys
            // into the new table. On conflict the buckets are chained.
            table = new int[newTableSize];
            Arrays.fill(table, -1);

            for(int i = 0; i < count; i++) {
                int keyHash = computePairHash(i);
                buckets[i] = table[keyHash];
                table[keyHash] = i;
            }
        }
    }

    private int computeHash(int firstKey, int secondKey) {
        // The table length should always be a power of two.
        assert((table.length & (table.length - 1)) == 0);
        int hash = 23;
        hash = hash * 31 + firstKey;
        hash = hash * 31 + secondKey;
        return hash & (table.length - 1);
    }

    private int computePairHash(int pairIndex) {
        int firstKey = firstKeys.get(pairIndex);
        int secondKey = secondKeys.get(pairIndex);
        return computeHash(firstKey, secondKey);
    }

    private boolean pairIsKey(int pairIndex, int firstKey, int secondKey) {
        return (firstKey == firstKeys.get(pairIndex)) &&
               (secondKey == secondKeys.get(pairIndex));
    }

    private int findBucketIndex(int firstKey, int secondKey, boolean returnLast) {
        int keyHash = computeHash(firstKey, secondKey);
        int bucketIndex = table[keyHash];
        int lastBucketIndex = bucketIndex;

        while(bucketIndex != -1) {
            if(pairIsKey(bucketIndex, firstKey, secondKey)) {
                return bucketIndex;
            }
            else {
                lastBucketIndex = bucketIndex;
                bucketIndex = buckets[bucketIndex];
            }
        }

        return returnLast ? lastBucketIndex : -1;
    }

    public int get(int firstKey, int secondKey) {
        int dataIndex = findBucketIndex(firstKey, secondKey,
                                        false /* returnLast */);
        if(dataIndex != -1) {
            return data.get(dataIndex);
        }
        else return Integer.MIN_VALUE;
    }

    private int appendData(int firstKey, int secondKey, int value) {
        buckets[count] = -1; // End of bucket chain.
        firstKeys.add(firstKey);
        secondKeys.add(secondKey);
        data.add(value);
        return count++;
    }

    public int put(int firstKey, int secondKey, int value) {
        // Check if the key is already in the table.
        // If it is, the new value is used.
        resizeTables(count);
        int bucketIndex = findBucketIndex(firstKey, secondKey,
                                          true /* returnLast */);
        if(bucketIndex != -1) {
            if(pairIsKey(bucketIndex, firstKey, secondKey)) {
                // The same key has been found.
                int oldValue = data.get(bucketIndex);
                data.set(bucketIndex, value);
                return oldValue;
            }
            else {
                // A new entry must be added at the end of the bucket.
                int dataIndex = appendData(firstKey, secondKey, value);
                buckets[bucketIndex] = dataIndex;
            }
        }
        else {
            // No bucket is associated with the hash code yet.
            int dataIndex = appendData(firstKey, secondKey, value);
            int keyHash = computeHash(firstKey, secondKey);
            table[keyHash] = dataIndex;
        }

        return Integer.MIN_VALUE;
    }

    public boolean containsKey(int firstKey, int secondKey) {
        return findBucketIndex(firstKey, secondKey,
                               false /* returnLast */) != -1;
    }

    public boolean containsValue(int value) {
        for(int i = 0; i < count; i++) {
            if(data.get(i) == value) {
                return true;
            }
        }

        return false;
    }
}
