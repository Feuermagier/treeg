package de.firemage.treeg;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RegExParser {
    public static RegularExpression parse(String regex) throws InvalidRegExSyntaxException {
        RegExLexer lexer = new RegExLexer(regex);
        RegExNode root = parseAlternatives(lexer);
        lexer.expect(RegExElementType.EOF);
        return new RegularExpression(root);
    }

    private static RegExNode parseAlternatives(RegExLexer lexer) throws InvalidRegExSyntaxException {
        List<RegExNode> alternatives = new ArrayList<>(1);
        while (true) {
            alternatives.add(parseChain(lexer));

            if (lexer.peek() == '|') {
                lexer.consumeNext();
            } else {
                break;
            }
        }

        if (alternatives.size() > 1) {
            return new Alternative(alternatives);
        } else {
            return alternatives.get(0);
        }
    }

    private static RegExNode parseChain(RegExLexer lexer) throws InvalidRegExSyntaxException {
        List<RegExNode> children = new ArrayList<>();
        outer:
        while (true) {
            switch (lexer.peekType()) {
                case HAT -> {
                    lexer.consumeNext();
                    children.add(new BoundaryMatcher(BoundaryMatcher.Type.LINE_START));
                }
                case DOLLAR -> {
                    lexer.consumeNext();
                    children.add(new BoundaryMatcher(BoundaryMatcher.Type.LINE_END));
                }
                case EOF, GROUP_END, CHARACTER_CLASS_END, OR -> {
                    break outer;
                }
                default -> children.add(parseMaybeQuantified(lexer));
            }
        }

        if (children.size() > 1) {
            return new Chain(children);
        } else {
            return children.get(0);
        }
    }

    private static Character parseCharacter(RegExLexer lexer) throws InvalidRegExSyntaxException {
        return new Character(lexer.consumeNext(), false);
    }

    private static RegExNode parseEscaped(RegExLexer lexer) throws InvalidRegExSyntaxException {
        lexer.expect(RegExElementType.ESCAPE);
        char value = lexer.consumeNext();
        return switch (value) {
            case 'd' -> new PredefinedCharacterClass(PredefinedCharacterClass.Type.DIGIT);
            case 'D' -> new PredefinedCharacterClass(PredefinedCharacterClass.Type.NON_DIGIT);
            case 'h' -> new PredefinedCharacterClass(PredefinedCharacterClass.Type.HORIZONTAL_WHITESPACE);
            case 'H' -> new PredefinedCharacterClass(PredefinedCharacterClass.Type.NON_HORIZONTAL_WHITESPACE);
            case 's' -> new PredefinedCharacterClass(PredefinedCharacterClass.Type.WHITESPACE);
            case 'S' -> new PredefinedCharacterClass(PredefinedCharacterClass.Type.NON_WHITESPACE);
            case 'v' -> new PredefinedCharacterClass(PredefinedCharacterClass.Type.VERTICAL_WHITESPACE);
            case 'V' -> new PredefinedCharacterClass(PredefinedCharacterClass.Type.NON_VERTICAL_WHITESPACE);
            case 'w' -> new PredefinedCharacterClass(PredefinedCharacterClass.Type.WORD);
            case 'W' -> new PredefinedCharacterClass(PredefinedCharacterClass.Type.NON_WORD);
            case 'b' -> new BoundaryMatcher(BoundaryMatcher.Type.WORD_BOUNDARY);
            case 'B' -> new BoundaryMatcher(BoundaryMatcher.Type.NON_WORD_BOUNDARY);
            case 'A' -> new BoundaryMatcher(BoundaryMatcher.Type.INPUT_START);
            case 'G' -> new BoundaryMatcher(BoundaryMatcher.Type.MATCH_END);
            case 'Z' -> new BoundaryMatcher(BoundaryMatcher.Type.INPUT_END_BEFORE_TERMINATOR);
            case 'z' -> new BoundaryMatcher(BoundaryMatcher.Type.INPUT_END);
            case 'R' -> new BoundaryMatcher(BoundaryMatcher.Type.LINEBREAK);
            case '(', ')', '[', ']', '{', '}', '.', '/', '\\' -> new Character(value, true);
            default -> throw new InvalidRegExSyntaxException("Unknown escape sequence '\\" + value + "'");
        };
    }

    private static Group parseGroup(RegExLexer lexer) throws InvalidRegExSyntaxException {
        lexer.expect(RegExElementType.GROUP_START);
        RegExNode child = parseAlternatives(lexer);
        lexer.expect(RegExElementType.GROUP_END);
        return new Group(child);
    }

    private static RegExNode parseMaybeQuantified(RegExLexer lexer) throws InvalidRegExSyntaxException {
        RegExNode child = switch (lexer.peekType()) {
            case CHARACTER, NUMBER, RANGE -> parseCharacter(lexer);
            case ESCAPE -> parseEscaped(lexer);
            case GROUP_START -> parseGroup(lexer);
            case CHARACTER_CLASS_START -> parseCharacterClass(lexer);
            case DOT -> {
                lexer.consumeNext();
                yield new PredefinedCharacterClass(PredefinedCharacterClass.Type.ANY);
            }
            case HAT -> {
                lexer.consumeNext();
                yield new BoundaryMatcher(BoundaryMatcher.Type.LINE_START);
            }
            default -> throw new InvalidRegExSyntaxException("Unexpected character '" + lexer.peek() + "'");
        };

        Quantifier quantifier = switch (lexer.peek()) {
            case '?' -> {
                lexer.consumeNext();
                yield new Quantifier(child, Quantifier.Type.AT_MOST_ONCE, 0, 1);
            }
            case '*' -> {
                lexer.consumeNext();
                yield new Quantifier(child, Quantifier.Type.ANY, 0, -1);
            }
            case '+' -> {
                lexer.consumeNext();
                yield new Quantifier(child, Quantifier.Type.AT_LEAST_ONCE, 1, -1);
            }
            case '{' -> {
                lexer.mark();
                lexer.consumeNext();
                if (lexer.peekType() == RegExElementType.NUMBER) {
                    int min = parseNumber(lexer);
                    if (lexer.peek() == '}') {
                        lexer.consumeNext();
                        yield new Quantifier(child, Quantifier.Type.TIMES, min, min);
                    } else if (lexer.peek() == ',') {
                        lexer.consumeNext();
                        if (lexer.peek() == '}') {
                            lexer.consumeNext();
                            yield new Quantifier(child, Quantifier.Type.OPEN_RANGE, min, -1);
                        } else if (lexer.peekType() == RegExElementType.NUMBER) {
                            int max = parseNumber(lexer);
                            if (lexer.peek() == '}') {
                                lexer.consumeNext();
                                yield new Quantifier(child, Quantifier.Type.RANGE, min, max);
                            } else {
                                lexer.backtrack();
                                yield null;
                            }
                        } else {
                            lexer.backtrack();
                            yield null;
                        }
                    } else {
                        lexer.backtrack();
                        yield null;
                    }
                } else {
                    lexer.backtrack();
                    yield null;
                }
            }
            default -> null;
        };

        if (quantifier != null) {
            return quantifier;
        } else {
            return child;
        }
    }

    private static CharacterClass parseCharacterClass(RegExLexer lexer) throws InvalidRegExSyntaxException {
        lexer.expect(RegExElementType.CHARACTER_CLASS_START);

        boolean negated;
        if (lexer.peekType() == RegExElementType.HAT) {
            lexer.consumeNext();
            negated = true;
        } else {
            negated = false;
        }

        List<CharacterClassEntry> entries = new ArrayList<>();
        while (lexer.peekType() != RegExElementType.CHARACTER_CLASS_END) {
            if (lexer.hasNext(3) && lexer.peekType(1) == RegExElementType.RANGE && lexer.peekType(2) != RegExElementType.CHARACTER_CLASS_END) {
                // Range
                char start = lexer.consumeNext();
                lexer.expect(RegExElementType.RANGE);
                char end = lexer.consumeNext();
                entries.add(new CharacterRange(start, end));
            } else if (lexer.peekType() == RegExElementType.ESCAPE) {
                lexer.consumeNext();
                entries.add(new Character(lexer.consumeNext(), true));
            } else {
                entries.add(new Character(lexer.consumeNext(), false));
            }
        }

        lexer.expect(RegExElementType.CHARACTER_CLASS_END);
        return new CharacterClass(negated, entries);
    }

    private static int parseNumber(RegExLexer lexer) throws InvalidRegExSyntaxException {
        StringBuilder content = new StringBuilder();
        while (lexer.peekType() == RegExElementType.NUMBER) {
            content.append(lexer.consumeNext());
        }
        return Integer.parseInt(content.toString());
    }

    public static void main(String[] args) throws InvalidRegExSyntaxException {
        String string = "[\\w -$]+";
        RegularExpression regex = RegExParser.parse(string);
        System.out.println(regex.toTree());
        System.out.println("O: " + string);
        System.out.println("R: " + regex.toRegEx());
    }
}
