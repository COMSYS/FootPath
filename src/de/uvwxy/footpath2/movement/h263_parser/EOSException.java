package de.uvwxy.footpath2.movement.h263_parser;

public class EOSException extends Exception {
	private static final long serialVersionUID = 3117508653196414443L;
	
	private String s;
	public EOSException (String s){
		this.s = s;
	}
	
	public String toString(){
		return s;
	}
}
