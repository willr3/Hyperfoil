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
package io.sailrocket.core.impl.statistics;

import io.sailrocket.api.Statistics;
import org.HdrHistogram.Histogram;

import java.util.function.Consumer;

public class PrintStatisticsConsumer implements Consumer<Statistics> {
    @Override
    public void accept(Statistics statistics) {
        Histogram histogramCopy = statistics.histogram.copy();
        System.out.format("%s : total requests/responses %d, max %.2f, min %.2f, mean %.2f, 90th centile: %.2f%n",
                statistics.histogram.toString(),
                statistics.requestCount,
                histogramCopy.getMaxValue() / 1_000_000.0,
                histogramCopy.getMinValue() / 1_000_000.0,
                histogramCopy.getMean() / 1_000_000.0,
                histogramCopy.getValueAtPercentile(99.0) / 1_000_000.0

        );

    }
}
