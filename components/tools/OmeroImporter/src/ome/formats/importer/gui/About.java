/*
 * ome.formats.importer.gui.About
 *
 *------------------------------------------------------------------------------
 *  Copyright (C) 2006-2008 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

 // This was an original package from LOCI Bio-Formats which has since been
 // depreciated and is Copyright (C) 2005 Melissa Linkert, Curtis Rueden, 
 // Chris Allan. Brian Loranger, and Eric Kjellman.

package ome.formats.importer.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import ome.formats.importer.ImportConfig;

import loci.formats.FormatTools;

/**
 * About is a small program for displaying version information in a dialog box.
 * It is intended to be used as a main class for JAR libraries to easily
 * determine library version and build date.
 */
public abstract class About
{

    private static String title;

    private static String msg;

    public static void show(JFrame c, ImportConfig config, boolean useSplashScreen)
    {
        
        if (useSplashScreen == true)
        {
            //SplashWindow.splash(Splasher.class.getResource(Main.splash));
        } else
        {

            if (title == null)
            {
                StringBuffer sb = new StringBuffer();
                try
                {
                    InputStream is = About.class.getResourceAsStream("about.txt");
                    if (is == null)
                    {
                        title = "About";
                        msg = "OMERO.importer developer's edition.";
                    } else
                    {
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(is));
                        while (true)
                        {
                            String line = in.readLine();
                            if (line == null) break;
                            if (title == null) title = "About " + line;
                            else
                                sb.append("\n");
                            sb.append(line);
                        }
                        in.close();
                        msg = sb.toString();
                    }
                } catch (IOException exc)
                {
                    if (title == null) title = "About";
                    msg = "OMERO.importer developer's edition.";
                    exc.printStackTrace();
                }
            }
            if (config.getAppTitle() != null)
                title = "About " + config.getAppTitle();
            msg = msg + "\n\n Version: " + config.getVersionNumber();
            msg = msg + "\n Bio-Formats " + FormatTools.VERSION + 
            " (SVN " + FormatTools.SVN_REVISION + ", " + FormatTools.DATE + ")";
            JOptionPane.showMessageDialog(c, msg, title,
                    JOptionPane.INFORMATION_MESSAGE); 
            c.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        }
    }

    public static void main(String[] args)
    {
        show(null, new ImportConfig(), false);
        System.exit(0);
    }

}
