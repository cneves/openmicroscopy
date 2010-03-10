/*
 *   $Id: SelfConfigurableService.java 1375 2007-03-20 11:23:44Z jmoore $
 *
 *   Copyright 2006 University of Dundee. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */
package ome.system;

import ome.api.ServiceInterface;

public interface SelfConfigurableService {

    void selfConfigure();
    Class<? extends ServiceInterface> getServiceInterface();
    
}
