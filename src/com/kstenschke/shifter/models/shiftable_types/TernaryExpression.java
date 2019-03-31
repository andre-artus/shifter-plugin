/*
 * Copyright 2011-2019 Kay Stenschke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kstenschke.shifter.models.shiftable_types;

import com.kstenschke.shifter.models.ActionContainer;
import com.kstenschke.shifter.models.ShiftableTypeAbstract;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ternary Expression
 */
public class TernaryExpression extends ShiftableTypeAbstract {

    private ActionContainer actionContainer;

    public static final String ACTION_TEXT = "Shift Ternary Expression";

    public TernaryExpression(@Nullable ActionContainer actionContainer) {
        super(actionContainer);
    }

    /**
     * Check whether shifted string is a ternary expression
     *
     * @return boolean
     */
    public boolean isApplicable() {
        String expression = actionContainer.selectedText.trim();

        return (
            expression.startsWith("?") || "?".equals(actionContainer.prefixChar)
            && (expression.contains(":") && !expression.endsWith(":") && !expression.startsWith(":"))
            && expression.length() >= 3
            && ("?".equals(actionContainer.prefixChar) || expression.indexOf("?") < expression.indexOf(":"))
        );
    }

    /**
     * Shift: swap IF and ELSE parts
     *
     * @param  str      string to be shifted
     * @return String   The shifted string
     */
    public String getShifted(
            String str,
            ActionContainer actionContainer,
            Integer moreCount,
            String leadingWhiteSpace
    ) {
        int offsetElse = str.indexOf(":");
        if (-1 == offsetElse) {
            return str;
        }

        boolean endsWithSemicolon    = str.endsWith(";");
        boolean isQuestionMarkInline = str.startsWith("?");

        if (isQuestionMarkInline) {
            str = str.substring(1);
        }

        Pattern pattern = Pattern.compile("\n[ |\t]*:");
        Matcher matcher = pattern.matcher(str);
        boolean isElseOnNewLine = matcher.find();

        String partThan = str.substring(0, offsetElse - 1);
        String partElse = endsWithSemicolon ? str.substring(offsetElse, str.length() - 1) : str.substring(offsetElse);

        // Detect and maintain "glue" w/ (single) whitespace
        boolean wrapWithSpace = partThan.endsWith(" ") || partElse.startsWith(" ");
        boolean wrapWithTab = partThan.endsWith("\t") || partElse.startsWith("\t");

        String glue = wrapWithSpace ? " " : (wrapWithTab ? "\t" : "");

        str = partElse.trim() + (isElseOnNewLine ? "\n" : "") + glue + ":" + glue + partThan.trim();

        if (isQuestionMarkInline) {
            str = "?" + glue + str;
        }

        return endsWithSemicolon ? str + ";" : str;
    }
}