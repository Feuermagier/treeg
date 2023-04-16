package de.firemage.treeg;

public record Group(RegExNode root) implements RegExNode {
    @Override
    public String toRegEx() {
        return "(" + this.root.toRegEx() + ")";
    }

    @Override
    public void toTree(TreePrinter printer) {
        printer.addLine("Group");
        printer.indent();
        this.root.toTree(printer);
        printer.unindent();
    }
}
