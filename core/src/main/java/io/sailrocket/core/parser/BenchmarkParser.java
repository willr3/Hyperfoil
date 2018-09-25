/*
 * Copyright 2018 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package io.sailrocket.core.parser;

import io.sailrocket.api.config.Benchmark;
import io.sailrocket.core.builders.BenchmarkBuilder;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.DocumentEndEvent;
import org.yaml.snakeyaml.events.DocumentStartEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.SequenceEndEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;
import org.yaml.snakeyaml.events.StreamEndEvent;
import org.yaml.snakeyaml.events.StreamStartEvent;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;

public class BenchmarkParser extends AbstractParser<BenchmarkBuilder, BenchmarkBuilder> {
    private static final BenchmarkParser INSTANCE = new BenchmarkParser();
    private static final boolean DEBUG_PARSER = Boolean.getBoolean("io.sailrocket.parser.debug");

    public static BenchmarkParser instance() {
        return INSTANCE;
    }

    private BenchmarkParser() {
        subBuilders.put("name", new PropertyParser.String<>(BenchmarkBuilder::name));
        subBuilders.put("hosts", new AgentsParser());
        subBuilders.put("simulation", new SimulationParser());
    }

    public Benchmark buildBenchmark(InputStream configurationStream) throws ConfigurationNotDefinedException, ConfigurationParserException {

        if (configurationStream == null)
            throw new ConfigurationNotDefinedException();

        Yaml yaml = new Yaml();

        Iterator<Event> events = yaml.parse(new InputStreamReader(configurationStream)).iterator();
        if (DEBUG_PARSER) {
            events = new DebugIterator<>(events);
        }
        Context ctx = new Context(events);

        ctx.expectEvent(StreamStartEvent.class);
        ctx.expectEvent(DocumentStartEvent.class);

        //instantiate new benchmark builder
        BenchmarkBuilder benchmarkBuilder = BenchmarkBuilder.builder();
        parse(ctx, benchmarkBuilder);

        ctx.expectEvent(DocumentEndEvent.class);
        ctx.expectEvent(StreamEndEvent.class);

        return benchmarkBuilder.build();
    }

    @Override
    public void parse(Context ctx, BenchmarkBuilder target) throws ConfigurationParserException {
        ctx.expectEvent(MappingStartEvent.class);
        //populate benchmark model
        callSubBuilders(ctx, target, MappingEndEvent.class);
    }

    private static class DebugIterator<T> implements Iterator<T> {
        private final Iterator<T> it;
        private String indent = "";

        private DebugIterator(Iterator<T> it) {
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public T next() {
            T event = it.next();
            if (event instanceof MappingEndEvent || event instanceof SequenceEndEvent) {
                indent = indent.substring(2);
            }
            StackTraceElement[] stackTrace = new Exception().fillInStackTrace().getStackTrace();
            System.out.println(indent + event + " fetched from " + stackTrace[1] + "\t" + stackTrace[2] + "\t" + stackTrace[3]);
            if (event instanceof MappingStartEvent || event instanceof SequenceStartEvent) {
                indent += "| ";
            }
            return event;
        }
    }
}
