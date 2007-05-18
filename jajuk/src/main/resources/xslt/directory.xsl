<?xml version='1.0' encoding='UTF-8'?>

<!-- 
	Description: This XSLT transforms a xml file containing the tagging of a directory into an html file.
	Author: The Jajuk Team
	Created: August 23, 2006
-->

<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform'
	version='1.0'>

	<xsl:template match='/'>
		<html>
			<head>
				<title>
					<xsl:value-of
						select='/collection/i18n/ReportAction.1' />
				</title>
				<style type='text/css'>
					.style { background-color: #f9f7ed; font-weight:
					bold; padding: 3px; }

					.author { background-color: #c3d9ff; font-weight:
					bold; padding: 3px; }

					.album { background-color: #cdeb8b; font-weight:
					bold; padding: 3px; }

					.track { font-weight: bold; background-color:
					#eeeeee; padding: 3px; width: 120px; }
				</style>
			</head>
			<body>
				<h1>
					<xsl:value-of
						select='/collection/i18n/ReportAction.1' />
				</h1>
				<p>
					<CAPTION>
						<xsl:value-of
							select='/collection/i18n/ReportAction.2' />
					</CAPTION>
				</p>
				<p>
					<ul>
						<li>
							<a href='#1'>
								<xsl:value-of
									select='/collection/i18n/ReportAction.10' />
							</a>
						</li>
						<li>
							<a href='#2'>
								<xsl:value-of
									select='/collection/i18n/ReportAction.11' />
							</a>
						</li>
					</ul>
				</p>
				<p>
					<h2 id='1'>
						<xsl:value-of
							select='/collection/i18n/ReportAction.10' />
					</h2>
					<xsl:call-template name='directories_list' />
				</p>
				<p>
					<h2 id='2'>
						<xsl:value-of
							select='/collection/i18n/ReportAction.11' />
					</h2>
					<xsl:apply-templates select='*/directory' />

				</p>
			</body>
		</html>
	</xsl:template>

	<xsl:template name='directories_list'>
		<xsl:for-each select='*/directory'>
			<xsl:variable name='id' select='id' />
				<a href='#{id}'>
					<xsl:value-of select='path' />
				</a>
				<br/>
			<xsl:call-template name='directories_list' />
		</xsl:for-each>
	</xsl:template>



<xsl:template match="*/directory">
	<hr />
		<xsl:variable name='id' select='id' />
		<h2 id='{id}'>
			<xsl:value-of select='path' />
		</h2>
		<xsl:apply-templates select="directory" />
			<table border='0' cellspacing='5'>
			<TR>
				<TH>
					<xsl:value-of
						select='/collection/i18n/ReportAction.12' />
				</TH>
				<TH>
					<xsl:value-of
						select='/collection/i18n/ReportAction.order' />
				</TH>
				<TH>
					<xsl:value-of
						select='/collection/i18n/ReportAction.13' />
				</TH>
				<TH>
					<xsl:value-of
						select='/collection/i18n/ReportAction.style' />
				</TH>
				<TH>
					<xsl:value-of
						select='/collection/i18n/ReportAction.author' />
				</TH>
				<TH>
					<xsl:value-of
						select='/collection/i18n/ReportAction.album' />
				</TH>
				<TH>
					<xsl:value-of
						select='/collection/i18n/ReportAction.length' />
				</TH>
				<TH>
					<xsl:value-of
						select='/collection/i18n/ReportAction.rate' />
				</TH>
				<TH>
					<xsl:value-of
						select='/collection/i18n/ReportAction.comment' />
				</TH>
			</TR>

			<xsl:for-each select='file'>
				<tr>
					<td>
						<xsl:value-of select='name' />
					</td>
					<td>
						<xsl:value-of select='track/order' />
					</td>
					<td class='track'>
						<xsl:value-of select='track/name' />
					</td>
					<td class='style'>
						<xsl:value-of select='track/style' />
					</td>
					<td class='author'>
						<xsl:value-of select='track/author' />
					</td>
					<td class='album'>
						<xsl:value-of select='track/album' />
					</td>
					<td>
						<xsl:value-of select='track/length' />
					</td>
					<td>
						<xsl:value-of select='track/rate' />
					</td>
					<td>
						<xsl:value-of select='track/comment' />
					</td>
				</tr>
			</xsl:for-each>
		</table>
		<br />				
	</xsl:template>


</xsl:stylesheet>