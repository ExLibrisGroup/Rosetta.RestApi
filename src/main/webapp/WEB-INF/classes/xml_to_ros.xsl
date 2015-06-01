<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:mets="http://www.loc.gov/METS/" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:template match="/mets:mets">
		<mets:mets xmlns:mets="http://www.loc.gov/METS/">
			<xsl:copy-of select="mets:dmdSec"/>
			<xsl:for-each select="mets:amdSec">
				<xsl:copy>
					<xsl:attribute name="ID"><xsl:value-of select="@ID" /></xsl:attribute>
					<xsl:if test="starts-with(@ID,'rep')">
					  <xsl:element name="mets:techMD">
				        <xsl:attribute name="ID"><xsl:value-of select="@ID"/>-tech</xsl:attribute>
					      <mets:mdWrap MDTYPE="OTHER" OTHERMDTYPE="dnx">
					        <mets:xmlData>
					          <dnx xmlns="http://www.exlibrisgroup.com/dps/dnx">
					            <section id="generalRepCharacteristics">
					              <record>
					                <key id="preservationType">PRESERVATION_MASTER</key>
					                <key id="usageType">VIEW</key>
					                <key id="RevisionNumber">1</key>
					                <key id="DigitalOriginal">True</key>
					              </record>
					            </section>
					          </dnx>
					        </mets:xmlData>
					      </mets:mdWrap>
					  </xsl:element>
			      	</xsl:if>
					<xsl:copy-of select="mets:rightsMD"/>
					<xsl:copy-of select="mets:sourceMD"/>
				</xsl:copy>
			</xsl:for-each>
			<xsl:copy-of select="mets:fileSec"/>
		</mets:mets>
	</xsl:template>
</xsl:stylesheet>