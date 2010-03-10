/*
 *   $Id: AbstractSecurityTest.java 5070 2009-09-24 10:37:03Z brain $
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.client.itests.sec;

import javax.sql.DataSource;

import junit.framework.TestCase;
import ome.api.IAdmin;
import ome.api.IQuery;
import ome.api.IUpdate;
import ome.model.meta.Experimenter;
import ome.system.Login;
import ome.system.ServiceFactory;

import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.testng.annotations.Test;

@Test(groups = { "client", "integration", "security" })
public class AbstractSecurityTest extends TestCase {

    protected ServiceFactory tmp = new ServiceFactory("ome.blitz.test");

    protected DataSource dataSource = (DataSource) tmp.getContext().getBean(
            "dataSource");

    protected SimpleJdbcTemplate jdbc = new SimpleJdbcTemplate(dataSource);

    protected Login rootLogin = (Login) tmp.getContext().getBean("rootLogin");

    protected ServiceFactory rootServices;

    protected IAdmin rootAdmin;

    protected IQuery rootQuery;

    protected IUpdate rootUpdate;

    // shouldn't use beforeTestClass here because called by all subclasses
    // in their beforeTestClass i.e. super.setup(); ...
    protected void init() throws Exception {
        rootServices = new ServiceFactory(rootLogin);
        rootAdmin = rootServices.getAdminService();
        rootQuery = rootServices.getQueryService();
        rootUpdate = rootServices.getUpdateService();
        try {
            rootQuery.get(Experimenter.class, 0l);
        } catch (Throwable t) {
            // TODO no, no, really. This is ok. (And temporary)
        }
    }
}
