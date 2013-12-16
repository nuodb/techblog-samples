/****************************************************************************
 * Copyright (c) 2013, NuoDB, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NuoDB, Inc. nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NUODB, INC. BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ****************************************************************************/
package com.nuodb.diff;

import java.util.AbstractList;
import java.util.Collections;
import java.util.List;

/**
 * TestDiff generates an edit script to turn its
 * first argument into its second.
 */
public class TestDiff
{
    public static void main(String[] argv)
    throws Exception
    {
        boolean coalesce = false;

        if (argv.length < 2) {
            System.err.printf("Usage:\tTestDiff <originalText> <newText> [-c]\n");
            System.err.printf("\t-c\tcoalesce element-by-element add/delete entries into chunks.\n");
            System.exit(1);
        }

        if (argv.length == 3 && argv[2].equals("-c")) {
            coalesce = true;
        }

        List<Character>  s1 = asList(argv[0]);
        List<Character>  s2 = asList(argv[1]);

        List<Difference> differences = new DiffEngine<Character>(s1, s2).getDifferences();

        if (coalesce) {
            for (Object nextChunk: DiffEngine.coalesceRegions(differences)) {
                System.out.println(nextChunk);
            }
        } else {
            for (Difference nextDiff: differences) {
                System.out.println(nextDiff);
            }
        }
    }

    /**
     * Present a String as a List of Characters.
     * @param string the string.
     * @return a List of Characters, backed by the string.
     */
    public static List<Character> asList(final String string) {

        // The string -empty- is a special test harness construct
        // that means "use an empty list." 
        if (! "-empty-".equals(string)) {
            return new AbstractList<Character>() {
               public int size() { return string.length(); }
               public Character get(int index) { return string.charAt(index); }
            };
        } else {
            return Collections.<Character>emptyList();
        }
    }
    
}
