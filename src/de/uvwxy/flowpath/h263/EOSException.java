package de.uvwxy.flowpath.h263;

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
