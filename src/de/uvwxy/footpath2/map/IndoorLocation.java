package de.uvwxy.footpath2.map;

import android.location.Location;

public class IndoorLocation extends Location {
	private String name;
	private boolean isDoor = false;
	private boolean isInDoors = true;
	
	public IndoorLocation(String name, String provider) {
		super(provider);
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isDoor() {
		return isDoor;
	}
	
	public void setDoor(boolean isDoor) {
		this.isDoor = isDoor;
	}
	
	public boolean isInDoors() {
		return isInDoors;
	}
	
	public void setInDoors(boolean isInDoors) {
		this.isInDoors = isInDoors;
	}
	

}
