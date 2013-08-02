package compactcollections;
import java.util.*;

public class IntPairObjectHashMap<T> extends AbstractMap<Map.Entry<Integer, Integer>, T> {
    static class KeyEntry implements Map.Entry<Integer, Integer> {
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
            return key == other.getKey() &&
                   value == other.getValue();
        }

        public int hashCode() {
            return key ^ value;
        }
    }


    static class MapEntry<T> implements Map.Entry<Map.Entry<Integer, Integer>, T> {
        private KeyEntry key;
        private T value;

        public MapEntry(KeyEntry key, T value) {
            this.key = key;
            this.value = value;
        }

        public Map.Entry<Integer, Integer> getKey() {
            return key;
        }

        public T getValue() {
            return value;
        }

        public T setValue(T newValue) {
            T oldValue = value;
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


    private static final int DEFAULT_TABLE_SIZE = 8;
    private static final int DEFAULT_BUCKET_TABLE_SIZE = 32;
    private static final int DEFAULT_DATA_TABLE_SIZE = 32;

    private int[] table;   // Start index of buckets.
    private Object[] data; // Value for corresponding Bucket.
    private int[] buckets; // <First Key, Next Table Index> pairs.
    private long[] pairs;  // <First Key, Second Key> pairs.
    private int count;     // The total number of values in the map.

    public IntPairObjectHashMap() {
        resetToDefault();
    }

    public IntPairObjectHashMap(Set<Entry<Entry<Integer, Integer>, T>> values) {
        this();
        for(Entry<Entry<Integer, Integer>, T> pair : values) {
            put(pair.getKey(), pair.getValue());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(Object key) {
        if(!(key instanceof Entry)) {
            throw new IllegalArgumentException("Key is not a Map.Entry integer pair!");
        }

        Entry<Integer, Integer> entry = (Entry<Integer, Integer>)key;
        return get(entry.getKey(), entry.getValue());
    }

    @Override
    public T put(Entry<Integer, Integer> key, T value) {
        int tempKey1 = key.getKey();
        int tempKey2 = key.getValue();
        return put(tempKey1, tempKey2, value);
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
    @SuppressWarnings("unchecked")
    public Set<Entry<Entry<Integer, Integer>, T>> entrySet() {
        return new AbstractSet<Entry<Entry<Integer, Integer>, T>>() {
            @Override
            public Iterator<Entry<Entry<Integer, Integer>, T>> iterator() {
                // Not the most efficient implementation, building
                // the pair list could be avoided by using a custom iterator.
                List<Entry<Entry<Integer, Integer>, T>> valuePairs =
                        new ArrayList<Entry<Entry<Integer, Integer>, T>>(count);

                for(int i = 0; i < count; i++) {
                    KeyEntry key = new KeyEntry(extractFirstKey(pairs[i]),
                                                extractSecondKey(pairs[i]));
                    valuePairs.add(new MapEntry(key, (T)data[i]));
                }

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
        pairs = new long[DEFAULT_BUCKET_TABLE_SIZE];
        data = new Object[DEFAULT_DATA_TABLE_SIZE];
        count = 0;
    }

    private long packValues(int firstKey, int secondKey) {
        return ((long)secondKey << 32) | ((long)firstKey & 0xFFFFFFFFL);
    }

    private int extractFirstKey(long value) {
        return (int)value;
    }

    private int extractSecondKey(long value) {
        return (int)(value >>> 32);
    }

    private int getNextTableSize(int currentSize) {
        // Don't let the hash table grow beyond 32 bit indices.
        long nextSize = currentSize * 2;
        return nextSize < Integer.MAX_VALUE ? (int)nextSize : currentSize;
    }

    private void resizeTables(int requiredSize) {
        if(requiredSize >= data.length) {
            int[] newBuckets = new int[buckets.length * 2];
            System.arraycopy(buckets, 0, newBuckets, 0, buckets.length);
            buckets = newBuckets;

            long[] newPairs = new long[pairs.length * 2];
            System.arraycopy(pairs, 0, newPairs, 0, pairs.length);
            pairs = newPairs;

            Object[] newData = new Object[data.length * 2];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }

        if(count >= table.length / 2) {
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
        long pair = pairs[pairIndex];
        int firstKey = extractFirstKey(pair);
        int secondKey = extractSecondKey(pair);
        return computeHash(firstKey, secondKey);
    }

    private boolean pairIsKey(int pairIndex, int firstKey, int secondKey) {
        long pair = pairs[pairIndex];
        return (firstKey == extractFirstKey(pair)) &&
               (secondKey == extractSecondKey(pair));
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

    @SuppressWarnings("unchecked")
    public T get(int firstKey, int secondKey) {
        int dataIndex = findBucketIndex(firstKey, secondKey,
                                        false /* returnLast */);
        if(dataIndex != -1) {
            return (T)data[dataIndex];
        }
        else return null;
    }

    private int appendData(int firstKey, int secondKey, T value) {
        buckets[count] = -1; // End of bucket chain.
        pairs[count] = packValues(firstKey, secondKey);
        data[count] = value;
        return count++;
    }

    @SuppressWarnings("unchecked")
    public T put(int firstKey, int secondKey, T value) {
        // Check if the key is already in the table.
        // If it is, the new value is used.
        resizeTables(count);
        int bucketIndex = findBucketIndex(firstKey, secondKey,
                                          true /* returnLast */);
        if(bucketIndex != -1) {
            if(pairIsKey(bucketIndex, firstKey, secondKey)) {
                // The same key has been found.
                T oldValue = (T)data[bucketIndex];
                data[bucketIndex] = value;
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

        return null;
    }

    public boolean containsKey(int firstKey, int secondKey) {
        return findBucketIndex(firstKey, secondKey,
                               false /* returnLast */) != -1;
    }
}
