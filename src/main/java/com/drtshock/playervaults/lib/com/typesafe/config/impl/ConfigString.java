/**
 * Copyright (C) 2011-2012 Typesafe Inc. <http://typesafe.com>
 */
package com.drtshock.playervaults.lib.com.typesafe.config.impl;

import com.drtshock.playervaults.lib.com.typesafe.config.ConfigOrigin;
import com.drtshock.playervaults.lib.com.typesafe.config.ConfigRenderOptions;
import com.drtshock.playervaults.lib.com.typesafe.config.ConfigValueType;

import java.io.ObjectStreamException;
import java.io.Serializable;

abstract class ConfigString extends AbstractConfigValue implements Serializable {

    private static final long serialVersionUID = 2L;

    final protected String value;

    protected ConfigString(ConfigOrigin origin, String value) {
        super(origin);
        this.value = value;
    }

    boolean wasQuoted() {
        return (this instanceof Quoted);
    }

    @Override
    public ConfigValueType valueType() {
        return ConfigValueType.STRING;
    }

    @Override
    public String unwrapped() {
        return value;
    }

    @Override
    String transformToString() {
        return value;
    }

    @Override
    protected void render(StringBuilder sb, int indent, boolean atRoot, ConfigRenderOptions options) {
        sb.append(ConfigImplUtil.renderJsonString(value));
    }

    final static class Quoted extends ConfigString {
        Quoted(ConfigOrigin origin, String value) {
            super(origin, value);
        }

        @Override
        protected Quoted newCopy(ConfigOrigin origin) {
            return new Quoted(origin, value);
        }

        // serialization all goes through SerializedConfigValue
        private Object writeReplace() throws ObjectStreamException {
            return new SerializedConfigValue(this);
        }
    }

    // this is sort of a hack; we want to preserve whether whitespace
    // was quoted until we process substitutions, so we can ignore
    // unquoted whitespace when concatenating lists or objects.
    // We dump this distinction when serializing and deserializing,
    // but that's OK because it isn't in equals/hashCode, and we
    // don't allow serializing unresolved objects which is where
    // quoted-ness matters. If we later make ConfigOrigin point
    // to the original token range, we could use that to implement
    // wasQuoted()
    final static class Unquoted extends ConfigString {
        Unquoted(ConfigOrigin origin, String value) {
            super(origin, value);
        }

        @Override
        protected Unquoted newCopy(ConfigOrigin origin) {
            return new Unquoted(origin, value);
        }

        // serialization all goes through SerializedConfigValue
        private Object writeReplace() throws ObjectStreamException {
            return new SerializedConfigValue(this);
        }
    }
}
