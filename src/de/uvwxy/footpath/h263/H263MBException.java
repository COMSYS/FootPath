package de.uvwxy.footpath.h263;

public class H263MBException extends Exception {
	private static final long serialVersionUID = 2021110802013382697L;

	private String s = null;
	public H263MBException(String msg){
		this.s = msg;
	}
	public String toString() {
		return s;
	}
}
