package com.drtshock.playervaults.lib.com.typesafe.config.impl;

final class SubstitutionExpression {

    final private Path path;
    final private boolean optional;

    SubstitutionExpression(Path path, boolean optional) {
        this.path = path;
        this.optional = optional;
    }

    Path path() {
        return path;
    }

    boolean optional() {
        return optional;
    }

    SubstitutionExpression changePath(Path newPath) {
        if (newPath == path)
            return this;
        else
            return new SubstitutionExpression(newPath, optional);
    }

    @Override
    public String toString() {
        return "${" + (optional ? "?" : "") + path.render() + "}";
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof SubstitutionExpression otherExp) {
            return otherExp.path.equals(this.path) && otherExp.optional == this.optional;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int h = 41 * (41 + path.hashCode());
        h = 41 * (h + (optional ? 1 : 0));
        return h;
    }
}
