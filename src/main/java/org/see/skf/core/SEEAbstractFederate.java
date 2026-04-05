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

import hla.rti1516_2025.TimeQueryReturn;
import hla.rti1516_2025.exceptions.*;
import hla.rti1516_2025.time.HLAinteger64Time;
import org.see.skf.conf.FederateConfiguration;
import org.see.skf.exceptions.DeadlineReachedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A complete federate implementation for SEE. It contains the necessary of methods needed for a SpaceFOM federate.
 * Depending on the role of the federate i.e., early or late joiner, these methods must be called in the sequence put
 * forth in the SpaceFOM standard.
 *
 * @since 1.5
 */
public abstract class SEEAbstractFederate extends SKBaseFederate {
    private static final Logger logger = LoggerFactory.getLogger(SEEAbstractFederate.class);
    protected static final long THREAD_WAIT_INTERVAL = 10L;

    private final SEEFederateAmbassador federateAmbassador;
    private final SimulationClock simClock;
    private final ExecutiveState state;
    private final Process process;

    private double simTime;

    protected SEEAbstractFederate(SEEFederateAmbassador federateAmbassador, FederateConfiguration config) {
        super(federateAmbassador, config);
        this.federateAmbassador = federateAmbassador;

        // At this point, it should be safe for us to set up the simulation time.
        simClock = federateAmbassador.getSimClock();
        long lookAhead = config.lookAhead();
        simClock.setLookAheadInterval(lookAhead);

        process = new Process();
        state = new ExecutiveState();
    }

    /**
     * Begin executing the simulation.
     */
    public final void startExecution() {
        state.gotoRun(false);
    }

    /**
     * Enter the SpaceFOM freeze executive control state and pause all simulation activity indefinitely.
     */
    public final void freezeExecution() {
        state.gotoFreeze();
    }

    /**
     * Leave the SpaceFOM freeze executive control state and resume simulation activity.
     */
    public final void resumeExecution() {
        state.gotoRun(true);
    }

    /**
     * Disconnect from the federation execution and terminate the simulation.
     */
    public final void shutdownExecution() {
        state.gotoShutdown();
    }

    public final void enforceTimeConstraint() throws RTIexception{
        rtiAmbassador.enableTimeConstrained();
        while (!federateAmbassador.isConstrained()) {
            try {
                Thread.sleep(THREAD_WAIT_INTERVAL);
            } catch (InterruptedException e) {
                logger.warn("Program thread was interrupted while waiting for the RTI to time constrain this federate.");
                Thread.currentThread().interrupt();
            }
        }
        logger.debug("The federate has been time constrained successfully. TSO messages can now be received.");
    }

    public final void enforceTimeRegulation() throws RTIexception {
        rtiAmbassador.enableTimeRegulation(simClock.getLookAheadInterval());
        while (!federateAmbassador.isRegulating()) {
            try {
                Thread.sleep(THREAD_WAIT_INTERVAL);
            } catch (InterruptedException e) {
                logger.warn("Program thread was interrupted while waiting for the RTI to enable time regulation for this federate.");
                Thread.currentThread().interrupt();
            }
        }
        logger.debug("The federate has been time regulated successfully. TSO messages can now be sent.");
    }

    public final void enforceAsynchronousDelivery() throws FederateNotExecutionMember, RestoreInProgress, AsynchronousDeliveryAlreadyEnabled, NotConnected, RTIinternalError, SaveInProgress {
        // In line with the SpaceFOM standard, asynchronous delivery is disallowed for late joiners.
        // Early and late joiners require it for multiphase initialization.
        rtiAmbassador.enableAsynchronousDelivery();
        logger.debug("Asynchronous delivery of messages has been enabled for this federate.");
    }

    /**
     * Advances the federate from its current time step to the HLA logical time provided.
     * @param timeStep The HLA logical time that the federate should advance to.
     */
    final void advanceTime(HLAinteger64Time timeStep) throws InTimeAdvancingState, FederateNotExecutionMember, RestoreInProgress, RequestForTimeConstrainedPending, NotConnected, LogicalTimeAlreadyPassed, InvalidLogicalTime, RTIinternalError, SaveInProgress, RequestForTimeRegulationPending {
        rtiAmbassador.timeAdvanceRequest(timeStep);
        federateAmbassador.setAdvancing(true);
    }

    /**
     * Activities performed by the simulation should be done here. It is called once per tick.
     */
    public abstract void update();

    /**
     * Registers a federation-wide synchronization point that must be achieved by all federates.
     *
     * @param syncPoint A supported synchronization point in the SpaceFOM standard.
     */
    public final void registerSyncPoint(SyncPoint syncPoint) throws FederateNotExecutionMember, RestoreInProgress, NotConnected, RTIinternalError, SaveInProgress {
        String syncPointLabel = syncPoint.getLabel();
        rtiAmbassador.registerFederationSynchronizationPoint(syncPointLabel, null);
    }

    /**
     * Signals that the federate has achieved a previously-announced synchronization point.
     *
     * @param syncPoint A supported synchronization point in the SpaceFOM standard.
     * @param flag true or false depending on if the synchronization point was achieved or not.
     */
    public final void achieveSyncPoint(SyncPoint syncPoint, boolean flag) throws SynchronizationPointLabelNotAnnounced, FederateNotExecutionMember, RestoreInProgress, NotConnected, RTIinternalError, SaveInProgress {
        String syncPointLabel = syncPoint.getLabel();
        rtiAmbassador.synchronizationPointAchieved(syncPointLabel, flag);
    }

    /**
     * Waits for a synchronization point to be announced within the supplied time period.
     * @param syncPoint The SpaceFOM sync point.
     * @param maxWaitingTime The anticipated time period to wait for the sync point to be announced.
     * @return true or false depending on whether the synchronization point announcement was successful or not.
     */
    public final boolean awaitSyncPointAnnouncement(SyncPoint syncPoint, int maxWaitingTime) {
        long deadline = System.currentTimeMillis() + maxWaitingTime;

        while (!syncPoint.isAnnounced()) {
            try {
                Thread.sleep(THREAD_WAIT_INTERVAL);
                if (System.currentTimeMillis() >= deadline) {
                    throw new DeadlineReachedException("The synchronization point <" + syncPoint.getLabel() + "> was not announced within the specified waiting period.");
                }
            } catch (InterruptedException e) {
                logger.error("Program thread was interrupted while waiting for the synchronization point <{}> to be announced.", syncPoint.getLabel());
                Thread.currentThread().interrupt();
            } catch (DeadlineReachedException e) {
                return false;
            }
        }

        return true;
    }

    /**
     * Waits for a synchronization point to be achieved by the federation within the supplied time period.
     * @param syncPoint The SpaceFOM sync point.
     * @param maxWaitingTime The anticipated time period to wait for the sync point to be achieved.
     * @return true or false depending on whether the federation was successfully synchronized or not.
     */
    public final boolean awaitFederationSynchronization(SyncPoint syncPoint, int maxWaitingTime) {
        long deadline = System.currentTimeMillis() + maxWaitingTime;

        while (!syncPoint.isFederationSynchronized()) {
            try {
                Thread.sleep(THREAD_WAIT_INTERVAL);
                if (System.currentTimeMillis() >= deadline) {
                    throw new DeadlineReachedException("Failed to achieve the synchronization point <" + syncPoint.getLabel() + "> within the specified waiting period.");
                }
            } catch (InterruptedException e) {
                logger.error("Program thread was interrupted while waiting for the federation to achieve the synchronization point <{}>.", syncPoint.getLabel());
                Thread.currentThread().interrupt();
            } catch (DeadlineReachedException e) {
                return false;
            }
        }

        return true;
    }

    SimulationClock getSimClock() {
        return simClock;
    }

    public double getSimTime() {
        return simTime;
    }

    private final class Process implements Runnable {
        // private static final long MICROSECONDS_PER_CYCLE = 1000000L;

        private final AtomicBoolean running;
        private final AtomicBoolean suspended;

        // private long executionCounter;

        public Process() {
            running = new AtomicBoolean(true);
            suspended = new AtomicBoolean(false);
        }

        @Override
        public void run() {
            while (isRunning()) {
                // simulationTime.setTimeCyclesExecuted(executionCounter * MICROSECONDS_PER_CYCLE);

                waitForTimeAdvanceGrant();
                update();

                synchronized (this) {
                    while (isSuspended()) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            logger.warn("Program thread was interrupted in suspended state while waiting for the federation execution to resume activities.");
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                try {
                    // There is a possibility that we have instructions from the RTI to shut down on an event listener
                    // thread, in which case attempting to advance time could be catastrophic.
                    // This check permits a graceful exit.
                    if (isRunning()) {
                        // advanceTime(simulationTime.nextTimeStep());
                        simClock.increment();
                        HLAinteger64Time nextLogicalTime = simClock.getLogicalTime();
                        advanceTime(nextLogicalTime);

                        simTime = simClock.getSimulationScenarioTime();
                    }
                } catch (RTIexception e) {
                    throw new IllegalStateException("The federate encountered an unexpected error while trying to advance to the next time step.", e);
                }

                // ++executionCounter;
            }

            try {
                resignFederationExecution();
            } catch (Exception e) {
                throw new IllegalStateException("Unexpected error encountered when attempting to resign and disconnect from the federation execution.", e);
            }
        }

        void waitForTimeAdvanceGrant() {
            while (federateAmbassador.isAdvancing() && isRunning()) {
                try {
                    Thread.sleep(THREAD_WAIT_INTERVAL);
                } catch (InterruptedException e) {
                    logger.warn("Program thread was interrupted while waiting for time advance grant from the RTI.");
                    Thread.currentThread().interrupt();
                }
            }
        }

        void suspend() {
            suspended.set(true);
        }

        synchronized void resume() {
            suspended.set(false);
            notifyAll();
        }

        void shutdown() {
            running.set(false);
        }

        boolean isRunning() {
            return running.get();
        }

        boolean isSuspended() {
            return suspended.get();
        }
    }

    private final class ExecutiveState {
        void gotoRun(boolean resumeFlag) {
            new Thread(() -> {
                if (resumeFlag) {
                    process.resume();
                } else {
                    process.run();
                }
            }).start();
        }

        void gotoFreeze() {
            // TODO - Implement federate freeze sequence.
        }

        void gotoShutdown() {
            process.shutdown();
        }
    }
}
