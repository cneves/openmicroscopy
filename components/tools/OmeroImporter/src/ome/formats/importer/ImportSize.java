/*
 *   $Id$
 *
 *   Copyright 2009 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 */

package ome.formats.importer;

import omero.model.Pixels;

/**
 * Calculates the various dimensions of an image from a {@link Pixels} instance.
 * 
 * @since Beta4.1
 */

public class ImportSize {

    public final String fileName;
    public final Pixels pixels;
    public final String dimOrder;
    public final int sizeX, sizeY, sizeZ, sizeC, sizeT, imageCount;
    public final int zSize, wSize, tSize;

    public ImportSize(String fileName, Pixels pixels, String dimOrder) {
        this.fileName = fileName;
        this.pixels = pixels;
        this.dimOrder = dimOrder;

        sizeZ = pixels.getSizeZ().getValue();
        sizeC = pixels.getSizeC().getValue();
        sizeT = pixels.getSizeT().getValue();
        sizeX = pixels.getSizeX().getValue();
        sizeY = pixels.getSizeY().getValue();
        imageCount = sizeZ * sizeC * sizeT;

        final int order = getSequenceNumber(dimOrder);

        int smallOffset = 1;
        switch (order) {
        // ZTW sequence
        case 0:
            zSize = smallOffset;
            tSize = zSize * sizeZ;
            wSize = tSize * sizeT;
            break;
        // WZT sequence
        case 1:
            wSize = smallOffset;
            zSize = wSize * sizeC;
            tSize = zSize * sizeZ;
            break;
        // ZWT sequence
        case 2:
            zSize = smallOffset;
            wSize = zSize * sizeZ;
            tSize = wSize * sizeC;
            break;
        // TWZ sequence
        case 3:
            tSize = smallOffset;
            wSize = tSize * sizeT;
            zSize = wSize * sizeC;
            break;
        // WTZ sequence
        case 4:
            wSize = smallOffset;
            tSize = wSize * sizeC;
            zSize = tSize * sizeT;
            break;
        // TZW
        case 5:
            tSize = smallOffset;
            zSize = tSize * sizeT;
            wSize = zSize * sizeZ;
            break;
        default:
            throw new RuntimeException("Bad order");
        }
    }

    private int getSequenceNumber(String dimOrder) {
        if (dimOrder.equals("XYZTC"))
            return 0;
        if (dimOrder.equals("XYCZT"))
            return 1;
        if (dimOrder.equals("XYZCT"))
            return 2;
        if (dimOrder.equals("XYTCZ"))
            return 3;
        if (dimOrder.equals("XYCTZ"))
            return 4;
        if (dimOrder.equals("XYTZC"))
            return 5;
        throw new RuntimeException(dimOrder + " not represented in "
                + "getSequenceNumber");
    }

}
