package de.uvwxy.footpath2.movement.h263;

/**
 * This class is used to provide a standard FlowPath configuration.
 * Currently no runtime changes allowed, because too much is set from these
 * values during object creation.
 * @author paul
 *
 */
public class FlowPathConfig {
	// Stream parsing
	// Format Video Resolution
	// SQCIF 128 × 96 @ 10 @ 30 (not 20)
	// QCIF 176 × 144 30,10
	// SCIF 256 x 192
	// SIF(525) 352 x 240
	// CIF/SIF(625) 352 × 288
	// 4SIF(525) 704 x 480
	// 4CIF/4SIF(625) 704 × 576
	// 16CIF 1408 × 1152
	// DCIF 528 × 384
	public static final int PIC_SIZE_WIDTH = 320;
	public static final int PIC_SIZE_HEIGHT = 240;
	public static final int PIC_FPS = 30;
	public static int port = 2000;
	
	
}
