package com.drtshock.playervaults.lib.com.typesafe.config.impl;

import com.drtshock.playervaults.lib.com.typesafe.config.ConfigException;
import com.drtshock.playervaults.lib.com.typesafe.config.ConfigParseOptions;
import com.drtshock.playervaults.lib.com.typesafe.config.ConfigRenderOptions;
import com.drtshock.playervaults.lib.com.typesafe.config.ConfigValue;
import com.drtshock.playervaults.lib.com.typesafe.config.parser.ConfigDocument;

import java.io.StringReader;
import java.util.Iterator;

final class SimpleConfigDocument implements ConfigDocument {
    private final ConfigNodeRoot configNodeTree;
    private final ConfigParseOptions parseOptions;

    SimpleConfigDocument(ConfigNodeRoot parsedNode, ConfigParseOptions parseOptions) {
        configNodeTree = parsedNode;
        this.parseOptions = parseOptions;
    }

    @Override
    public ConfigDocument withValueText(String path, String newValue) {
        if (newValue == null)
            throw new ConfigException.BugOrBroken("null value for " + path + " passed to withValueText");
        SimpleConfigOrigin origin = SimpleConfigOrigin.newSimple("single value parsing");
        StringReader reader = new StringReader(newValue);
        Iterator<Token> tokens = Tokenizer.tokenize(origin, reader, parseOptions.getSyntax());
        AbstractConfigNodeValue parsedValue = ConfigDocumentParser.parseValue(tokens, origin, parseOptions);
        reader.close();

        return new SimpleConfigDocument(configNodeTree.setValue(path, parsedValue, parseOptions.getSyntax()), parseOptions);
    }

    @Override
    public ConfigDocument withValue(String path, ConfigValue newValue) {
        if (newValue == null)
            throw new ConfigException.BugOrBroken("null value for " + path + " passed to withValue");
        ConfigRenderOptions options = ConfigRenderOptions.defaults();
        options = options.setOriginComments(false);
        return withValueText(path, newValue.render(options).trim());
    }

    @Override
    public ConfigDocument withoutPath(String path) {
        return new SimpleConfigDocument(configNodeTree.setValue(path, null, parseOptions.getSyntax()), parseOptions);
    }

    @Override
    public boolean hasPath(String path) {
        return configNodeTree.hasValue(path);
    }

    public String render() {
        return configNodeTree.render();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ConfigDocument && render().equals(((ConfigDocument) other).render());
    }

    @Override
    public int hashCode() {
        return render().hashCode();
    }
}
