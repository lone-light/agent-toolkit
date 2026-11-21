// 检查：Indentation, LeftCurly, RightCurly, WhitespaceAfter, WhitespaceAround, WhitespaceBeforeEmptyBody
// 检查：GenericWhitespace, SingleSpaceSeparator, MethodParamPad, ParenPad, TypecastParenPad, EmptyForIteratorPad
// 检查：NoWhitespaceBefore, NoWhitespaceBeforeCaseDefaultColon, EmptyLineSeparator
// 检查：OneStatementPerLine, MultipleVariableDeclarations, ArrayTypeStyle, UpperEll
// 检查：ModifierOrder, RedundantModifier, DeclarationOrder, VisibilityModifier, VariableDeclarationUsageDistance
package base;

import java.util.List;

class LayoutCases
{
  static public final long LIMIT = 9l;
    private List< String > names;
    public String values[];
    private int  total = 1;

    void reset(){}
    void combine ( int first,int second ) {
        total=first + second;
        if (total > 0) {
            reset() ;
        }
        else {
            reset();
        }
    }

    int count;

    void checkExpressions(Object value) {
        String text = ( String ) value;
        int first = 1, second = 2;
        int third = first; int fourth = second;
        for (int index = 0; index < text.length();) {
            break;
        }
        switch (fourth) {
            case 2 : break;
            default: break;
        }
    }

    int delayedUse() {
        int start = 1;
        reset();
        reset();
        reset();
        reset();
        reset();
        reset();
        reset();
        reset();
        reset();
        reset();
        reset();
        reset();
        reset();
        reset();
        return start;
    }

    interface Resettable {
        public void reset();
    }
}
