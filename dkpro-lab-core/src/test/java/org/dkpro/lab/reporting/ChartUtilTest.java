/*******************************************************************************
 * Copyright 2016
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.junit.Test;

public class ChartUtilTest
{
    @Test
    public void testSvg()
        throws Exception
    {
        double[][] data = new double[2][10];

        for (int n = 1; n < 10; n++) {
            data[0][n] = 1.0 / n;
            data[1][n] = 1.0 - (1.0 / n);
        }

        DefaultXYDataset dataset = new DefaultXYDataset();
        dataset.addSeries("data", data);

        JFreeChart chart = ChartFactory.createXYLineChart(null, "Recall", "Precision", dataset,
                PlotOrientation.VERTICAL, false, false, false);
        chart.getXYPlot().setRenderer(new XYSplineRenderer());
        chart.getXYPlot().getRangeAxis().setRange(0.0, 1.0);
        chart.getXYPlot().getDomainAxis().setRange(0.0, 1.0);

        File tmp = Files.createTempFile("testfile", ".svg").toFile();
        try (OutputStream os = new FileOutputStream(tmp)) {
            ChartUtil.writeChartAsSVG(os, chart, 400, 400);
        }

//        String ref = FileUtils.readFileToString(new File("src/test/resources/chart/test.svg"),
//                "UTF-8");
//        String actual = FileUtils.readFileToString(tmp, "UTF-8");
//        assertEquals(ref, actual);
    }

    @Test
    public void testPDF()
        throws Exception
    {
        double[][] data = new double[2][10];

        for (int n = 1; n < 10; n++) {
            data[0][n] = 1.0 / n;
            data[1][n] = 1.0 - (1.0 / n);
        }

        DefaultXYDataset dataset = new DefaultXYDataset();
        dataset.addSeries("data", data);

        JFreeChart chart = ChartFactory.createXYLineChart(null, "Recall", "Precision", dataset,
                PlotOrientation.VERTICAL, false, false, false);
        chart.getXYPlot().setRenderer(new XYSplineRenderer());
        chart.getXYPlot().getRangeAxis().setRange(0.0, 1.0);
        chart.getXYPlot().getDomainAxis().setRange(0.0, 1.0);

        File tmp = Files.createTempFile("testfile", ".pdf").toFile();
        try (OutputStream os = new FileOutputStream(tmp)) {
            ChartUtil.writeChartAsPDF(os, chart, 400, 400);
        }

        // Do not have an assert here because the creation date encoded in the PDF changes
//        String ref = FileUtils.readFileToString(new File("src/test/resources/chart/test.pdf"),
//                "UTF-8");
//        String actual = FileUtils.readFileToString(tmp, "UTF-8");
//        assertEquals(ref, actual);
    }
}
