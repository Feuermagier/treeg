package de.firemage.treeg;

public record CharacterRange(char start, char end) implements CharacterClassEntry {
    @Override
    public String toRegEx() {
        return this.start + "-" + this.end;
    }

    @Override
    public void toTree(TreePrinter printer) {
        printer.addLine("Range ('" + this.start + "' to '" + end + "')");
    }
}