package de.uvwxy.footpath2.log;

/**
 * To maintain a consistent logging functionality this interface should be implemented and the implementing classes
 * should behave as documented here.
 * 
 * 
 * @author Paul Smith
 * 
 */
public interface Exporter {
	/**
	 * When calling this function an class is supposed to export all its logged data. For example this could be called
	 * at the end of the objects life cycle, when all the gathered information should be saved.
	 * 
	 * @param path
	 * @return number of entries/lines exported
	 */
	public int export_allData(String path);

	/**
	 * When calling this function a class is supposed to export all its logged data since the last time this function
	 * has been called. This can be called periodically to prevent data loss during an experiment.
	 * 
	 * @param path
	 * @return number of entries/lines cleared
	 */
	public int export_recentData(String path);
	
	/**
	 * This function can be called after exportRecentData(...) to prevent a class from consuming too much memory.
	 * @return number of entries/lines cleared
	 */
	public int export_clearData();
	
	/**
	 * An approximaiton on how much memory the implementing class has stored so far of exportable data.
	 * @return number of bytes consumed by implementing class
	 */
	public int export_consumedBytes();
}
