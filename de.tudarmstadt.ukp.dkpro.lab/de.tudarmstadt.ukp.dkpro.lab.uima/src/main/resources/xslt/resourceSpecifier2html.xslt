<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:uima="http://uima.apache.org/resourceSpecifier" version="1.0">
    <xd:doc xmlns:xd="http://www.oxygenxml.com/ns/doc/xsl" scope="stylesheet">
        <xd:desc>
            <xd:p><xd:b>Created on:</xd:b> Oct 25, 2009</xd:p>
            <xd:p><xd:b>Author:</xd:b> Richard Eckart de Castilho</xd:p>
            <xd:p></xd:p>
        </xd:desc>
    </xd:doc>
    
    <xsl:template match="uima:analysisEngineMetaData | uima:processingResourceMetaData">
        <xsl:apply-templates select="uima:configurationParameterSettings"/>
    </xsl:template>
    
    <xsl:template match="uima:nameValuePair">
        <tr>
            <th><xsl:value-of select="uima:name"/></th>
            <td><xsl:apply-templates select="uima:value"/></td>
        </tr>
    </xsl:template>
    
    <xsl:template match="uima:configurationParameterSettings">
        <table class="configurationParameterSettings">
            <tbody>
                <xsl:for-each select="uima:nameValuePair">
                    <xsl:sort select="uima:name"/>
                    <xsl:apply-templates select="."/>
                </xsl:for-each>
            </tbody>
        </table>
    </xsl:template>
    
    <xsl:template match="uima:analysisEngineDescription">
        <div class="analysisEngineDescription">
            <xsl:choose>
                <xsl:when test="uima:primitive = 'true'">
                    <h4><xsl:value-of select="uima:annotatorImplementationName"/></h4>
                </xsl:when>
                <xsl:otherwise>
                    <h4>Aggregate Analysis Engine</h4>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:apply-templates select="uima:analysisEngineMetaData"/>
            <xsl:apply-templates select="uima:delegateAnalysisEngineSpecifiers"/>
        </div>
    </xsl:template>

    <xsl:template match="uima:collectionReaderDescription">
        <div class="analysisEngineDescription">
            <h4><xsl:value-of select="uima:annotatorImplementationName"/></h4>
            <xsl:apply-templates select="uima:processingResourceMetaData"/>
        </div>
    </xsl:template>
    
    <xsl:template match="uima:delegateAnalysisEngineSpecifiers">
        <div class="delegateAnalysisEngineSpecifiers">
            <xsl:apply-templates/>
        </div>
    </xsl:template>
    
    <xsl:template match="/">
        <html>
            <head>
                <style type="text/css">
                    .analysisEngineDescription {
                        border: solid black 1px;
                    }
                    .delegateAnalysisEngineSpecifiers {
                        margin-top: 1em;
                        margin-left: 2.5em;
                        border: solid black 1px;
                    }
                    .configurationParameterSettings th {
                        text-align: left;
                        vertical-align: text-top;
                    }
                    .analysisEngineDescription h4 {
                        margin: 0px 0px 0px 0px;
                        padding: 0px 0px 0px 0px;
                        background: lightgray;
                    }
                </style>
            </head>
            <body>
                <xsl:apply-templates/>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>
