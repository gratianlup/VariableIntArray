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
package compactcollections.tests;
import compactcollections.IntPairHashMap;
import org.junit.Assert;
import org.junit.Test;
import java.util.*;

public class IntPairHashMapTest {
    @Test
    public void testPutGet() {
        IntPairHashMap map = new IntPairHashMap();

        for(int i = 0; i < 1000; i++) {
            for(int j = 0; j < 1000; j++) {
                map.put(i, j, i + j);
            }
        }

        for(int i = 0; i < 1000; i++) {
            for(int j = 0; j < 1000; j++) {
                Assert.assertEquals(map.get(i, j), i + j);
            }
        }
    }

    @Test
    public void testPutGetRandom() {
        Random random = new Random(59);
        IntPairHashMap map = new IntPairHashMap();
        Map<IntPairHashMap.KeyEntry, Integer> inserted = new HashMap<IntPairHashMap.KeyEntry, Integer>();

        for(int i = 0; i < 100000; i++) {
            IntPairHashMap.KeyEntry key = new IntPairHashMap.KeyEntry(random.nextInt(),
                                                                      random.nextInt());
            int value = random.nextInt();
            map.put(key, value);
            inserted.put(key, value);
        }

        for(Map.Entry<IntPairHashMap.KeyEntry, Integer> entry : inserted.entrySet()) {
            Assert.assertEquals(map.get(entry.getKey()), entry.getValue());
        }
    }
}
