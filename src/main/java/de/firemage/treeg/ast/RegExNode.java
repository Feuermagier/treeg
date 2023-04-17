package de.firemage.treeg.ast;

import de.firemage.treeg.TreePrinter;

public sealed interface RegExNode permits
        RegExCharacter,
        Alternative,
        BoundaryMatcher,
        CaptureGroupReference,
        Chain,
        CharacterClass,
        Group,
        Lookaround,
        PredefinedCharacterClass,
        Quantifier {

    String toRegEx();

    void toTree(TreePrinter printer);
}
