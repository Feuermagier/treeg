package de.firemage.treeg;

import de.firemage.treeg.ast.Alternative;
import de.firemage.treeg.ast.BoundaryMatcher;
import de.firemage.treeg.ast.CaptureGroupReference;
import de.firemage.treeg.ast.Chain;
import de.firemage.treeg.ast.CharacterClass;
import de.firemage.treeg.ast.CharacterClassEntry;
import de.firemage.treeg.ast.CharacterRange;
import de.firemage.treeg.ast.Group;
import de.firemage.treeg.ast.Lookaround;
import de.firemage.treeg.ast.PredefinedCharacterClass;
import de.firemage.treeg.ast.Quantifier;
import de.firemage.treeg.ast.RegExCharacter;
import de.firemage.treeg.ast.RegExNode;

public class Score {

    public static double scoreRegEx(RegularExpression regex) {
        return scoreNode(regex.root());
    }

    private static double scoreNode(RegExNode node) {
        return switch (node) {
            case RegExCharacter c -> scoreCharacter(c);
            case Alternative a -> scoreAlternative(a);
            case BoundaryMatcher b -> scoreBoundaryMatcher(b);
            case CaptureGroupReference c -> scoreCaptureGroupReference(c);
            case Chain c -> scoreChain(c);
            case CharacterClass c -> scoreCharacterClass(c);
            case Group g -> scoreGroup(g);
            case Lookaround l -> scoreLookaround(l);
            case PredefinedCharacterClass p -> scorePredefinedCharacterClass(p);
            case Quantifier q -> scoreQuantifier(q);
        };
    }

    private static double scoreCharacter(RegExCharacter character) {
        if (character.escaped()) {
            return 0.5;
        } else {
            return 0.1;
        }
    }

    private static double scoreAlternative(Alternative alternative) {
        return Math.exp(alternative.alternatives().size() / 5.0) * alternative.alternatives().stream().mapToDouble(Score::scoreNode).sum();
    }

    private static double scoreBoundaryMatcher(BoundaryMatcher matcher) {
        return 1.0;
    }

    private static double scoreCaptureGroupReference(CaptureGroupReference ref) {
        return 5.0;
    }

    private static double scoreChain(Chain chain) {
        return chain.children().stream().mapToDouble(Score::scoreNode).sum();
    }

    private static double scoreCharacterClass(CharacterClass c) {
        return (c.negated() ? 2.0 : 1.0) * c.ranges().stream().mapToDouble(Score::scoreCharacterClassEntry).sum();
    }

    private static double scoreCharacterClassEntry(CharacterClassEntry entry) {
        return switch (entry) {
            case RegExCharacter c -> scoreCharacter(c);
            case CharacterRange r -> 3.0;
        };
    }

    private static double scoreGroup(Group group) {
        double multiplier = switch (group.type()) {
            case CAPTURING -> 1.0;
            case NON_CAPTURING -> 2.0;
            case INDEPENDENT_NON_CAPTURING -> 5.0;
        };

        if (group.name() != null) {
            multiplier += 2.0;
        }

        if (group.flags() != null) {
            multiplier += Math.exp(group.flags().length());
        }

        return multiplier * scoreNode(group.root());
    }

    private static double scoreLookaround(Lookaround lookaround) {
        return 10.0 * scoreNode(lookaround.child());
    }

    private static double scorePredefinedCharacterClass(PredefinedCharacterClass c) {
        return switch (c.type()) {
            case ANY, DIGIT, WORD -> 0.5;
            case NON_DIGIT, WHITESPACE, NON_WORD -> 2.0;
            case HORIZONTAL_WHITESPACE, NON_HORIZONTAL_WHITESPACE, NON_WHITESPACE, VERTICAL_WHITESPACE, NON_VERTICAL_WHITESPACE -> 5.0;
        };
    }

    private static double scoreQuantifier(Quantifier quantifier) {
        return switch (quantifier.type()) {
            case AT_MOST_ONCE, ANY, AT_LEAST_ONCE -> 1.5;
            case TIMES, OPEN_RANGE, RANGE -> 2.0;
        } * scoreNode(quantifier.child());
    }
}
