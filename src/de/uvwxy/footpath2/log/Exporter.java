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
	 * When calling this function a class is supposed to export all its logged data. For example this could be called
	 * at the end of the objects life cycle, when all the gathered information should be saved.
	 * 
	 * ### Implementation note:
	 * ### remember up to where you have exported your data.  -> last_i
	 * 
	 * @param path
	 * @return number of entries/lines exported
	 */
	public int export_allData(String filename);

	/**
	 * When calling this function a class is supposed to export all its logged data since the last time this function
	 * has been called. This can be called periodically to prevent data loss during an experiment.
	 * 
	 * ### Implementation note:
	 * ### remember up to where you have exported your data.  -> last_i
	 * ### i.e.: export form last_i to size(), then set last_i = size()
	 * 
	 * @param path
	 * @return number of entries/lines exported
	 */
	public int export_recentData(String filename);
	
	/**
	 * Call this function to free all data in memory.
	 * 
	 * ### Implementation note:
	 * ### -> last_i = 0
	 * 
	 * @return number of entries/lines deleted
	 */
	public int export_clearAllData();
	
	/**
	 * Call this function to delete all data that has been exported so far.
	 * 
	 * ### Implementation note:
	 * ### delete entries upto last_i
	 * ### -> last_i = 0
	 * 
	 * @param numEntries
	 * @return number of entries/lines deleted
	 */
	public int export_clearRecentData();

	
	/**
	 * An approximation on how much memory the implementing class has stored so far of exportable data.
	 * 
	 * 	 * @return number of bytes consumed by implementing class
	 */
	public int export_consumedBytes();
}
