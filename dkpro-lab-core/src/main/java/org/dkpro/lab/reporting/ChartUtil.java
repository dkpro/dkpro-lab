/*******************************************************************************
 * Copyright 2011
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *   
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.dkpro.lab.reporting;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_INTERPOLATION;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.fop.svg.PDFDocumentGraphics2D;
import org.apache.xmlgraphics.java2d.GraphicContext;
import org.jfree.chart.JFreeChart;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public class ChartUtil
{
	/**
	 * Exports a JFreeChart to a SVG file.
	 *
	 * @param chart JFreeChart to export
	 * @param aOS stream to write to.
	 * @param aWidth width of the chart in pixels
	 * @param aHeight height of the chart in pixels
	 * @throws IOException if writing the svgFile fails.
	 * @see <a href="http://dolf.trieschnigg.nl/jfreechart/">Saving JFreeChart as SVG vector images
	 *      using Batik</a>
	 */
	public static void writeChartAsSVG(OutputStream aOS, JFreeChart chart, int aWidth, int aHeight)
		throws IOException
	{
		// Get a DOMImplementation and create an XML document
		DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
		Document document = domImpl.createDocument(null, "svg", null);

		// Create an instance of the SVG Generator
		SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

		// draw the chart in the SVG generator
		chart.draw(svgGenerator, new Rectangle(aWidth, aHeight));

		// Write svg file
		Writer out = new OutputStreamWriter(aOS, "UTF-8");
		svgGenerator.stream(out, true /* use css */);
		out.flush();
		out.close();
	}

	/**
	 * Exports a JFreeChart to a scalable PDF file.
	 *
	 * @param chart JFreeChart to export
	 * @param aOS stream to write to.
	 * @param aWidth width of the chart in pixels
	 * @param aHeight height of the chart in pixels
	 * @throws IOException if writing the svgFile fails.
	 */
	public static void writeChartAsPDF(OutputStream aOS, JFreeChart chart, int aWidth, int aHeight)
		throws IOException
	{
		// Create an instance of the SVG Generator
		PDFDocumentGraphics2D pdfGenerator = new PDFDocumentGraphics2D(true, aOS, aWidth, aHeight);
		pdfGenerator.setDeviceDPI(PDFDocumentGraphics2D.NORMAL_PDF_RESOLUTION);
		pdfGenerator.setGraphicContext(new GraphicContext());
		pdfGenerator.setSVGDimension(aWidth, aHeight);
		pdfGenerator.setClip(0, 0, aWidth, aHeight);
		pdfGenerator.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
		pdfGenerator.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR);
		chart.setBackgroundPaint(Color.white);
		chart.getPlot().setBackgroundPaint(Color.white);
		// draw the chart in the SVG generator
		chart.draw(pdfGenerator, new Rectangle(aWidth, aHeight));
		pdfGenerator.finish();
	}
}
