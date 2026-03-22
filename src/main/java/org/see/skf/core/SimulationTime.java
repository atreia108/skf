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
 * TODO
 *
 * @since 1.5
 */
public final class SimulationTime {
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

    public SimulationTime() {
        simulationScenarioTimeEpoch = -1L;
        simulationScenarioTime = -1L;
        logicalTimeBoundary = new AtomicLong(-1L);
        federateLogicalTime = new AtomicLong(-1L);
        federateLookAheadInterval = new AtomicLong(-1L);

        simulationElapsedTime = new AtomicLong(0L);
    }

    void setupTimeFactory() {
        timeFactory = HLAUtilityFactory.INSTANCE.getTimeFactory();
    }

    void setSimulationScenarioTimeEpoch(long epoch) {
        if (simulationScenarioTimeEpoch == -1L) {
            // TODO
            // 1. How many ticks since HLT 0?
            // 2. Result of (1) + SST_EPOCH
            // 3. Also set SST to SST_EPOCH
        }
    }

    private long ticksSinceEpoch() {

        return 0L;
    }

    void setFederationScenarioTimeEpoch(double value) {
        federationScenarioTimeEpoch = value;
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

    void increment() {
        // Move to the next time step.
        // Increment SET, SST, and of course HLT.
        simulationElapsedTime.set(getSimulationElapsedTime() + 1L);
        simulationScenarioTime += 1.0;
        federateLogicalTime.getAndAdd(federateLookAheadInterval.get());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////

    /*
    private HLAinteger64Interval lookAhead;
    private HLAinteger64Time federationTime;
    private HLAinteger64Time federateTime;

    private long timeCyclesExecuted;

    public HLAinteger64Time nextTimeStep() throws IllegalTimeArithmetic {
        federationTime = federationTime.add(lookAhead);
        return federationTime;
    }

    public HLAinteger64Time getFederationLogicalTime() {
        return federationTime;
    }

    public void setFederationLogicalTime(LogicalTime<?, ?> newTime) {
        federationTime = (HLAinteger64Time) newTime;
    }

    public HLAinteger64Time getFederateLogicalTime() {
        return federateTime;
    }

    public void setFederateLogicalTime(LogicalTime<?, ?> federateTime) {
        this.federateTime = (HLAinteger64Time) federateTime;
    }

    public LogicalTimeInterval<HLAinteger64Interval> getLookAheadAsLogicalTime() {
        return timeFactory.makeInterval(federateLookAheadInterval.get());
    }

    public long getLookAhead() {
        return lookAhead.getValue();
    }

    public void setLookAhead(long lookAheadInterval) {
        HLAinteger64TimeFactory timeFactory = HLAUtilityFactory.INSTANCE.getTimeFactory();
        lookAhead = timeFactory.makeInterval(lookAheadInterval);
    }

    public void setTimeCyclesExecuted(long value) {
        timeCyclesExecuted = value;
    }
     */
}
