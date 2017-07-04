/*
 * Copyright 2011-2017 Kay Stenschke
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
package com.kstenschke.shifter.models.shiftableTypes;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.kstenschke.shifter.utils.UtilsEnvironment;
import com.kstenschke.shifter.utils.UtilsPhp;
import com.kstenschke.shifter.utils.UtilsTextual;

import static org.apache.commons.lang.StringUtils.trim;

/**
 * JavaScript DOC @param comment
 */
public class JsDoc {

    public static boolean isJsDocBlock(String str) {
        str = trim(str);

        return UtilsTextual.isMultiLine(str)
            && (str.startsWith("/**") && str.endsWith("*/"))
            && (str.contains("@param") || str.contains("@return"));
    }

    /**
     * Check whether given string represents a JsDoc @param comment
     *
     * @param  str     String to be checked
     * @return boolean
     */
    public static boolean isAtParamLine(String str) {
        str = trim(str);

        return str.startsWith("* ") && str.contains("@param");
    }

    public static boolean isInvalidAtReturnsLine(String str) {
        str = trim(str);

        return str.startsWith("*") && str.contains("@return") && !str.contains("@returns");
    }

    private static boolean isAtReturnsLine(String str) {
        str = trim(str);

        return str.startsWith("*") && str.contains("@returns ");
    }

    public static boolean isDataType(String str) {
        return isDataType(str, true);
    }

    public static boolean isWordRightOfAtParamOrAtReturn(String word, String line) {
        if (line.contains("@param")) {
            line = trim(line.split("@param")[1]);
            return line.startsWith(word);
        }
        if (line.contains("@return")) {
            line = trim(line.split("@return")[1]);
            return line.startsWith(word);
        }

        return false;
    }

    private static boolean isDataType(String str, boolean includeInvalidTypes) {
        str = trim(str.toLowerCase());

        if (   str.equals("array")
            || str.equals("boolean")
            || str.equals("date")
            || str.equals("function")
            || str.equals("null")
            || str.equals("number")
            || str.equals("object")
            || str.equals("string")
            || str.equals("symbol")
            || str.equals("undefined")
            || str.equals("*")
        ) {
            return true;
        }

        return includeInvalidTypes && (
                str.equals("bool")
             || str.equals("event")
             || str.equals("float")
             || str.equals("int")
             || str.equals("integer")
             || str.equals("null")
             || str.equals("void")
        );
    }

    public static boolean containsDataType(String str, String lhs, boolean allowInvalidTypes) {
        str = trim(str.toLowerCase());

        if (
           // javaScript primitive data types
              str.contains(lhs + "array")
           || str.contains(lhs + "boolean")
           || str.contains(lhs + "function")
           || str.contains(lhs + "null")
           || str.contains(lhs + "number")
           || str.contains(lhs + "object")
           || str.contains(lhs + "string")
           || str.contains(lhs + "symbol")
           || str.contains(lhs + "undefined")
        ) {
            return true;
        }

        return allowInvalidTypes && (
                str.contains(lhs + "bool")
             || str.contains(lhs + "event")
             || str.contains(lhs + "float")
             || str.contains(lhs + "int")
             || str.contains(lhs + "void")
        );
    }

    public static boolean containsCompounds(String str) {
        return str.contains("{") && str.contains("}");
    }

    /**
     * Actual shifting method
     *
     * @param  word
     * @param  document
     * @param  caretOffset
     * @return boolean
     */
    public static boolean addCompoundsAroundDataTypeAtCaretInDocument(String word, Document document, int caretOffset) {
        return UtilsEnvironment.replaceWordAtCaretInDocument(document, caretOffset, "{" + word + "}");
    }

    /**
     * @param line
     * @param docCommentType        "@param" or "@returns"
     * @param wrapInvalidDataTypes
     * @return
     */
    private static String addCompoundsToDataType(String line, String docCommentType, boolean wrapInvalidDataTypes) {
        line = line.replaceAll("(?i)(" + docCommentType + "\\s*)(array|boolean|function|null|number|object|string|undefined)", "$1{$2}");

        return wrapInvalidDataTypes
            ? line.replaceAll("(?i)(" + docCommentType + "\\s*)(bool|event|float|int|integer|void)", "$1{$2}")
            : line;
    }

    public static boolean correctInvalidReturnsCommentInDocument(Document document, int caretOffset) {
        return UtilsEnvironment.replaceWordAtCaretInDocument(document, caretOffset, "returns");
    }

    /**
     * Correct invalid JsDoc block comment
     *
     * Correct "@return" into "@returns"
     * Add curly brackets around data shiftableTypes in "@param" and "@returns" lines
     * Correct invalid data shiftableTypes into existing primitive data shiftableTypes (event => Object, int(eger) => number)
     *
     * @param document
     * @param offsetStart
     * @param offsetEnd
     * @return
     */
    public static boolean correctDocBlockInDocument(Editor editor, Document document, int offsetStart, int offsetEnd) {
        String documentText = document.getText();
        String docBlock = documentText.substring(offsetStart, offsetEnd);
        String lines[] = docBlock.split("\n");

        String docBlockCorrected = "";
        int index = 0;
        for (String line : lines) {
            if (isAtParamLine(line)) {
                line = correctAtParamLine(line);
            } else if (isAtReturnsLine(line)) {
                line = correctAtReturnsLine(line);
            }

            docBlockCorrected += (index > 0 ? "\n" : "") + line;
            index++;
        }
        docBlockCorrected = reduceDoubleEmptyCommentLines(docBlockCorrected);

        if (!docBlockCorrected.equals(docBlock)) {
            document.replaceString(offsetStart, offsetEnd, docBlockCorrected);
            UtilsEnvironment.reformatSubString(editor, editor.getProject(), offsetStart, offsetEnd);
            return true;
        }

        return false;
    }

    public static String correctAtParamLine(String line) {
        if (!containsCompounds(line) && containsDataType(line, " ", true)) {
            line = addCompoundsToDataType(line, "@param", true);
        }

        line = correctInvalidDataTypes(line, "{", "}");

        return containsDataType(line, "{", false)
            ? line
            : addDataType(line);
    }

    public static String correctAtReturnsLine(String line) {
        line = correctInvalidAtReturnsStatement(line);

        if (containsDataType(line, " ", true) && !containsCompounds(line)) {
            line = addCompoundsToDataType(line, "@returns", true);
        }
        line = correctInvalidDataTypes(line, "{", "", true);
        line = correctInvalidDataTypes(line, "|", "", true);

        return containsDataType(line, "{", true)
                ? line
                : addDataType(line);
    }

    private static String correctInvalidAtReturnsStatement(String line) {
        return line.replace(" @return ", " @returns ");
    }

    private static String correctInvalidDataTypes(String line, String lhs, String rhs) {
        return correctInvalidDataTypes(line, lhs, rhs, false);
    }
    private static String correctInvalidDataTypes(String line, String lhs, String rhs, boolean allowVoid) {
        if (!allowVoid) {
            line = line.replace(lhs + "void" + rhs,    lhs + "undefined" + rhs);
        }
        return line
                .replace(lhs + "bool" + rhs,    lhs + "boolean" + rhs)
                .replace(lhs + "event" + rhs,   lhs + "Object" + rhs)
                .replace(lhs + "float" + rhs,   lhs + "number" + rhs)
                .replace(lhs + "int" + rhs,     lhs + "number" + rhs)
                .replace(lhs + "integer" + rhs, lhs + "number" + rhs)
                .replace(lhs + "object" + rhs,  lhs + "Object" + rhs);
    }

    private static String reduceDoubleEmptyCommentLines(String block) {
        String lines[] = block.split("\n");
        String blockCleaned = "";

        boolean wasPreviousEmpty = false;
        int index = 0;
        for (String line : lines) {
            boolean isEmpty = index == 0 || (trim(trim(line).replaceAll("\\*", "")).isEmpty());

            if (index == 0 || !(isEmpty && wasPreviousEmpty)) {
                blockCleaned += (index > 0 ? "\n" : "") + line;
            }
            wasPreviousEmpty = isEmpty;
            index++;
        }

        return blockCleaned;
    }

    private static String addDataType(String line) {
        String parameterName = trim(trim(line.replaceAll("\\*", "")).replace("@param", "").replace("@returns", ""));

        return parameterName.isEmpty()
                ? line
                : line.replace(parameterName, guessDataType(parameterName) + " " + parameterName);
    }

    private static String guessDataType(String parameterName) {
        String dataType = null;
        if (parameterName.startsWith("$") || parameterName.matches("(?i)(\\w*element)")) {
            dataType = "*";
        } else if (parameterName.matches("(?i)(\\w*date\\w*)")) {
            dataType = "Date";
        } else if (parameterName.matches("(?i)(\\w*obj\\w*)")) {
            dataType = "Object";
        } else if (parameterName.length() == 1) {
            // e.g. x, y, i, etc.
            dataType = "number";
        }

        if (null == dataType) {
            dataType = UtilsPhp.guessPhpDataTypeByName(parameterName);
        }
        if ("unknown".equals(dataType)) {
            String camelWords[] = UtilsTextual.splitCamelCaseIntoWords(parameterName, true);
            String lastWord = camelWords[camelWords.length - 1];

            if ("func".equals(lastWord) || "function".equals(lastWord) || "callback".equals(lastWord)
            ) {
                dataType = "Object";
            }
        }

        return "{" + correctInvalidDataTypes(dataType, "", "") + "}";
    }
}