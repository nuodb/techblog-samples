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

import java.util.ArrayList;
import java.util.List;

/**
 * A Chunk is a coalesced set of element-by-element differences.
 */
public class Chunk implements Coalescible
{
    /**
     * Begin a Chunk by coalescing two Differences.
     * @param a the first Difference.
     * @param b the second Difference.
     */
    public Chunk(Difference a, Difference b)
    {
        this.positionA  = a.positionA;
        if (a.isRemove()) {
            this.positionB  = b.positionB;
        } else {
            this.positionB  = a.positionB;
        }

        addContent(a);
        addContent(b);
    }

    /**
     * Position in "Source A" where the change was detected.
     */
    private int    positionA;
        
    /**
     * Position in "Source B" where the change was detected.
     */
    private int    positionB;

    /**
     * Lines "removed," i.e., present in Source A but not in the LCS.
     */
    public final List<Object> contentR = new ArrayList<Object>();

    /**
     * Lines "added," i.e., present in Source B but not in the LCS.
     */
    public final List<Object> contentA = new ArrayList<Object>();

    /**
     * Add the content of a Difference to the appropriate content list.
     * @param d the Difference of interest.
     */
    private void addContent(Difference d)
    {
        if (d.isInsert()) {
            contentA.add(d.content);
        } else {
            contentR.add(d.content);
        }
    }

    /**
     * Emit this chunk in diff format.
     */
    public String toString()
    {
        if (contentR.size() > 0 && contentA.size() > 0) {
            return String.format(
                "%sc%s%s\n---%s",
                emitContentHeader(positionA, contentR), 
                emitContentHeader(positionB, contentA),
                emitContent(contentR, Difference.EditType.Remove),
                emitContent(contentA, Difference.EditType.Insert)
            );
        } else if (contentR.size() > 0) {
            return String.format(
                "%sd%d%s",
                emitContentHeader(positionA, contentR),
                positionB,
                emitContent(contentR, Difference.EditType.Remove)
            );
        } else {
            return String.format(
                "%da%s%s",
                positionA,
                emitContentHeader(positionB, contentA),
                emitContent(contentA, Difference.EditType.Insert)
            );
        }
    }

    /**
     * Emit the diff-format header of a list of content.
     * @return the header of the content, as a tuple in
     * diff format -- the trailing member of the pair is
     * elided if the size is one.
     */
    private String emitContentHeader(int start, List<Object> content)
    {
        assert(content.size() > 0);
        return content.size() > 1?
            String.format("%d,%d", start, start+content.size()-1):
            Integer.toString(start);
    }

    /**
     * Stringify a list of added/deleted lines in diff format.
     * @param content the added or deleted lines.
     * @param type the content's type.
     */
    private Object emitContent(List<Object> content, Difference.EditType type)
    {
        StringBuilder result = new StringBuilder();
        for (Object o: content) {
            result.append("\n");
            result.append(type.getIndicator());
            result.append(" ");
            result.append(o);
        }

        return result;
    }

    /**
     * @see Coalescible#canCoalesce(Difference)
     */
    public boolean canCoalesce(Difference b)
    {
        if (b.isInsert()) {
            return b.positionA == this.positionA && b.positionB == this.positionB + this.contentA.size();
        } else {
            return b.positionB == this.positionB && b.positionA == this.positionA + this.contentR.size();
        }
    }

    /**
     * @see Coalescible#coalesce(Difference)
     */
    public Coalescible coalesce(Difference b)
    {
        assert(canCoalesce(b));

        addContent(b);
        return this;
    }
}
