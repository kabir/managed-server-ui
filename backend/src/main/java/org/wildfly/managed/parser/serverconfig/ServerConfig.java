/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.managed.parser.serverconfig;

import org.wildfly.managed.parser.Node;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class ServerConfig implements Node {
    private final Layers layers;

    public ServerConfig(Layers layers) {
        this.layers = layers;
    }

    public Layers getLayers() {
        return layers;
    }

    @Override
    public void marshall(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(ServerConfigParser.ROOT_ELEMENT_NAME);
        layers.marshall(writer);
        writer.writeEndElement();
    }

    @Override
    public boolean hasContent() {
        return !layers.getLayers().isEmpty();
    }
}
