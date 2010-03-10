/*
 *   $Id: ConfigTest.java 2135 2008-02-07 11:21:08Z jmoore $
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.client.itests;

import java.util.UUID;

import junit.framework.TestCase;
import ome.api.IAdmin;
import ome.api.IConfig;
import ome.model.meta.Experimenter;
import ome.model.meta.ExperimenterGroup;
import ome.system.Login;
import ome.system.ServiceFactory;

import org.testng.annotations.Configuration;
import org.testng.annotations.Test;

/**
 * simple client-side test of the ome.api.IConfig service.
 * 
 * Also used as the main developer example for developing
 * (stateless/client-side) tests. See source code documentation for more.
 * 
 * @author Josh Moore, josh.moore at gmx.de
 * @version $Revision: 2135 $, $Date: 2008-02-07 11:21:08 +0000 (Thu, 07 Feb 2008) $
 * @since 3.0-M3
 */
/*
 * Developer's note: Like with the server-side test, the "integration" group is
 * important for having tests run properly. Here, however, it's needed because
 * there is no super-class.
 * 
 * Client-side tests test the full spectrum, including login, security
 * permissions, transactions and other interceptors, and the logic itself.
 * 
 * As with the method names (mentioned in the server-side version of this test),
 * we subclass from junit.framework.TestCase for some semblance of supporting
 * both testing platforms.
 */
@Test(groups = { "ticket:306", "config", "integration" })
public class ConfigTest extends TestCase {

    /*
     * Developer notes: --------------- In general, there is no superclass on
     * the client-side, though in some cases (like for security) one has been
     * created. There is no particular reason for this and all classes could be
     * made to inherit from a single class like AbstractManagedContextTest on
     * the server-side.
     */
    IConfig iConfig;

    /*
     * Developer notes: --------------- Here we override the setUp() method from
     * TestCase and add the @Configuration annotation TestNG so as to have both
     * frameworks properly initialize the test. (@Configuration has actually
     * been deprecated in TestNG 5. All our test classes will need to be
     * updated.)
     */
    @Override
    @Configuration(beforeTestClass = true)
    protected void setUp() throws Exception {
        /*
         * Developer notes: --------------- without further configuration,
         * ServiceFactory() uses your default (development) user for logging in.
         * This is to ensure a login as far as possible.
         */
        ServiceFactory sf = new ServiceFactory();
        iConfig = sf.getConfigService();

        /*
         * Developer notes: --------------- Due to
         * http://bugs.openmicroscopy.org.uk/show_bug.cgi?id=649 which causes
         * the first DB access after a new start or redeploy to fail, all tests
         * should attempt to access the DB during setup in a try/catch block
         */
        try {
            iConfig.getServerTime();
        } catch (Exception ex) {
            // ok.
        }

    }

    @Test
    public void testConfigGetServerTime() throws Exception {
        assertNotNull(iConfig.getServerTime());
    }

    @Test
    public void testConfigGetDBTime() throws Exception {
        assertNotNull(iConfig.getDatabaseTime());
    }

    @Test
    public void testGetMissingConfigValue() throws Exception {
        // HIGHLY unlikely that this will be available
        String value = iConfig.getConfigValue(UUID.randomUUID().toString());
        assertNull(value);

    }

    @Test(groups = "ignore")
    public void testThisTestDoesntWork() throws Exception {
        fail("Should not be called.");
    }

    /*
     * Developer notes: --------------- We can also test the ability for root or
     * a new user to perform the same task. To do this, we'll need to obtain or
     * create a Login for root. Here we look up the value from the OmeroContext.
     * (This is marked as deprecated, but that may change.)
     */
    @Test
    public void testAsOtherUsers() throws Exception {
        // get default service factory
        ServiceFactory sf = new ServiceFactory("ome.client.test");
        Login rootLogin = (Login) sf.getContext().getBean("rootLogin");

        ServiceFactory rootSf = new ServiceFactory(rootLogin);
        IConfig rootConfig = rootSf.getConfigService();
        rootConfig.getConfigValue("foo");
        rootConfig.setConfigValue("foo", "bar");

        // Now let's create another user.
        final IAdmin rootAdmin = rootSf.getAdminService();
        ExperimenterGroup g = new ExperimenterGroup();
        g.setName(UUID.randomUUID().toString());
        rootAdmin.createGroup(g);
        Experimenter e = new Experimenter();
        e.setOmeName(UUID.randomUUID().toString());
        e.setFirstName("Config");
        e.setLastName("Test");
        rootAdmin.createUser(e, g.getName()); // Not an admin or system user
        rootAdmin.changeUserPassword(e.getOmeName(), "bar");

        // And use it to login
        Login l = new Login(e.getOmeName(), "bar");
        ServiceFactory userSf = new ServiceFactory(l);
        assertNotNull(userSf.getConfigService().getServerTime());
        try {
            userSf.getConfigService().setConfigValue("foo", "bax");
            fail("Non-system users should not be able to configure the server.");
        } catch (Exception ex) {
            // ok
        }
    }
}
