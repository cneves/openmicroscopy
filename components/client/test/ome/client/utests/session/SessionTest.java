/*
 *   $Id: SessionTest.java 2003 2008-01-04 14:16:00Z jmoore $
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.client.utests.session;

import ome.api.IUpdate;
import ome.model.IObject;
import ome.model.containers.Project;

import org.jmock.Mock;
import org.testng.annotations.Test;

public class SessionTest extends AbstractTest {

    @Test
    public void test_initialTimeIsAfterNow() throws Exception {
        long last = session.lastModification();
        assertTrue("Didn't happen in past!!", last <= System
                .currentTimeMillis());
        // Use <= here because the interval can be too short.
    }

    @Test
    public void test_modificationIncrements() throws Exception {
        long original = session.lastModification();
        session.register(new Project());
        long updated = session.lastModification();

        assertTrue("Registering didn't update lastModification",
                original <= updated);

    }

    @Test
    public void test_registeredObjectCanBeFound() throws Exception {
        Project p = new Project(new Long(1L), true);
        p.setVersion(new Integer(1));
        session.register(p);
        Project p2 = (Project) session.find(Project.class, new Long(1L));
        assertTrue("Must be same instance", p == p2);
    }

    @Test
    public void test_mock_flush() throws Exception {
        Project retVal = new Project(new Long(1L), true);
        Mock m = mock(IUpdate.class);
        serviceFactory.mockUpdate = m;
        m.expects(atLeastOnce()).method("saveAndReturnArray").will(
                returnValue(new IObject[] {})).id("save");
        m.expects(atLeastOnce()).method("saveAndReturnArray").after("save")
                .will(returnValue(new IObject[] { retVal })).id("update");

        Project p = new Project(new Long(1L), true);
        p.setVersion(new Integer(1));
        session.markDirty(p);
        session.flush();
    }

    @Test
    public void test_newAndThenCheckOut() throws Exception {
        Project p_new = new Project();
        Project p_old = new Project(new Long(1L), true);
        p_old.setVersion(new Integer(1));
        IObject[] arr = new IObject[] { p_old };

        serviceFactory.mockUpdate = updateMockForFlush(arr, null);

        session.register(p_new);
        session.flush();

        Project p_test = (Project) session.checkOut(p_new);
        assertTrue("Must be identical", p_old == p_test);
    }

    @Test
    public void test_allMethodsThrowExceptionAfterClose() throws Exception {
        session.close();

        try {
            session.checkOut(null);
            fail("Should have thrown IllegalState.");
        } catch (IllegalStateException e) {
            // good;
        }

        try {
            session.register(null);
            fail("Should have thrown IllegalState.");
        } catch (IllegalStateException e) {
            // good;
        }

        try {
            session.delete(null);
            fail("Should have thrown IllegalState.");
        } catch (IllegalStateException e) {
            // good;
        }

        try {
            session.find(null, null);
            fail("Should have thrown IllegalState.");
        } catch (IllegalStateException e) {
            // good;
        }

        try {
            session.flush();
            fail("Should have thrown IllegalState.");
        } catch (IllegalStateException e) {
            // good;
        }

        try {
            session.lastModification();
            fail("Should have thrown IllegalState.");
        } catch (IllegalStateException e) {
            // good;
        }

        try {
            session.markDirty(null);
            fail("Should have thrown IllegalState.");
        } catch (IllegalStateException e) {
            // good;
        }

    }

}
