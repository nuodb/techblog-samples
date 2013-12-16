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

/**
 * A Difference represents a single editing operation on
 * the pair "Source A" and "Source B" which will serve to
 * change "Source A" to correspond to "Source B."
 */
public class Difference implements Coalescible
{
    /**
     * Construct a Difference.
     * @param positionA the position in "Source A" where the change was detected.
     * @param positionB the position in "Source B" where the change was detected.
     * @param type the type of change: Insert or Remove.
     * @param content the content of the changed element.
     */
    public Difference(int positionA, int positionB, EditType type, Object content)
    {
        this.positionA  = positionA;
        this.positionB  = positionB;
        this.type       = type;
        this.content    = content;
    }

    /**
     * Position in "Source A" where the change was detected.
     */
    public final int    positionA;
        
    /**
     * Position in "Source B" where the change was detected.
     */
    public final int    positionB;

    /**
     * EditType of edit.
     */
    public final EditType   type;

    /**
     * The content of the change.
     */
    public final Object content;

    /**
     * @return true if this is an Insert difference.
     */
    public boolean isInsert()
    {
        return type == EditType.Insert;
    }

    /**
     * @return true if this is a Remove difference.
     */
    public boolean isRemove()
    {
        return type == EditType.Remove;
    }

    /**
     * @return this difference in diff format.
     */
    public String toString()
    {
        return String.format("%d%s%d\n%s %s", positionA, type.getOperator(), positionB, type.getIndicator(), content);
    }

    /**
     * @see Coalescible#canCoalesce(Difference)
     */
    public boolean canCoalesce(Difference b)
    {
        if (this.isRemove()) {

            if (b.isInsert()) {
                return this.positionA == b.positionA;
            } else {
                return this.positionA + 1 == b.positionA;
            }
        } else {

            if (b.isInsert()) {
                return this.positionB + 1 == b.positionB;
            } else {
                return b.positionB == this.positionB;
            }
        }
    }

    /**
     * @see Coalescible#coalesce(Difference)
     */
    public Coalescible coalesce(Difference b)
    {
        return new Chunk(this, b);
    }

    /**
     * The type of an edit operation, with embedded formatting information.
     */
    public enum EditType
    {
        Insert("a", ">"),
        Remove("d", "<");

        /**
         * Construct a EditType.
         * @param operator the diff "operator" header.
         * @param indicator the diff "which source is this" indicator string.
         */
        EditType(String operator, String indicator)
        {
            this.operator   = operator;
            this.indicator  = indicator;
        }

        private final String operator;
        private final String indicator;

        /**
         * @return the diff "operator" header of this EditType.
         */
        public String getOperator()
        {
            return this.operator;
        }

        /**
         * @return the diff "which source is this" indicator string of this EditType.
         */
        public String getIndicator()
        {
            return this.indicator;
        }
    }
}
