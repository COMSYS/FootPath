package de.uvwxy.footpath2.tools;

import java.io.File;
import java.io.FilenameFilter;

public class FileNameFilterFromStringArrayWithDirectores implements FilenameFilter {
	private String[] postfixes = null;

	/**
	 * Create a new FileNameFilter from a String array. Please supply a list of Strings without leading ".", i.e.
	 * {"osm","xml",...}. This is inserted during automatically during filename comparison. Each postfix is converted to
	 * lower case and compared with a filename converted to lower case.
	 * 
	 * @param postfixes
	 *            array of possible file endings
	 */
	public FileNameFilterFromStringArrayWithDirectores(String[] postfixes) {
		this.postfixes = postfixes;
		// Convert each postfix to lower case
		for (String postfix : this.postfixes) {
			postfix = postfix.toLowerCase();
		}
	}

	@Override
	public boolean accept(File dir, String filename) {
		filename = filename.toLowerCase();
		File f = new File(dir + "/" + filename);
		for (String postfix : postfixes) {
			if (filename.endsWith("." + postfix) || f.isDirectory()) {
				return true;
			}
		}
		return false;
	}
}
