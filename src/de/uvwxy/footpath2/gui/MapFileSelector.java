package de.uvwxy.footpath2.gui;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import de.uvwxy.footpath.R;

/**
 * Author: paul Date: May 11, 2012 7:10:40 PM
 */
public class MapFileSelector extends Activity {
	// ####################################################################
	// Variables & Handles
	// ####################################################################
	private ListView lvFileSelector = null;
	private FileListAdapter lvFileSelectorAdapter = null;
	private TextView tvCurrentDirectory = null;
	private TextView tvSelectedFile = null;
	private Button btnLoad = null;
	private File selectedFile = null;

	private String initPath = null;
	private String[] filter = null;

	// ####################################################################
	// Listener, Callbacks
	// ####################################################################

	// ####################################################################
	// Application Life Cycle
	// ####################################################################
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map_file_selector);
		initPath = getIntent().getStringExtra("INIT_PATH");
		String buf = getIntent().getStringExtra("FILTER");
		if (buf != null) {
			filter = buf.split(",");
			for (String f : filter) {
				Log.i("FOOTPATH", "Filtering \"" + f + "\"");
			}
		} else {
			Log.i("FOOTPATH", "Using no filtering");
		}

		lvFileSelector = (ListView) findViewById(R.id.lvFileSelector);

		tvSelectedFile = (TextView) findViewById(R.id.tvSelectedFile);
		tvCurrentDirectory = (TextView) findViewById(R.id.tvCurrentDirectory);
		btnLoad = (Button) findViewById(R.id.btnLoad);
		btnLoad.setEnabled(false);
	}

	@Override
	public void onStart() {
		super.onStart();
		// -> onResume()
	}

	@Override
	public void onResume() {
		super.onResume();
		// Activity came from background
		lvFileSelector.setEmptyView(findViewById(R.id.rlFileSelectorLine));

		if (initPath != null) {
			lvFileSelectorAdapter = new FileListAdapter(this,
					new File(initPath));
		} else {
			lvFileSelectorAdapter = new FileListAdapter(this, new File(
					Environment.getExternalStorageDirectory() + "/"));
		}
		lvFileSelectorAdapter.notifyDataSetChanged();
		lvFileSelector.setAdapter(lvFileSelectorAdapter);
		lvFileSelector.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				File clicked = lvFileSelectorAdapter.getItem(position);
				if (clicked.isDirectory()) {
					lvFileSelectorAdapter.goToDir(clicked);
					lvFileSelectorAdapter.notifyDataSetChanged();
				} else {
					selectedFile = clicked;
					tvSelectedFile.setText("Selected File:\n"
							+ clicked.getName());
					btnLoad.setEnabled(true);
				}

			}
		});
	}

	@Override
	public void onPause() {
		super.onPause();
		// Reason: Another Activity comes in fron of this Activity
		// May hapen: Other applications may need memory -> Process is killed!
		// -> onCreate

	}

	@Override
	public void onStop() {
		super.onStop();
		// Reason: The Activity is no longer visible
		// May happen: Other applications may need memory -> Process is killed!
		// -> onCreate
		// May happen: The Activity comes to the foreground -> onRestart()

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// Bye Bye!

	}

	// ####################################################################
	// Functions & Methods
	// ####################################################################
	public void goFolderUp(View v) {
		lvFileSelectorAdapter.goDirUp();
		lvFileSelectorAdapter.notifyDataSetChanged();
	}

	public void loadFile(View v) {
		Intent resultIntent = new Intent();
		resultIntent.putExtra("SELECTED_FILE", selectedFile.getAbsolutePath());
		setResult(Activity.RESULT_OK, resultIntent);
		finish();
	}

	private class FileListAdapter extends BaseAdapter {
		private Context context;
		private LayoutInflater inflater;
		private File dir;
		private LinkedList<File> files = new LinkedList<File>();

		private ImageView ivIcon = null;
		private TextView lblFileName = null;
		private TextView lblFileInfo = null;

		public FileListAdapter(Context context, File dir) {
			this.dir = dir;
			this.context = context;
			inflater = LayoutInflater.from(context);
			goToDir(dir);
		}

		public synchronized void goDirUp() {
			if (!dir.equals(Environment.getExternalStorageDirectory()))
				goToDir(new File(dir.getParent()));
		}

		public synchronized void goToDir(File dir) {
			this.dir = dir;
			files = new LinkedList<File>(Arrays.asList(dir.listFiles()));
			filter();
			sort();
			tvCurrentDirectory.setText("Current directory:\n"
					+ dir.getAbsolutePath() + "/");

		}

		private synchronized void filter() {
			Log.i("FOOTPATH", "size: " + files.size());
			LinkedList<File> toRemove = new LinkedList<File>();

			for (File f : files) {
				if (f.isFile()) {
					String buf = f.getName();
					// note: minimum accepted filename: a.bc
					if (buf.length() >= 4) {
						String[] s = buf.split("\\.");
						if (s.length != 0) {
							String ending = s[s.length - 1];
							boolean keep = false;
							for (String e : filter) {
								if (ending.equals(e)) {
									keep = true;
								}
							}
							if (!keep)
								toRemove.add(f);
						}
					}
				}
			}

			for (File f : toRemove) {
				files.remove(f);
			}
		}

		@Override
		public synchronized int getCount() {
			if (files != null) {
				return files.size();
			}
			return 0;
		}

		@Override
		public synchronized File getItem(int i) {
			if (files != null) {
				return files.get(i);
			}
			return null;
		}

		@Override
		public synchronized long getItemId(int position) {
			// TODO: what is this?
			return 0;
		}

		private synchronized void sort() {
			String sortMode = "Windows";
			Comparator<File> alphabetical_sort = new Comparator<File>() {
				@Override
				public int compare(File object1, File object2) {
					return object1.getName().compareToIgnoreCase(
							object2.getName());
				}
			};
			Comparator<File> file_dir_sort = new Comparator<File>() {
				@Override
				public int compare(File object1, File object2) {
					if (object1.isDirectory() == object2.isDirectory()) {
						return 0;
					} else if (object1.isFile()) {
						return 1;
					}
					return -1;
				}
			};
			Comparator<File> date_sort = new Comparator<File>() {
				@Override
				public int compare(File object1, File object2) {
					if (object1.lastModified() == object2.lastModified()) {
						return 0;
					} else if (object1.lastModified() < object2.lastModified()) {
						return 1;
					}
					return -1;
				}
			};
			Comparator<File> size_sort = new Comparator<File>() {
				@Override
				public int compare(File object1, File object2) {
					if (object1.length() == object2.length()) {
						return 0;
					} else if (object1.length() < object2.length()) {
						return 1;
					}
					return -1;
				}
			};
			if (sortMode.equals("Linux")) {
				//
			} else if (sortMode.equals("Windows")) {
				Collections.sort(files, alphabetical_sort);
				Collections.sort(files, file_dir_sort);
			} else if (sortMode.equals("Alphabetical")) {
				Collections.sort(files, alphabetical_sort);
			} else if (sortMode.equals("Date")) {
				Collections.sort(files, date_sort);
			} else if (sortMode.equals("Size")) {
				Collections.sort(files, size_sort);
			}
		}

		@Override
		public synchronized View getView(int position, View convertView,
				ViewGroup parent) {
			View entry = inflater.inflate(R.layout.map_file_selector_entry,
					parent, false);

			ivIcon = (ImageView) entry.findViewById(R.id.ivIcon);
			lblFileInfo = (TextView) entry.findViewById(R.id.lblFileInfo);
			lblFileName = (TextView) entry.findViewById(R.id.lblFileName);

			if (lblFileName != null)
				lblFileName.setText(files.get(position).getName());
			if (lblFileInfo != null)
				lblFileInfo.setText("TODO: more info about a map?");

			if (ivIcon != null)
				if (files.get(position).isDirectory()) {
					ivIcon.setImageResource(R.drawable.icon_folder);
				} else {
					ivIcon.setImageResource(R.drawable.icon);
				}

			return entry;
		}

	}
}
