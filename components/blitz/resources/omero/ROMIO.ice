/*
 *   $Id$
 *
 *   Copyright 2007 Glencoe Software, Inc. All rights reserved.
 *   Use is subject to license terms supplied in LICENSE.txt
 *
 */

#ifndef OMERO_IO_ICE
#define OMERO_IO_ICE

#include <omero/ServerErrors.ice>
#include <Ice/BuiltinSequences.ice>

module omero
{

/**

Primitives for working with binary data.

@see omero::api::RenderingEngine
@see omero::api::RawPixelsStore

 **/
module romio
{
    sequence<Ice::ByteSeq> RGBBands;

    const int RedBand = 0;
    const int GreenBand = 1;
    const int BlueBand = 2;

    class RGBBuffer
    {
      RGBBands bands;
      int sizeX1;
      int sizeX2;
    };

    const int XY = 0;
    const int ZY = 1;
    const int XZ = 2;

    class PlaneDef
    {
      int slice;
      int x;
      int y;
      int z;
      int t;
    };

    class CodomainMapContext
    {
    };
};

};

#endif  // OMERO_IO_ICE
