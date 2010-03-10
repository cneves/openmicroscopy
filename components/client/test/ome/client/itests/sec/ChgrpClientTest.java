/*
 *   $Id: ChgrpClientTest.java 1167 2006-12-15 10:39:34Z jmoore $
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.client.itests.sec;

import org.testng.annotations.*;

import ome.conditions.SecurityViolation;
import ome.model.core.Image;
import ome.model.internal.Permissions;
import ome.system.ServiceFactory;

@Test(groups = { "client", "integration", "security", "ticket:52", "chgrp" })
public class ChgrpClientTest extends AbstractChangeDetailClientTest {

    // design parameters:
    // 1. new or existing object (belonging to root, user, or other)
    // 2. as user or root
    // 3. changing to system ,user group, or some third group
    //
    // TODO: things work differently here because the images weren't carefully
    // placed within a particular group. Rather,this is a direct copy of the
    // ChownClientTest. This needs to be worked on.

    // ~ AS USER TO ROOT
    // =========================================================================

    @Test
    @ExpectedExceptions(SecurityViolation.class)
    public void test_NewImageAsUserChgrpToSystem() throws Exception {
        createAsUserToGroup(asUser, toSystem);
    }

    @Test
    @ExpectedExceptions(SecurityViolation.class)
    public void test_UserImageAsUserChgrpToSystem() throws Exception {
        updateAsUserToGroup(managedImage(asUser), asUser, toSystem);
    }

    @Test
    @ExpectedExceptions(SecurityViolation.class)
    public void test_OtherImageAsUserChgrpToSystem() throws Exception {
        updateAsUserToGroup(managedImage(asOther), asUser, toSystem);
    }

    @Test
    // already belongs to system group
    public void test_RootImageAsUserChgrpToSystem() throws Exception {
        updateAsUserToGroup(managedImage(asRoot), asUser, toSystem);
    }

    // ~ AS USER TO USER
    // =========================================================================
    @Test
    public void test_NewImageAsUserChgrpToUserGroup() throws Exception {
        createAsUserToGroup(asUser, toUserGroup);
    }

    @Test
    public void test_UserImageAsUserChgrpToUserGroup() throws Exception {
        updateAsUserToGroup(managedImage(asUser), asUser, toUserGroup);
    }

    @Test
    @ExpectedExceptions(SecurityViolation.class)
    // default permissions causes this to be illegal
    public void test_OtherImageAsUserChgrpToUserGroup() throws Exception {
        updateAsUserToGroup(managedImage(asOther), asUser, toUserGroup);
    }

    @Test
    public void test_OtherGroupWritableImageAsUserChgrpToUserGroup()
            throws Exception {
        Long groupWritableImage = managedImage(asOther);
        new ServiceFactory(asOther).getAdminService().changePermissions(
                new Image(groupWritableImage, false),
                Permissions.GROUP_WRITEABLE);
        updateAsUserToGroup(groupWritableImage, asUser, toUserGroup);
    }

    @Test
    @ExpectedExceptions(SecurityViolation.class)
    // because root logged into sys.
    public void test_RootImageAsUserChgrpToUserGroup() throws Exception {
        updateAsUserToGroup(managedImage(asRoot), asUser, toUserGroup);
    }

    // ~ AS USER TO OTHER
    // =========================================================================
    @Test
    @ExpectedExceptions(SecurityViolation.class)
    public void test_NewImageAsUserChgrpToOtherGroup() throws Exception {
        createAsUserToGroup(asUser, toOtherGroup);
    }

    @Test
    @ExpectedExceptions(SecurityViolation.class)
    public void test_UserImageAsUserChgrpToOtherGroup() throws Exception {
        updateAsUserToGroup(managedImage(asUser), asUser, toOtherGroup);
    }

    @Test
    @ExpectedExceptions(SecurityViolation.class)
    public void test_OtherImageAsUserChgrpToOtherGroup() throws Exception {
        updateAsUserToGroup(managedImage(asOther), asUser, toOtherGroup);
    }

    @Test
    @ExpectedExceptions(SecurityViolation.class)
    public void test_RootImageAsUserChgrpToOtherGroup() throws Exception {
        updateAsUserToGroup(managedImage(asRoot), asUser, toOtherGroup);
    }

    // ~ AS ROOT TO USER
    // =========================================================================
    @Test
    public void test_NewImageAsRootChgrpToUserGroup() throws Exception {
        createAsUserToGroup(asRoot, toUserGroup);
    }

    @Test
    public void test_UserImageAsRootChgrpToUserGroup() throws Exception {
        updateAsUserToGroup(managedImage(asUser), asRoot, toUserGroup);
    }

    @Test
    public void test_OtherImageAsRootChgrpToUserGroup() throws Exception {
        updateAsUserToGroup(managedImage(asOther), asRoot, toUserGroup);
    }

    @Test
    public void test_RootImageAsRootChgrpToUserGroup() throws Exception {
        updateAsUserToGroup(managedImage(asRoot), asRoot, toUserGroup);
    }

    // ~ AS ROOT TO OTHER
    // =========================================================================
    @Test
    public void test_NewImageAsRootChgrpToOtherGroup() throws Exception {
        createAsUserToGroup(asRoot, toOtherGroup);
    }

    @Test
    public void test_UserImageAsRootChgrpToOtherGroup() throws Exception {
        updateAsUserToGroup(managedImage(asUser), asRoot, toOtherGroup);
    }

    @Test
    public void test_OtherImageAsRootChgrpToOtherGroup() throws Exception {
        updateAsUserToGroup(managedImage(asOther), asRoot, toOtherGroup);
    }

    @Test
    public void test_RootImageAsRootChgrpToOtherGroup() throws Exception {
        updateAsUserToGroup(managedImage(asRoot), asRoot, toOtherGroup);
    }

    // ~ AS ROOT TO ROOT
    // =========================================================================

    @Test
    public void test_NewImageAsRootChgrpToSystem() throws Exception {
        createAsUserToGroup(asRoot, toSystem);
    }

    @Test
    public void test_UserImageAsRootChgrpToSystem() throws Exception {
        updateAsUserToGroup(managedImage(asUser), asRoot, toSystem);
    }

    @Test
    public void test_OtherImageAsRootChgrpToSystem() throws Exception {
        updateAsUserToGroup(managedImage(asOther), asRoot, toSystem);
    }

    @Test
    public void test_RootImageAsRootChgrpToSystem() throws Exception {
        updateAsUserToGroup(managedImage(asRoot), asRoot, toSystem);
    }

}
