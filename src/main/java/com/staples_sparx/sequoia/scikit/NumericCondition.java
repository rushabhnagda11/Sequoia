/*
 * Copyright 2014 Staples Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.staples_sparx.sequoia.scikit;

import com.staples_sparx.sequoia.Condition;

public class NumericCondition implements Condition<Integer, double[]> {

    private final double cutPoint;

    public NumericCondition(double cutPoint) {
        this.cutPoint = cutPoint;
    }

    @Override
    public int nextOffsetIndex(Integer feature, double[] features) {
        double value = features[feature];
        if (value <= cutPoint) {
            return 0;
        } else {
            return 1;
        }
    }
}
