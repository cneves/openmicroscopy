package ome.formats.utests;

import ome.formats.OMEROMetadataStoreClient;
import ome.formats.model.BlitzInstanceProvider;
import ome.util.LSID;
import omero.api.ServiceFactoryPrx;
import omero.model.PlaneInfo;
import junit.framework.TestCase;

public class PlaneInfoProcessorTest extends TestCase
{
	private OMEROMetadataStoreClient store;
	
	private static final int IMAGE_INDEX = 1;
	
	private static final int PIXELS_INDEX = 1;
	
	private static final int PLANE_INFO_INDEX = 1;
	
	@Override
	protected void setUp() throws Exception
	{
		ServiceFactoryPrx sf = new TestServiceFactory();
        store = new OMEROMetadataStoreClient();
        store.initialize(sf);
        store.setEnumerationProvider(new TestEnumerationProvider());
        store.setInstanceProvider(
        		new BlitzInstanceProvider(store.getEnumerationProvider()));
        store.setPlaneTheC(0, IMAGE_INDEX, PIXELS_INDEX, PLANE_INFO_INDEX);
        store.setPlaneTheZ(0, IMAGE_INDEX, PIXELS_INDEX, PLANE_INFO_INDEX);
        store.setPlaneTheT(0, IMAGE_INDEX, PIXELS_INDEX, PLANE_INFO_INDEX);
        store.setPlaneTheC(1, IMAGE_INDEX, PIXELS_INDEX, PLANE_INFO_INDEX + 1);
        store.setPlaneTheZ(1, IMAGE_INDEX, PIXELS_INDEX, PLANE_INFO_INDEX + 1);
        store.setPlaneTheT(1, IMAGE_INDEX, PIXELS_INDEX, PLANE_INFO_INDEX + 1);
        store.setPlaneTheC(2, IMAGE_INDEX, PIXELS_INDEX, PLANE_INFO_INDEX + 2);
        store.setPlaneTheZ(2, IMAGE_INDEX, PIXELS_INDEX, PLANE_INFO_INDEX + 2);
        store.setPlaneTheT(2, IMAGE_INDEX, PIXELS_INDEX, PLANE_INFO_INDEX + 2);
        store.setPlaneTimingDeltaT(1.0f, IMAGE_INDEX, PIXELS_INDEX,
        		                   PLANE_INFO_INDEX +2);
	}
	
	public void testPlaneInfoExists()
	{
		assertEquals(3, store.countCachedContainers(PlaneInfo.class, null));
		LSID planeInfoLSID1 = new LSID(PlaneInfo.class, IMAGE_INDEX,
				                       PIXELS_INDEX, PLANE_INFO_INDEX);
		LSID planeInfoLSID2 = new LSID(PlaneInfo.class, IMAGE_INDEX,
                                       PIXELS_INDEX, PLANE_INFO_INDEX + 1);
		LSID planeInfoLSID3 = new LSID(PlaneInfo.class, IMAGE_INDEX,
                                       PIXELS_INDEX, PLANE_INFO_INDEX + 2);
		PlaneInfo pi1 = (PlaneInfo) store.getSourceObject(planeInfoLSID1);
		PlaneInfo pi2 = (PlaneInfo) store.getSourceObject(planeInfoLSID2);
		PlaneInfo pi3 = (PlaneInfo) store.getSourceObject(planeInfoLSID3);
		assertNotNull(pi1);
		assertNotNull(pi2);
		assertNotNull(pi3);
		assertEquals(0, pi1.getTheC().getValue());
		assertEquals(0, pi1.getTheZ().getValue());
		assertEquals(0, pi1.getTheT().getValue());
		assertEquals(1, pi2.getTheC().getValue());
		assertEquals(1, pi2.getTheZ().getValue());
		assertEquals(1, pi2.getTheT().getValue());
		assertEquals(2, pi3.getTheC().getValue());
		assertEquals(2, pi3.getTheZ().getValue());
		assertEquals(2, pi3.getTheT().getValue());
		assertEquals(1.0, pi3.getDeltaT().getValue());
	}
	
	public void testPlaneInfoCleanup()
	{
		store.postProcess();
		assertEquals(1, store.countCachedContainers(PlaneInfo.class, null));
		LSID planeInfoLSID = new LSID(PlaneInfo.class, IMAGE_INDEX,
                PIXELS_INDEX, PLANE_INFO_INDEX + 2);
		PlaneInfo pi = (PlaneInfo) store.getSourceObject(planeInfoLSID);
		assertNotNull(pi);
		assertEquals(2, pi.getTheC().getValue());
		assertEquals(2, pi.getTheZ().getValue());
		assertEquals(2, pi.getTheT().getValue());
		assertEquals(1.0, pi.getDeltaT().getValue());
	}
}
