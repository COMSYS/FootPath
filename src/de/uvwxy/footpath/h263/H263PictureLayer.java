package de.uvwxy.footpath.h263;

public class H263PictureLayer {
	public int hTemporalReference = -1; // Temporal Reference
	public boolean hSplitScreen = false;
	public boolean hDocumentCamera = false;
	public boolean hFullPictureFreezeRelease = false;
	public int hSourceFormat = -1; // enum if needed
	
	public boolean hExtendedPTYPE = false; // if true PLUSTYPE is present
	
	// To determine if OPTYPE fields are present, or only MPTYPE
	public int hUFEP = -1;
	
	// Optional PlustTYPE fields
	public boolean hOptionalPTYPE = false;
	public boolean hCustomPCF = false;
	public boolean hAdvanceINTRACoding = false;
	public boolean hDeblockingFilter = false;
	public boolean hSliceStructured = false;
	public boolean hReferencePicureSelection = false;
	public boolean hIndependentSegmentDecoding = false;
	public boolean hAlternativeINTERVLC = false;
	public boolean hModifiedQuantization = false;
	
	// Mandatory PlustTYPE fields
	public boolean hReferencePictureResampling = false;
	public boolean hReducedResolutionUpdate = false;
	public boolean hRoundingType = false;
	
	public H263PCT hPictureCodingType = H263PCT.Undefined;
	public boolean hUnrestrictedMotionVector = false;
	public boolean hSyntaxArithmeticCoding = false;
	public boolean hAdvancedPrediction = false;
	public boolean hPBFrames = false;
	
	public boolean hCPM = false; // Continuous Presence Multipoint and Video Multiplex mode
	public int hPSBI = -1;
	
//	public int hCustomPictureFormat = -1; // CPFMT is divided as below
	public int hCPFMTPixelAspectRatio = -1;
	public int hCPFMTPictureWidthIndication = -1;
	public int hCPFMTPictureHeightIndication = -1;
	
//	public int hExtendedPixelAspectRatio = -1; // EPAR is divided as below
	public int hEPARWidth = -1;
	public int hEPARHeight = -1;
	
//	public int hCustomPictureClockFrequencyCode = -1;
	public boolean hCPCFClockConversion = false;
	public int hCPCFClockDivisor = -1;
	
	public int hExtendedTemporalReference = -1;
	public int hUnlimitedUnrestrictedMotionVectorsIndicator = -1;
	public int hSliceStructuredSubmodeBits = -1;
	public int hEnhancementLayerNumber = -1;
	public int hReferenceLayerNumber = -1;
	public int hReferencePictureSelectionModeFlags = -1;
	public boolean hTemporalReferenceForPredictionIndication = false;
	public int hTemporalReferenceForPrediction = -1;
	public int hBackChannelMessageIndication = -1;
	public int hBackChannelMessage = -1; // TODO length unknown if above = 1 else 0
	public int hReferencePictureResamplingParameters = -1;
	public int hQuantizerInformation = -1;
	
	// CPM; PSBI, see above
	
	public int hTemporalReferenceForBPicturesInPBFrames = -1;
	public int hQuantizationInformationForBPicturesInPBFrames = -1;
	public boolean hExtraInsertionInformation = false;
	// EII == true indicates the following two fields
	public int hSupplementalEnhancmentInformation = -1;
	
	public float[][][][] hMVDs = null;
	
	
}
