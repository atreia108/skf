package org.see.skf.core;

import hla.rti1516_2025.exceptions.*;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.see.skf.conf.FederateConfiguration;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LateJoinerFederateTest {
    private static File confFile = new File("src/test/resources/test_federate.conf");

    @Disabled
    @Test
    void testLaunch() {
        assertNotNull(confFile);

        FederateConfiguration config = FederateConfiguration.Factory.parse(confFile);
        SEELateJoinerFederate federate = new TestFederate(new SEEFederateAmbassador(), config);
        federate.configureAndStart();
    }

    // .. More SpaceFOM-specific compliance tests to come in the future.

    static class TestFederate extends SEELateJoinerFederate {
        protected TestFederate(SEEFederateAmbassador federateAmbassador, FederateConfiguration federateConfiguration) {
            super(federateAmbassador, federateConfiguration);
        }

        @Override
        public void declareClasses() throws FederateNotExecutionMember, AttributeNotDefined, ObjectClassNotDefined, RestoreInProgress, NameNotFound, NotConnected, RTIinternalError, InvalidObjectClassHandle, SaveInProgress, InvalidInteractionClassHandle, InteractionClassNotDefined, FederateServiceInvocationsAreBeingReportedViaMOM {

        }

        @Override
        public void declareObjectInstances() throws FederateNotExecutionMember, ObjectClassNotPublished, ObjectClassNotDefined, RestoreInProgress, ObjectInstanceNotKnown, IllegalName, ObjectInstanceNameInUse, ObjectInstanceNameNotReserved, NotConnected, RTIinternalError, SaveInProgress {

        }

        @Override
        public void update() {

        }
    }

    public static void main(String[] args) {
        FederateConfiguration config = FederateConfiguration.Factory.parse(confFile);
        SEELateJoinerFederate federate = new TestFederate(new SEEFederateAmbassador(), config);
        federate.configureAndStart();
    }
}
