/*
 * Copyright 2016 Longri
 * Copyright 2016-2018 devemux86
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.ios.backend;

import org.oscim.backend.CanvasAdapter;
import org.oscim.utils.GraphicUtils;
import org.oscim.utils.IOUtils;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.coregraphics.CGSize;
import org.robovm.apple.uikit.UIImage;
import svg.SVGRenderer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

public class IosSvgBitmap extends IosBitmap {
    private static final Logger log = Logger.getLogger(IosSvgBitmap.class.getName());

    /**
     * Default size is 20x20px (400px) at 160dpi.
     */
    public static float DEFAULT_SIZE = 400f;

    private static String getStringFromInputStream(InputStream is) {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            log.severe(e.toString());
        } finally {
            IOUtils.closeQuietly(br);
        }
        return sb.toString();
    }

    public static UIImage getResourceBitmap(InputStream inputStream, float scaleFactor, float defaultSize, int width, int height, int percent) {
        String svg = getStringFromInputStream(inputStream);
        SVGRenderer renderer = new SVGRenderer(svg);
        CGRect viewRect = renderer.getViewRect();

        double scale = scaleFactor / Math.sqrt((viewRect.getHeight() * viewRect.getWidth()) / defaultSize);

        float[] bmpSize = GraphicUtils.imageSize((float) viewRect.getWidth(), (float) viewRect.getHeight(), (float) scale, width, height, percent);

        return renderer.asImageWithSize(new CGSize(bmpSize[0], bmpSize[1]), 1);
    }

    private static UIImage getResourceBitmapImpl(InputStream inputStream, int width, int height, int percent) {
        return getResourceBitmap(inputStream, CanvasAdapter.getScale(), DEFAULT_SIZE, width, height, percent);
    }

    public IosSvgBitmap(InputStream inputStream, int width, int height, int percent) throws IOException {
        super(getResourceBitmapImpl(inputStream, width, height, percent));
    }
}
