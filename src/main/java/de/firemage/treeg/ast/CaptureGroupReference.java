package de.firemage.treeg.ast;

import de.firemage.treeg.TreePrinter;

public record CaptureGroupReference(int index) implements RegExNode {
    @Override
    public String toRegEx() {
        return "\\" + this.index;
    }

    @Override
    public void toTree(TreePrinter printer) {
        printer.addLine("Capture Group Ref (n = " + this.index + ")");
    }
}
