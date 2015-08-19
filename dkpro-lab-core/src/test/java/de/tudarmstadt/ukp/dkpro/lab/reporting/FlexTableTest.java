/*******************************************************************************
 * Copyright 2015
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
package de.tudarmstadt.ukp.dkpro.lab.reporting;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Test class for FlexTable.
 */
public class FlexTableTest
{
    private FlexTable<String> table = null;

    @Before
    public void setUp()
        throws Exception
    {
        this.table = FlexTable.forClass(String.class);

        this.table.addColumns("Col 1", "Col 2", "Col 3");
        Map<String, String> row1 = new LinkedHashMap<>();
        row1.put("Col 1", "Val 1, 1");
        row1.put("Col 2", "Val 2, 1");
        row1.put("Col 3", "Val 3, 1");
        this.table.addRow("Row 1", row1);

        Map<String, String> row2 = new LinkedHashMap<>();
        row2.put("Col 1", "Val 1, 2");
        row2.put("Col 2", "Val 2, 2");
        this.table.addRow("Row 2", row2);

        Map<String, String> row3 = new LinkedHashMap<>();
        row3.put("Col 1", "Val 1, 3");
        row3.put("Col 3", "Val 3, 3");
        this.table.addRow("Row 3", row3);

        Map<String, String> row4 = new LinkedHashMap<>();
        row4.put("Col 1", "Val 1, 4");
        row4.put("Col 2", "Val 2, 4");
        row4.put("Col 3", "Val 3, 4");
        this.table.addRow("Row 4", row4);

        System.out.println("Table created: \n" + this.table);
    }

    @After
    public void tearDown()
        throws Exception
    {
        // placeholder
    }

    @Test
    public void testTranspose()
    {
        // originalTable = this.table.clone();
        this.table.transposeTable();

        FlexTable<String> transposedTable = this.table;

        System.out.println("Table transposed: \n" + transposedTable);
        // TODO MW: Create a proper toString() for FlexTable that outputs the table in a nice
        // format.

        String[] columnIds = transposedTable.getColumnIds();
        Assert.assertEquals("The table should now have 4 columns.", 4, columnIds.length);
        String[] rowIds = transposedTable.getRowIds();
        Assert.assertEquals("The table should now have 3 rows.", 3, rowIds.length);

        Assert.assertEquals("The first column should have the header 'Row 1'", columnIds[0],
                "Row 1");
        Assert.assertEquals("The first row should have the ID 'Col 1'", rowIds[0], "Col 1");

        Map<String, String> firstRow = transposedTable.getRow("Col 1");
        Assert.assertEquals("The first row should have 4 entries.", 4, firstRow.size());

        for (String colID : columnIds) {
            Assert.assertTrue("The row should contain column ID " + colID,
                    firstRow.containsKey(colID));
        }
    }
}
