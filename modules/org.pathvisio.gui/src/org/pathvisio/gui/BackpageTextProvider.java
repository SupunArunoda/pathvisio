// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2011 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package org.pathvisio.gui;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bridgedb.AttributeMapper;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.pathvisio.core.debug.Logger;
import org.pathvisio.core.debug.WorkerThreadOnly;
import org.pathvisio.core.model.ObjectType;
import org.pathvisio.core.model.PathwayElement;
import org.pathvisio.core.util.Resources;
import org.pathvisio.core.util.Utils;
import org.pathvisio.gui.DataPaneTextProvider.DataHook;

/**
 * BackpageTextProvider knows how to generate a html "backpage" for a given PathwayElement.
 * The backpage consists of a template, and several sections. The sections are each
 * generated by an implementation of @link{BackpageHook}, and plugins can register
 * more backpage hooks to extend the information in the backpage.
 * <p>
 * Two basic BackpageHooks are defined here: @link{BackpageAttributes} and
 * {@link BackpageXrefs}. However, these are not automatically registered, that
 * is the responsibility of the instantiator.
 */
public class BackpageTextProvider
{
	/**
	 * Hook into the backpage text provider,
	 * use this to generate a fragment of text for the backpage
	 */
	public static interface BackpageHook
	{
		/**
		 * Return a fragment of html-formatted text. The returned fragment should not
		 * contain &lt;html> or &lt;body> tags, but it can contain most other
		 * html tags.
		 * <p>
		 * The function getHtml is normally called from a worker thread.
		 */
		@WorkerThreadOnly
		public String getHtml (PathwayElement e);
	}

	/**
	 * A @{link BackpageHook} that generates a section with a description
	 * and a few other attributes to the backpage panel.
	 */
	public static class BackpageAttributes implements BackpageHook, DataHook
	{
		private final AttributeMapper attributeMapper;

		public BackpageAttributes (AttributeMapper attr)
		{
			attributeMapper = attr;
		}
		
		public String getType(PathwayElement e) {
			ObjectType obj = e.getObjectType();
			if(obj.equals(ObjectType.LINE)) {
				return "Interaction";
			} else {
				return e.getDataNodeType();
			}
		}
		
		public String getHtml(PathwayElement e) {
			String text = "";
			String type = getType(e);

			text += "<H1><font color=\"006699\">" + type + " annotation</font></H1><br>";

			if(e.getXref().getId() == null || "".equals(e.getXref().getId())) {
				text += "<font color='red'>Invalid annotation: missing identifier.</font>";
				return text;
			}
			
			try
			{
				StringBuilder bpInfo = new StringBuilder("<TABLE border = 1>");

				Map<String, Set<String>> attributes = null;
				if(e.getXref().getDataSource() != null) {
					attributes = attributeMapper.getAttributes(e.getXref());
				} else {
					attributes = new HashMap<String, Set<String>>();
				}

				 String[][] table = new String[][] {
						{"Name", Utils.oneOf(attributes.get("Symbol"))},	
						{"Identifier", e.getXref().getId()},
						{"Database", e.getXref().getDataSource().getFullName()},
						{"Description", Utils.oneOf (attributes.get("Description"))},
						{"Synonyms", Utils.oneOf (attributes.get("Synonyms"))},
						{"Chromosome", Utils.oneOf (attributes.get("Chromosome"))},
						{"Molecular Formula", Utils.oneOf (attributes.get("BrutoFormula"))},
						{"Direction", Utils.oneOf (attributes.get("Direction"))}
				};

				for (String[] row : table)
				{
					if (!(row[1] == null))
					{
						bpInfo.append ("<TR><TH align=\"left\" bgcolor=\"#F0F0F0\">");
						bpInfo.append (row[0]);
						bpInfo.append (":<TH align=\"left\">");
						bpInfo.append (row[1]);
					}
				}
				bpInfo.append ("</TABLE>");
				text += bpInfo.toString();
			}
			catch (IDMapperException ex)
			{
				text += "Exception occurred, see log for details</br>";
				Logger.log.error ("Error fetching backpage info", ex);
			}
			return text;
		}

//		@Override
//		public String getHtml(SwingEngine swe) {
//			// TODO Auto-generated method stub
//			return null;
//		}

	}

	/**Graphics
	 * A @{link BackpageHook} that adds a list of crossref links to
	 * the backpage panel.
	 */
	public static class BackpageXrefs implements BackpageHook
	{
		private final IDMapper gdb;

		public BackpageXrefs (IDMapper mapper)
		{
			gdb = mapper;
		}

		public String getHtml(PathwayElement e) {
			try
			{
				if(	e.getXref().getId() == null || 
					"".equals(e.getXref().getId()) ||
					e.getXref().getDataSource() == null	) {
					return "";
				}
				
				Set<Xref> crfs = gdb.mapID(e.getXref());
				crfs.add(e.getXref());
				if(crfs.size() == 0) return "";
				List<Xref> sortedRefs = new ArrayList<Xref>(crfs);
				Collections.sort(sortedRefs);
				StringBuilder crt = new StringBuilder("<br><br><hr><br><br><H1><font color=\"006699\">Cross references</font></H1><BR>");
				
				String db = "";
				crt.append("<table border=0>");
				for(Xref cr : sortedRefs) {
					String dbNew = (cr.getDataSource().getFullName() != null ? cr.getDataSource().getFullName() : cr.getDataSource().getSystemCode());
					if(!dbNew.equals(db)) {
						db = dbNew;
						crt.append("<TR></TR>");
						crt.append("<TR><TH border=1 align=\"left\" bgcolor=\"#F0F0F0\"><font size=\"4\"><b>" + db + "</b></font></TH></TR>");
					}
					String idtxt = cr.getId();
					String url = cr.getUrl();
					if(url != null && !url.equals(idtxt)) {
						url = url.replace("&", "&amp;"); // primitive HTML entity encoding. TODO: do it properly 
						idtxt = "<a href=\"" + url + "\">" + idtxt + "</a>";
					}
					crt.append("<TR><TH align=\"left\" style=\"border-left : 1\">" + idtxt + "</TH></TR>");
				}
			
				crt.append("</table>");
			
				return crt.toString();
			}
			catch (IDMapperException ex)
			{
				return "Exception occured while getting cross-references</br>\n"
					+ ex.getMessage() + "\n";
			}
		}
	}

	/**
	 * Register a BackpageHook with this text provider. Backpage fragments
	 * are generated in the order that the hooks were registered.
	 */
	public void addBackpageHook(BackpageHook hook)
	{
		hooks.add (hook);
	}

	private final List<BackpageHook> hooks = new ArrayList<BackpageHook>();

	public BackpageTextProvider()
	{
		initializeHeader();
	}

	/**
	 * generates html for a given PathwayElement. Combines the base
	 * header with fragments from all BackpageHooks into one html String.
	 */
	public String getBackpageHTML(PathwayElement e)
	{
		if (e == null) {
			return "<p>No pathway element is selected.</p>";
		} else if (e.getObjectType() != ObjectType.DATANODE && e.getObjectType() != ObjectType.LINE) {
			return "<p>Backpage is not available for this type of element.<BR>Only DataNodes or Interactions can have a backpage.</p>";
		} else if (e.getDataSource() == null || e.getXref().getId().equals("")) {
			return "<p>There is no annotation for this pathway element defined.</p>";
		}
		StringBuilder builder = new StringBuilder(backpagePanelHeader);
		for (BackpageHook h : hooks)
		{
			builder.append(h.getHtml(e));
		}
		builder.append ("</body></html>");
		return builder.toString();
	}

	/**
	 * Header file, containing style information
	 */
	final private static String HEADERFILE = "header.html";

	private String backpagePanelHeader;

	/**
	 * Reads the header of the HTML content displayed in the browser. This header is displayed in the
	 * file specified in the {@link HEADERFILE} field
	 */
	private void initializeHeader()
	{
		try {
			BufferedReader input = new BufferedReader(new InputStreamReader(
					Resources.getResourceURL(HEADERFILE).openStream()));
			String line;
			backpagePanelHeader = "";
			while((line = input.readLine()) != null) {
				backpagePanelHeader += line.trim();
			}
		} catch (Exception e) {
			Logger.log.error("Unable to read header file for backpage browser: " + e.getMessage(), e);
		}
	}

}
