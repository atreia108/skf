/*****************************************************************
 SEE HLA Starter Kit Framework -  A Java library that supports
 the development of HLA Federates in the Simulation Exploration
 Experience (SEE) program.

 Copyright (c) 2014, 2026 SMASH Lab - University of Calabria
 (Italy), Hridyanshu Aatreya - Modelling & Simulation Group (MSG)
 at Brunel University of London. All rights reserved.

 GNU Lesser General Public License (GNU LGPL).

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3.0 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public
 License along with this library.
 If not, see http://http://www.gnu.org/licenses/
 *****************************************************************/

package org.see.skf.core;

import hla.rti1516_2025.time.*;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Global clock for the simulation that maintains a coordinated progression of time
 * across the physical and HLA timelines.
 *
 * @since 2.1
 */
public final class SimulationClock {
    // Given that the SST values are highly unlikely to be used outside of this class AND the absence of a built-in
    // AtomicDouble type in the JDK, primitive double types are used instead.
    private double federationScenarioTimeEpoch;
    private double simulationScenarioTimeEpoch;
    private double simulationScenarioTime;
    private final AtomicLong simulationElapsedTime;
    private final AtomicLong federateLogicalTime;
    private final AtomicLong federateLookAheadInterval;
    private final AtomicLong logicalTimeBoundary;

    private HLAinteger64TimeFactory timeFactory;

    public SimulationClock() {
        simulationScenarioTimeEpoch = -1L;
        simulationScenarioTime = -1L;
        logicalTimeBoundary = new AtomicLong(-1L);
        federateLogicalTime = new AtomicLong(-1L);
        federateLookAheadInterval = new AtomicLong(-1L);
        simulationElapsedTime = new AtomicLong(0L);
    }

    void increment() {
        // Move to the next time step.
        // Increment SET, SST, and of course HLT.
        simulationElapsedTime.set(getSimulationElapsedTime() + 1L);
        simulationScenarioTime += 1.0;
        federateLogicalTime.getAndAdd(federateLookAheadInterval.get());
    }

    void setupTimeFactory() {
        timeFactory = HLAUtilityFactory.INSTANCE.getTimeFactory();
    }

    void setSimulationScenarioTimeEpoch(long logicalTime) {
        if (simulationScenarioTimeEpoch == -1L) {
            // 1. How many ticks since HLT 0?
            // 2. Result of (1) + FST_EPOCH
            // 3. Finally, set SST = SST_EPOCH
            simulationScenarioTimeEpoch = ticksSinceEpoch(logicalTime) + federationScenarioTimeEpoch;
            simulationScenarioTime = simulationScenarioTimeEpoch;
        }
    }

    private long ticksSinceEpoch(long logicalTime) {
        if ((logicalTime % 1000000) == 0) {
            return logicalTime / 1000000;
        } else {
            return logicalTime;
        }
    }

    void setFederationScenarioTimeEpoch(double value) {
        federationScenarioTimeEpoch = value;
    }

    double getSimulationScenarioTime() {
        return simulationScenarioTime;
    }

    HLAinteger64Interval getLookAheadInterval() {
        return timeFactory.makeInterval(federateLookAheadInterval.get());
    }

    void setLookAheadInterval(long interval) {
        federateLookAheadInterval.set(interval);
    }

    void setLogicalTimeBoundary(long value) {
        logicalTimeBoundary.set(value);
    }

    HLAinteger64Time getLogicalTimeBoundary() {
        return timeFactory.makeTime(logicalTimeBoundary.get());
    }

    HLAinteger64Time getLogicalTime() {
        return timeFactory.makeTime(federateLogicalTime.get());
    }

    void setLogicalTime(HLAinteger64Time logicalTime) {
        federateLogicalTime.set(logicalTime.getValue());
    }

    long getSimulationElapsedTime() {
        return simulationElapsedTime.get();
    }
}
