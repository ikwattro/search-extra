package org.apache.lucene.util.automaton;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.TestUtil;

/**
 * Not thorough, but tries to test determinism correctness
 * somewhat randomly, by determinizing a huge random lexicon.
 */
public class TestDeterminizeLexicon extends LuceneTestCase {
  private List<XAutomaton> automata = new ArrayList<>();
  private List<String> terms = new ArrayList<>();
  
  public void testLexicon() throws Exception {
    int num = atLeast(1);
    for (int i = 0; i < num; i++) {
      automata.clear();
      terms.clear();
      for (int j = 0; j < 5000; j++) {
        String randomString = TestUtil.randomUnicodeString(random());
        terms.add(randomString);
        automata.add(XAutomata.makeString(randomString));
      }
      assertLexicon();
    }
  }
  
  public void assertLexicon() throws Exception {
    Collections.shuffle(automata, random());
    XAutomaton lex = XOperations.union(automata);
    lex = XOperations.determinize(lex, 1000000);
    assertTrue(XOperations.isFinite(lex));
    for (String s : terms) {
      assertTrue(XOperations.run(lex, s));
    }
    final XByteRunAutomaton lexByte = new XByteRunAutomaton(lex, false, 1000000);
    for (String s : terms) {
      byte bytes[] = s.getBytes(StandardCharsets.UTF_8);
      assertTrue(lexByte.run(bytes, 0, bytes.length));
    }
  }
}
