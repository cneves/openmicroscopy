#!/usr/bin/env python
# -*- coding: utf-8 -*-

"""
   conftest.py - py.test fixtures for gatewaytest

   Copyright 2013 Glencoe Software, Inc. All rights reserved.
   Use is subject to license terms supplied in LICENSE.txt

"""
import omero
from omero.rtypes import rstring

#from omero.gateway.scripts import dbhelpers
from omero.gateway.scripts.testdb_create import *


import pytest

class GatewayWrapper (TestDBHelper):
    def __init__ (self):
        super(GatewayWrapper, self).__init__()
        self.setUp(skipTestDB=False, skipTestImages=True)

    def createTestImg_generated (self):
        ds = self.getTestDataset()
        assert ds
        testimg = self.createTestImage(dataset=ds)
        return testimg


@pytest.fixture(scope='module')
def gatewaywrapper (request):
    """
    Returns a test helper gateway object.
    """
    g = GatewayWrapper()
    def fin ():
        g.tearDown()
        dbhelpers.cleanup()
    request.addfinalizer(fin)
    return g


@pytest.fixture(scope='function')
def author_testimg_generated (gatewaywrapper):
    """
    logs in as Author and returns the test image, creating it first if needed.
    """
    gatewaywrapper.loginAsAuthor()
    return gatewaywrapper.createTestImg_generated()
