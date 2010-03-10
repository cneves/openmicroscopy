/*
 *   $Id: Preferences2Test.java 1167 2006-12-15 10:39:34Z jmoore $
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.client.utests;

//Java imports
import java.util.List;

//Third-party libraries
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.testng.annotations.Configuration;
import org.testng.annotations.Test;

//Application-internal dependencies

/**
 * 
 * @author Josh Moore &nbsp;&nbsp;&nbsp;&nbsp; <a
 *         href="mailto:josh.moore@gmx.de">josh.moore@gmx.de</a>
 * @version 1.0 <small> (<b>Internal version:</b> $Rev: 1167 $ $Date: 2006-12-15 10:39:34 +0000 (Fri, 15 Dec 2006) $) </small>
 * @since 1.0
 */
public class Preferences2Test extends
        AbstractDependencyInjectionSpringContextTests {

    static {
        System.getProperties().setProperty("test.system.value",
                "This was set at class load.");
        System.getProperties().setProperty("test.file.value",
                "An attempt to override.");
    }

    @Override
    protected String[] getConfigLocations() {
        return new String[] { "ome/client/utests/no_props.xml" };
    }

    List l;

    public void setList(List list) {
        this.l = list;
    }

    @Test
    public void testArePreferencesSet() {
        // if this doesn't explode it's fine.
    }

    // ~ Testng Adapter
    // =========================================================================
    @Configuration(beforeTestMethod = true)
    void adapterSetup() throws Exception {
        super.setUp();
    }

    @Configuration(afterTestMethod = true)
    void adapterTearDown() throws Exception {
        super.tearDown();
    }

}
