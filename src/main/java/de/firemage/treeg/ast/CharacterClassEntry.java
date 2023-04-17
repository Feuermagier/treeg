package de.firemage.treeg.ast;

import de.firemage.treeg.TreePrinter;

public sealed interface CharacterClassEntry permits RegExCharacter, CharacterRange {
    String toRegEx();
    void toTree(TreePrinter printer);
}
