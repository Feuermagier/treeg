package de.firemage.treeg;

public record Character(char content, boolean escaped) implements RegExNode, CharacterClassEntry {
    @Override
    public String toRegEx() {
        return (this.escaped ? "\\" : "") + this.content;
    }

    @Override
    public void toTree(TreePrinter printer) {
        printer.addLine(this.content + (this.escaped ? " (escaped)" : ""));
    }
}
