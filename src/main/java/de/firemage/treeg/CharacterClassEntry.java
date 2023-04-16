package de.firemage.treeg;

public interface CharacterClassEntry {
    String toRegEx();
    void toTree(TreePrinter printer);
}
