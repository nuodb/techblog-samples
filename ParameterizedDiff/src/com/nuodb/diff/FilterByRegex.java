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

import java.io.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import java.util.regex.*;

/**
 * FilterByRegex compares two files, using a set of regular expressions
 * specified in a separate pattern file; lines that match a regex will
 * be compared using the contents of the capturing groups in the first
 * matching regular expression.
 */
public class FilterByRegex
{
    /**
     * Program entry point: process command-line options, load
     * the pattern file and the contents of the files to be 
     * compared, and emit a coalesced list of differences.
     */
    public static void main(String[] argv)
    throws Exception
    {
        // Command line processing.
        String  fileA = null;
        String  fileB = null;
        boolean quietMode = false;

        for (int i = 0; i < argv.length; i++) {
            if (argv[i].equals("-q")) {
                quietMode = true;
            } else if (argv[i].equals("-p") && i+1 < argv.length) {
                loadPatterns(argv[++i]);
            } else if (!argv[i].startsWith("-")) {
                if (fileA == null) {
                    fileA = argv[i];
                } else if (fileB == null) {
                    fileB = argv[i];
                } else {
                    usage("Unrecognized option: %s", argv[i]);
                    System.exit(1);
                }
            } else {
                usage("Unrecognized option: %s", argv[i]);
                System.exit(2);
            }
        }

        if (fileA == null || fileB == null) {
            usage("You must specify expected and actual files.");
            System.exit(3);
        }

        List<AbstractedString> sequenceA = loadContent(fileA);
        List<AbstractedString> sequenceB  = loadContent(fileB);

        // Get a line-by-line list of differences.
        // This list is coalesced into the more familiar
        // diff format by the Chunk.coalesceChunks logic,
        // but it is itself a valid diff (a verbose one).
        DiffEngine<AbstractedString> diff = new DiffEngine<AbstractedString>(sequenceA, sequenceB);
        List<Difference> diagnostics = diff.getDifferences();

        // Emit the list of coalesced differences if requested.
        if (!quietMode) {
            for (Object diagnostic: DiffEngine.coalesceRegions(diagnostics)) {
                System.err.println(diagnostic);
            }
        }

        System.exit(diagnostics.size());
    }

    /**
     * Load the contents of one of the files to be compared.
     * @param filePath a path to the file.
     */
    private static List<AbstractedString> loadContent(String filePath)
    throws IOException
    {
        List<AbstractedString>  result  = new ArrayList<AbstractedString>();
        BufferedReader          in      = new BufferedReader(new FileReader(filePath));

        for (String raw = in.readLine(); raw != null; raw = in.readLine()) {
            result.add(new AbstractedString(raw));
        }

        return result;
    }

    /**
     * Load a file of regualar expression patterns and compile them.
     * @param patternFile a path to the file.
     */
    private static void loadPatterns(String patternFile)
    throws IOException
    {
        BufferedReader in = new BufferedReader(new FileReader(patternFile));

        for (String pattern = in.readLine(); pattern != null; pattern = in.readLine()) {
            AbstractedString.patterns.add(Pattern.compile(pattern));
        }
    }

    /**
     * Display usage information.
     * @param diagnostic a printf-style diagnostic.
     * @param args arguments for the diagnostic.
     */
    private static void usage(String diagnostic, Object... args)
    {
        System.err.printf(diagnostic, args);
        System.err.println();
        System.err.println("Usage: FilterByRegex fileA fileB -p patternFile [-q]");
    }

    /**
     * A AbstractedString is a string (typically a line from a file) that
     * implements the Comparable interface in terms of an abstract of 
     * the original string; the abstract is formed by concatenating the
     * capturing groups of the first regular expression in the pattern
     * library that matches the original text.
     */
    private static class AbstractedString implements Comparable<AbstractedString>
    {
        AbstractedString(String original)
        {
            this.original = original;

            String abstracted = original;

            for (Pattern p: patterns) {
                Matcher matcher  = p.matcher(original);

                if (matcher.matches()) {
                
                    // The groups in the regex are significant.
                    StringBuilder abstractedBuffer = new StringBuilder();

                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        abstractedBuffer.append(matcher.group(i));
                    }

                    abstracted = abstractedBuffer.toString();

                    // First match wins.
                    break;
                }
            }

            this.abstracted = abstracted;
        }

        /**
         * The original string.
         */
        private final String original;

        /**
         * The abstract of the original string; this may be
         * the same as the original string, if no pattern
         * matches the original.
         */
        private final String abstracted;

        /**
         * Patterns specified in the pattern file loaded by -p patternFile command line option.
         */
        private static final List<Pattern> patterns = new ArrayList<Pattern>();

        /**
         * @return the original string.
         */
        public String toString()
        {
            return original;
        }

        /**
         * Define the Comparable interface as a comparison
         * of the abstracted string contents.
         */
        @Override
        public int compareTo(AbstractedString other)
        {
            return this.abstracted.compareTo(other.abstracted);
        }
    }
}
