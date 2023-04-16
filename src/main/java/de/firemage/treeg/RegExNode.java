package de.firemage.treeg;

public interface RegExNode {
    String toRegEx();
    void toTree(TreePrinter printer);
}
