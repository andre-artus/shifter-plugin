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
package com.kstenschke.shifter.models;

import com.kstenschke.shifter.models.entities.AbstractShiftable;
import com.kstenschke.shifter.models.entities.shiftables.*;
import com.kstenschke.shifter.resources.StaticTexts;
import com.kstenschke.shifter.utils.UtilsTextual;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

// Shiftable (non-block) selection
public class ShiftableSelection {

    static final String ACTION_TEXT_SHIFT_SELECTION = "Shift Selection";

    /**
     * @param actionContainer
     * @param moreCount     Current "more" count, starting w/ 1. If non-more shift: null
     */
    public static void shiftSelectionInDocument(final ActionContainer actionContainer, @Nullable Integer moreCount) {
        if (null == actionContainer.selectedText ||
            actionContainer.selectedText.trim().isEmpty()
        ) return;

        ShiftableTypesManager shiftableTypesManager = new ShiftableTypesManager(actionContainer);

        //int editorTextLength = actionContainer.editorText.length();
        //int offsetPostfixChar = actionContainer.caretOffset + actionContainer.selectedText.length();
        //String postfixChar = editorTextLength > offsetPostfixChar
        //        ? String.valueOf(actionContainer.editorText.charAt(offsetPostfixChar))
        //        : "";
        //boolean isLastLineInDocument = offsetPostfixChar == editorTextLength;
        shiftableTypesManager.setPrefixChar("");

        AbstractShiftable shiftable;

        if (
            // Detect and shift PHPDoc block or single line out of it (containing @param caretLine(s) w/o data type)
            null != (shiftable = new PhpDocParam(actionContainer).getInstance()) ||
            // Shift selected comment: Must be before multi-line sort to allow multi-caretLine comment shifting
            null != (shiftable = new JsDoc(actionContainer).getInstance()) ||
            null != (shiftable = new Comment(actionContainer).getInstance()) ||
            null != (shiftable = new XmlAttributes(actionContainer).getInstance()) ||
            null != (shiftable = new Parenthesis(actionContainer).getInstance()) ||
            null != (shiftable = new Css(actionContainer).getInstance()) ||
            null != (shiftable = new TernaryExpression(actionContainer).getInstance()) ||
            null != (shiftable = new MultipleLines(actionContainer).getInstance()) ||
            null != (shiftable = new JsVariableDeclarations(actionContainer).getInstance()) ||
            null != (shiftable = new JqueryObserver(actionContainer).getInstance()) ||
            null != (shiftable = new SizzleSelector(actionContainer).getInstance()) ||
            null != (shiftable = new TrailingComment(actionContainer).getInstance()) ||
            null != (shiftable = new PhpDocument(actionContainer).getInstance()) ||
            null != (shiftable = new SeparatedList(actionContainer).getInstance()) ||
            null != (shiftable = new SeparatedListPipeDelimited(actionContainer).getInstance()) ||
            null != (shiftable = new ConcatenationJsInTs(actionContainer).getInstance()) ||
            null != (shiftable = new QuotedString(actionContainer).getInstance(null)) ||
            null != (shiftable = new CamelCaseString(actionContainer).getInstance()) ||
            null != (shiftable = new WordPair(actionContainer).getInstance()) ||
            null != (shiftable = new Tupel(actionContainer).getInstance(true)) ||
            null != (shiftable = new StringContainingSlash(actionContainer).getInstance()) ||
            null != (shiftable = new LogicalOperator(actionContainer).getInstance()) ||
            null != (shiftable = new LogicalConjunction(actionContainer).getInstance(null)) ||
            null != (shiftable = new HtmlEncodable(actionContainer).getInstance()) ||
            null != (shiftable = new PhpVariableOrArray(actionContainer).getInstance())
        ) {
            if (shiftable.shiftSelectionInDocument(moreCount)) return;
        }

        // @todo call from within shiftSelectionInDocument()
        try {
            shiftable.replaceSelectionWithShiftedMaintainingCasing(
                    shiftableTypesManager.getShiftedWord(actionContainer, moreCount));
        } catch (NullPointerException e) {
            // silently continue (for now)
        }
    }

    /**
     * Sort lines in document alphabetically ascending / descending
     *
     * @param actionContainer
     * @param reverse
     */
    static void sortLinesInDocument(final ActionContainer actionContainer, boolean reverse) {
        List<String> lines = UtilsTextual.extractLines(
            actionContainer.document, actionContainer.lineNumberSelStart, actionContainer.lineNumberSelEnd);
        UtilsTextual.sortLinesNatural(lines, reverse);
        String linesString = UtilsTextual.joinLines(lines).toString().trim();

        if (UtilsTextual.hasDuplicateLines(linesString) && JOptionPane.showConfirmDialog(
                null,
                StaticTexts.MESSAGE_REDUCE_DUPLICATE_LINES,
                StaticTexts.TITLE_REDUCE_DUPLICATE_LINES,
                JOptionPane.OK_CANCEL_OPTION
        ) == JOptionPane.OK_OPTION)
        {
            linesString = UtilsTextual.reduceDuplicateLines(linesString);
        }

        actionContainer.writeUndoable(actionContainer.getRunnableReplaceSelection(linesString, true), ACTION_TEXT_SHIFT_SELECTION);
    }
}