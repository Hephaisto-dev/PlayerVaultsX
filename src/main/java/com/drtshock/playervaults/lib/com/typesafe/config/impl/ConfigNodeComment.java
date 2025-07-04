package com.drtshock.playervaults.lib.com.typesafe.config.impl;


import com.drtshock.playervaults.lib.com.typesafe.config.ConfigException;

final class ConfigNodeComment extends ConfigNodeSingleToken {
    ConfigNodeComment(Token comment) {
        super(comment);
        if (!Tokens.isComment(super.token)) {
            throw new ConfigException.BugOrBroken("Tried to create a ConfigNodeComment from a non-comment token");
        }
    }

    String commentText() {
        return Tokens.getCommentText(super.token);
    }
}
