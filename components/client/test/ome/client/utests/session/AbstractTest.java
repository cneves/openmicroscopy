/*
 *   $Id: AbstractTest.java 1167 2006-12-15 10:39:34Z jmoore $
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.client.utests.session;

import java.util.Arrays;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Constraint;
import org.testng.annotations.Configuration;

import ome.api.IUpdate;
import ome.client.Session;
import ome.model.IObject;
import ome.testing.MockServiceFactory;

public class AbstractTest extends MockObjectTestCase {

    /** subclassed to not create context, and to return mock objects */
    // TODO move to ome.testing
    MockServiceFactory serviceFactory = new MockServiceFactory();

    Session session;

    @Override
    @Configuration(beforeTestMethod = true)
    protected void setUp() throws Exception {
        super.setUp();
        session = new Session(serviceFactory);
    }

    @Override
    @Configuration(afterTestMethod = true)
    protected void tearDown() throws Exception {
        super.tearDown();
    };

    /**
     * takes two arrays for creating the {@link Mock} for {@link IUpdate}.
     * 
     * @param createdEntitites
     *            array to return from
     *            {@link IUpdate#saveAndReturnArray(ome.model.IObject[])} for
     *            new entities.
     * @param updatedEntities
     *            array to return from
     *            {@link IUpdate#saveAndReturnArray(ome.model.IObject[])} for
     *            dirty entities.
     */
    protected Mock updateMockForFlush(IObject[] createdEntitites,
            IObject[] updatedEntities) {
        if (createdEntitites == null) {
            createdEntitites = new IObject[] {};
        }
        if (updatedEntities == null) {
            updatedEntities = new IObject[] {};
        }

        Mock m = mock(IUpdate.class);
        m.expects(once()).method("saveAndReturnArray").will(
                returnValue(createdEntitites)).id("new");
        m.expects(once()).method("saveAndReturnArray").after("new").will(
                returnValue(updatedEntities)).id("dirty");
        return m;
    }

}

// currently unused TODO move to ome.testing
class EqualArray implements Constraint {
    private IObject[] array;

    public EqualArray(IObject[] array) {
        this.array = array;
    }

    public boolean eval(Object o) {
        return o instanceof IObject[] && true; // FIXME here
    }

    public StringBuffer describeTo(StringBuffer buffer) {
        return buffer.append("an array equal to \"").append(
                Arrays.toString(array)).append("\"");
    }
}
