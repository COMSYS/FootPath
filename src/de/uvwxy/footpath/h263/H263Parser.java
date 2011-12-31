package de.uvwxy.footpath.h263;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import android.util.Log;

/**
 * Warning: this is highly unstable and undocumented code ;)
 * asdf you know!
 * @author Paul Smith
 *
 */
public class H263Parser {
	private FileInputStream fis = null;
	private int lastFisPtr = 0;
	private int fisPtr = 0; // points to the next unread byte
	private int bitPtr = 7; // points to the current bit of the last byte read
	private int currentByte = -1;

	private int numIframes = 0;
	private int numPframes = 0;
	private int pictureBoxCount = 0;
	private int groupOfBlocksCount = 0;
	private int macroBlockCount = 0;

	// k is set at the beginning of the stream to the correct size
	// as we assume we are working with a video height between 404 and 800
	// we already set it to 2, see H.263(01/2005) page 10 (19 in pdf)
	int k = 2;
	
	// resolution from nexus s is
	int width = 640;
	int height = 480;
	int blockWidth = 0;
	int blockHeight = 0;

	boolean parsePs = false; // parse picture layer
	boolean parseGOBs = false; // parse Group of Blocks layer
	boolean parseMBs = false; // parse Macro Block layer
	boolean parseBs = false; // parse Block layer
	
	boolean blocking = false;
		
	private boolean breakOnBitErrors = true;
	private boolean noGSCMode = true;
	
	// I herby assume that there will be no frame smaller than 128 bytes
	// this is supposed to stop the parser wandering off into areas at the
	// end of the file where PSC are detected but no pictures are to befound
	private static int MINIMUM_BYTES_BETWEEN_PICTURES = 128;
	/**
	 * TODO: write this.
	 * 
	 * @param fis
	 * @param ptrOffset
	 */
	public H263Parser(FileInputStream fis, int ptrOffset, boolean parsePs, boolean parseGOBs, boolean parseMBs, boolean parseBs, boolean blocking) {
		this.fis = fis;
		this.fisPtr = ptrOffset;
		this.parsePs = parsePs;
		this.parseGOBs = parseGOBs;
		this.parseMBs = parseMBs;
		this.parseBs = parseBs;
		this.blocking = blocking;
	}

	public void parseH263() throws IOException {
		DebugOut.debug_vv("Parsing H263 video data beginning from " + fisPtr);

		// int numFrames = 512;
		// for (int i = 0; i < numFrames; i++) {
		while (true) {
			try {
				if(parsePs)
					decodePicture();
			} catch (EOSException e) {
				DebugOut.debug_vv("end of stream occured, or is blocking");
				break;
			}
		}

		DebugOut.debug_vv("Parsing of H263 video data ended at " + fisPtr);
		DebugOut.debug_v("Found " + numIframes + " I-Frames, " + numPframes + " P-Frames");
	}
	
	public double[][][] parseH263Frame() throws IOException {
		DebugOut.debug_vv("Parsing H263 video data beginning from " + fisPtr);

		try {
			return decodePicture();
		} catch (EOSException e) {
			DebugOut.debug_vv("end of stream occured, or is blocking");
		}
		
		return null;
	}
	
	public void skipH263Frame() throws IOException, EOSException {
		checkForPictureStartCode();
	}

	private double[][][] decodePicture() throws IOException, EOSException {
		long timeMillis = System.currentTimeMillis();
		H263PictureLayer p = new H263PictureLayer();

		checkForPictureStartCode();
		
		DebugOut.debug_v("Found PSC @ " + fisPtr + "/" + bitPtr + " diff: " + (fisPtr - lastFisPtr) +  " bytes");
		
		// ugly hack to prevent crashing at end of file
		if ((fisPtr - lastFisPtr) < MINIMUM_BYTES_BETWEEN_PICTURES 
				&& pictureBoxCount > 2 ){
			DebugOut.debug_v("We assume " + (fisPtr - lastFisPtr) +  " bytes are not enough to be a picture, skipping...");
			lastFisPtr = fisPtr;
			return null;
		}
		
		lastFisPtr = fisPtr;
		
		p.hTemporalReference = readBits(8);

		int hPTYPE = readBits(5);

		// Integrity Bits
		DebugOut.debug_vvv("PTYPE bit_1 is 1 " + b(hPTYPE, 4));
		DebugOut.debug_vvv("PTYPE bit_2 is 0 " + !b(hPTYPE, 3));
		if (!(b(hPTYPE, 4) && !b(hPTYPE, 3))) {
			// break here if something goes wrong!
			if (breakOnBitErrors)
				return null;
		}

		p.hSplitScreen = b(hPTYPE, 2);
		p.hDocumentCamera = b(hPTYPE, 1);
		p.hFullPictureFreezeRelease = b(hPTYPE, 0);

		p.hSourceFormat = readBits(3);

		// Check if last 3 bits equal 111
		if (b(p.hSourceFormat, 2) && b(p.hSourceFormat, 1)
				&& b(p.hSourceFormat, 0)) {
			DebugOut.debug_vv("Detected PlusPTYPE bits");
			p.hExtendedPTYPE = true;
			p.hUFEP = readBits(3);

			// Check if UFEP equals 001 (Extended PTYPE)
			if (!b(p.hUFEP, 2) && !b(p.hUFEP, 1) && b(p.hUFEP, 0)) {
				// When set to "001", it indicates that all extended PTYPE
				// fields are included in the current picture header. If the
				// picture type is INTRA or EI, this field shall be set to
				// "001".
				DebugOut.debug_vv("Detected PlusPTYPE.OPPTYPE bits");
				p.hOptionalPTYPE = true;

				p.hSourceFormat = readBits(3);
				switch (p.hSourceFormat) {
				case 0:
					DebugOut.debug_vvv("Source Format reserved");
					break;
				case 1:
					DebugOut.debug_vvv("Source Format sub-QCIF");
					break;
				case 2:
					DebugOut.debug_vvv("Source Format QCIF");
					break;
				case 3:
					DebugOut.debug_vvv("Source Format CIF");
					break;
				case 4:
					DebugOut.debug_vvv("Source Format 4CIF");
					break;
				case 5:
					DebugOut.debug_vvv("Source Format 16CIF");
					break;
				case 6:
					DebugOut.debug_vvv("Source Format custom source format");
					break;
				case 7:
					DebugOut.debug_vvv("Source Format reserved");
					break;
				}
				int hOPPTYPE_footer = readBits(15);
				p.hCustomPCF = b(hOPPTYPE_footer, 14);
				p.hUnrestrictedMotionVector = b(hOPPTYPE_footer, 13);
				p.hSyntaxArithmeticCoding = b(hOPPTYPE_footer, 12);
				p.hAdvancedPrediction = b(hOPPTYPE_footer, 11);
				p.hAdvanceINTRACoding = b(hOPPTYPE_footer, 10);
				p.hDeblockingFilter = b(hOPPTYPE_footer, 9);
				p.hSliceStructured = b(hOPPTYPE_footer, 8);
				p.hReferencePicureSelection = b(hOPPTYPE_footer, 7);
				p.hIndependentSegmentDecoding = b(hOPPTYPE_footer, 6);
				p.hAlternativeINTERVLC = b(hOPPTYPE_footer, 5);
				p.hModifiedQuantization = b(hOPPTYPE_footer, 4);

//				DebugOut.debug_vvv("##### Last four bits are correct ( = 1000)  "
//						+ (b(hOPPTYPE_footer, 3) && !b(hOPPTYPE_footer, 2)
//								&& !b(hOPPTYPE_footer, 1) && !b(
//									hOPPTYPE_footer, 0)));

				if (!((b(hOPPTYPE_footer, 3) && !b(hOPPTYPE_footer, 2)
						&& !b(hOPPTYPE_footer, 1) && !b(hOPPTYPE_footer, 0)))) {
					if (breakOnBitErrors)
						return null;
				}

			} else {
				DebugOut.debug_vvv("No PlusPTYPE.OPPTYPE bits!!!");
				// When set to "000", it indicates that only those extended
				// PTYPE fields which need to be signalled in every picture
				// header (MPPTYPE) are included in the current picture header.
			} // check if Optionlal Part of PlusTYPE is present

			// Regardless of the value of UFEP, the following 9 bits are also
			// present in PLUSPTYPE: (Mandatory Part of PlusTYPE)

			int hMPPTYPE_header = readBits(3);

			switch (hMPPTYPE_header) {
			case 0:
//				DebugOut.debug_vv("Picture Type Code I-picture (INTRA)");
				numIframes++;
				p.hPictureCodingType = H263PCT.INTRA;
				break;
			case 1:
//				DebugOut.debug_vv("Picture Type Code P-picture (INTER) /!\\");
				numPframes++;
				p.hPictureCodingType = H263PCT.INTER;
				break;
			case 2:
//				DebugOut.debug_vv("Picture Type Code Improved PB-frame");
				p.hPictureCodingType = H263PCT.ImprovedPBFrame;
				break;
			case 3:
//				DebugOut.debug_vv("Picture Type Code B-picture");
				p.hPictureCodingType = H263PCT.BPicture;
				break;
			case 4:
//				DebugOut.debug_vv("Picture Type Code EI-picture");
				p.hPictureCodingType = H263PCT.EIPicture;
				break;
			case 5:
//				DebugOut.debug_vv("Picture Type Code EP-picture");
				p.hPictureCodingType = H263PCT.EPPicture;
				break;
			case 6:
//				DebugOut.debug_vv("Picture Type Code Reserved");
				p.hPictureCodingType = H263PCT.Reserved;
				break;
			case 7:
//				DebugOut.debug_vv("Picture Type Code Reserved");
				p.hPictureCodingType = H263PCT.Reserved;
				break;
			}

			int hMPPTYPE_footer = readBits(6);
			p.hReferencePictureResampling = b(hMPPTYPE_footer, 5);
			p.hReducedResolutionUpdate = b(hMPPTYPE_footer, 4);
			p.hRoundingType = b(hMPPTYPE_footer, 3);

//			DebugOut.debug_vvv("Reference Picture Resampling mode "
//					+ p.hReferencePictureResampling);
//			DebugOut.debug_vvv("Reduced-Resolution Update mode "
//					+ p.hReducedResolutionUpdate);
//			DebugOut.debug_vvv("Rounding Type " + p.hRoundingType);
//
//			DebugOut.debug_vvv("##### Last three bits are correct (= 001) "
//					+ (!b(hMPPTYPE_footer, 2) && !b(hMPPTYPE_footer, 1) && b(
//							hMPPTYPE_footer, 0)));

			if (!(!b(hMPPTYPE_footer, 2) && !b(hMPPTYPE_footer, 1) && b(
					hMPPTYPE_footer, 0))) {
//				DebugOut.debug_vv("ÖRKS");
				if (breakOnBitErrors)
					return null;
			}

		} else {
//			DebugOut.debug_vvv("Not Detected PlusPTYPE");
			// read bits 9 - 13
			int hPTYPE_noPLUSPTYPE = readBits(5);

			p.hPictureCodingType = (b(hPTYPE_noPLUSPTYPE, 4) ? H263PCT.INTER
					: H263PCT.INTRA);
			p.hUnrestrictedMotionVector = b(hPTYPE_noPLUSPTYPE, 3);
			p.hSyntaxArithmeticCoding = b(hPTYPE_noPLUSPTYPE, 2);
			p.hAdvancedPrediction = b(hPTYPE_noPLUSPTYPE, 1);
			p.hPBFrames = b(hPTYPE_noPLUSPTYPE, 0);

//			DebugOut.debug_vvv("## Picture Type Code: "
//					+ (b(hPTYPE_noPLUSPTYPE, 4) ? "P-picture (INTER)"
//							: "I-picture (INTRA)"));
//			DebugOut.debug_vvv("# Unrestricted Motion Vector Mode "
//					+ p.hUnrestrictedMotionVector);
//			DebugOut.debug_vvv("# Syntax-based Arithmetic Coding "
//					+ p.hSyntaxArithmeticCoding);
//			DebugOut.debug_vvv("# Advanced Prediction mode "
//					+ p.hAdvancedPrediction);
//			DebugOut.debug_vvv("# PB-frames mode " + p.hPBFrames);
		} // check if PLUSTYPE is present

		// TODO: DO WE NEE ANY FURTHER FIELDS FROM THIS LAYER? YES!!!
		// We need to know what CPM is.

		if (p.hExtendedPTYPE) {
			// "If PLUSPTYPE is present, then CPM follows immediately after
			// PLUSPTYPE in the picture header."
//			DebugOut.debug_vv("READING CPM AT FIRST LOCATION");
			p.hCPM = readBits(1) == 1;
			if (p.hCPM) {
				// "PSBI always follows immediately after CPM (if CPM = "1")"
				p.hPSBI = readBits(2);
			}
		}

		if (p.hUFEP == 1 && p.hSourceFormat == 6) { // 6 = "110" custom source
													// format

			// A fixed length codeword of 23 bits that is present only if the
			// use of a custom picture format is signaled in PLUSPTYPE and
			// UFEP is '001'

			// ######## Warning!!!!
			// I assume they mean Optional Custom PCF, "0" CIF PCF, "1" custom
			// PCF;
			// If UFEP is "001", then the following bits are present in
			// PLUSPTYPE:
			// Other possible candidate:
			// Source Format, "000" reserved, "001" sub-QCIF, "010" QCIF, "011"
			// CIF,
			// "100" 4CIF, "101" 16CIF, "110" custom source format <---

			// ######## This means we might fail badly here!!

			// Possible Solution to this:
			// p.hCustomPCF = is Custom Picture Clock Frequency

			// p.hCustomPictureFormat = readBits(23); is divided as below
			p.hCPFMTPixelAspectRatio = readBits(4);
			// Number of pixels per line = (PWI + 1) * 4
			p.hCPFMTPictureWidthIndication = readBits(9);
			int mustBeOne = readBits(1); // prevent start code emulation
//			DebugOut.debug_vvv("Must be True: " + (mustBeOne == 1));

			if (!(mustBeOne == 1)) {
				return null;
			}

			// Number of lines = PHI * 4
			p.hCPFMTPictureHeightIndication = readBits(9);
		}

		if (p.hSourceFormat == 6 && p.hCPFMTPixelAspectRatio == 0xf) {
			// A fixed length codeword of 16 bits that is present only if CPFMT
			// is present and extended PAR is indicated therein.

			// p.hExtendedPixelAspectRatio = readBits(16); // EPAR is divided as
			// below
			p.hEPARWidth = readBits(8);
			p.hEPARHeight = readBits(8);
		}

		if (p.hExtendedPTYPE && p.hUFEP == 1 && p.hCustomPCF) {
			// A fixed length codeword of 8 bits that is present only if
			// PLUSPTYPE is present and UFEP is 001 and a custom picture clock
			// frequency is signaled in PLUSPTYPE.

			// p.hCustomPictureClockFrequencyCode = readBits(8); is divided
			// below
			p.hCPCFClockConversion = readBits(1) == 1;
			p.hCPCFClockDivisor = readBits(7);
		}

		if (p.hCustomPCF) {
			// A fixed length codeword of 2 bits which is present only if a
			// custom picture clock frequency is in use (regardless of the
			// value of UFEP).

			p.hExtendedTemporalReference = readBits(2);
		}

		if (p.hUnrestrictedMotionVector && p.hUFEP == 1) {
			// A variable length codeword of 1 or 2 bits that is present only
			// if the optional Unrestricted Motion Vector mode is indicated in
			// PLUSPTYPE and UFEP is 001.

			int bit_1 = readBits(1);
			if (bit_1 == 0) {
				int bit_2 = readBits(1);
				if (bit_2 == 1) {
					// The motion vector range is not limited except by the
					// picture size.
					p.hUnlimitedUnrestrictedMotionVectorsIndicator = 2;
				} else {
					// Houston we have a problem.
				}
			} else {
				// The motion vector range is limited according to Tables D.1
				// and D.2.
				p.hUnlimitedUnrestrictedMotionVectorsIndicator = 1;
			}
		}

		if (p.hSliceStructured && p.hUFEP == 1) {
			// A fixed length codeword of 2 bits which is present only if the
			// optional Slice Structured mode (see Annex K) is indicated in
			// PLUSPTYPE and UFEP is 001.

			p.hSliceStructuredSubmodeBits = readBits(2);
		}

		if (p.hPictureCodingType == H263PCT.BPicture
				|| p.hPictureCodingType == H263PCT.EIPicture
				|| p.hPictureCodingType == H263PCT.EPPicture) {
			// A fixed length codeword of 4 bits which is present only if the
			// optional Temporal, SNR, and Spatial Scalability mode is in use
			// (regardless of the value of UFEP).

			// FROM ANNEX 0, i.e. B-, EI- and EP-Pictures

			p.hEnhancementLayerNumber = readBits(4);
		}

		if (p.hPictureCodingType == H263PCT.BPicture
				|| p.hPictureCodingType == H263PCT.EIPicture
				|| p.hPictureCodingType == H263PCT.EPPicture) {
			// A fixed length codeword of 4 bits which is present only if the
			// optional Temporal, SNR, and Spatial Scalability mode is in use
			// (regardless of the value of UFEP).

			// FROM ANNEX 0, i.e. B-, EI- and EP-Pictures

			p.hReferenceLayerNumber = readBits(4);
		}

		if (p.hReferencePicureSelection && p.hUFEP == 1) {
			// A fixed length codeword of 3 bits that is present only if the
			// Reference Picture Selection mode is in use and UFEP is 001.

			p.hReferencePictureSelectionModeFlags = readBits(3);
		}

		if (p.hReferencePicureSelection) {
			// A fixed length codeword of 1 bit that is present only if the
			// optional Reference Picture Selection mode is in use (regardless
			// of the value of UFEP).

			p.hTemporalReferenceForPredictionIndication = readBits(1) == 1;

		}

		if (p.hTemporalReferenceForPredictionIndication) {
			// When present (as indicated in TRPI), TRP indicates the Temporal
			// Reference which is used for prediction of the encoding, except
			// for in the case of B-pictures. For B-pictures, the picture having
			// the temporal reference TRP is used for the prediction in the
			// forward direction.

			p.hTemporalReferenceForPrediction = readBits(10);
		}

		if (p.hReferencePicureSelection) {
			// A variable length field of one or two bits that is present only
			// if the optional Reference Picture Selection mode is in use.

			int bit_1 = readBits(1);
			if (bit_1 == 0) {
				int bit_2 = readBits(1);
				if (bit_2 == 1) {
					// "01" indicates the absence or the end of the video back-
					// channel message field.

					p.hBackChannelMessageIndication = 2;
				} else {
					// Houston we have a problem.
//					DebugOut.debug_vv("Houston we have a problem.");
				}
			} else {
				// When set to "1", this signals the presence of the following
				// optional video Back-Channel Message (BCM) field.

				p.hBackChannelMessageIndication = 1;
			}
		}

		if (p.hBackChannelMessageIndication == 1) {
			// TODO: READ BCM
			// Let's hope this is not present
//			DebugOut.debug_vv("mega shit crap (hBackChannelMessageIndication)");
		}

		if (p.hReferencePictureResampling) {
			// A variable length field that is present only if the optional
			// Reference Picture Resampling mode bit is set in PLUSPTYPE.

			// Let's hope this is not present
//			DebugOut.debug_vv("mega shit crap (hReferencePictureResampling)");
		}

		p.hQuantizerInformation = readBits(5);

		if (!p.hExtendedPTYPE) {
			// [...] but follows PQUANT in the picture header if PLUSPTYPE is
			// not present.
//			DebugOut.debug_vv("READING CPM!!!!!!");
			p.hCPM = readBits(1) == 1;
			if (p.hCPM) {
				// "PSBI always follows immediately after CPM (if CPM = "1")"
				p.hPSBI = readBits(2);
			}
		}

		if (p.hPBFrames || p.hPictureCodingType == H263PCT.ImprovedPBFrame) {
			// TRB is present if PTYPE or PLUSPTYPE indicates "PB-frame" or
			// "Improved PB-frame"

			// It is 3 bits long for standard CIF picture clock frequency and
			// is extended to 5 bits when a custom picture clock frequency is
			// in use.

			if (p.hCustomPCF) {
				// custom
				p.hTemporalReferenceForBPicturesInPBFrames = readBits(5);
			} else {
				// standard
				p.hTemporalReferenceForBPicturesInPBFrames = readBits(3);
			}

		}

		if (p.hPBFrames || p.hPictureCodingType == H263PCT.ImprovedPBFrame) {
			// DBQUANT is present if PTYPE or PLUSPTYPE indicates "PB-frame"
			// or "Improved PB-frame"

			p.hQuantizationInformationForBPicturesInPBFrames = readBits(2);
		}

		p.hExtraInsertionInformation = readBits(1) == 1;

		if (p.hExtraInsertionInformation) {
//			DebugOut.debug_vv("Extra Insertion Information not supported");
			// A codeword of variable length consisting of less than 8
			// zero-bits. Encoders may insert this codeword directly before an
			// EOS codeword. Encoders shall insert this codeword as necessary to
			// attain mandatory byte alignment directly before an EOSBS
			// codeword. If ESTUF is present, the last bit of ESTUF shall be
			// the last (least significant) bit of a byte, so that the start of
			// the EOS or EOSBS codeword is byte aligned. Decoders shall be
			// designed to discard ESTUF. See Annex C for a description of
			// EOSBS and its use.

		}

		// Remove Stuffing here for byte alignment?
		
		// OUTPUT CRAP FROM ABOVE:

//		DebugOut.debug_vvv("Temporal Reference = " + p.hTemporalReference);
//
//		DebugOut.debug_vvv("Split Screen mode " + p.hSplitScreen);
//		DebugOut.debug_vvv("Document Camera mode  " + p.hDocumentCamera);
//		DebugOut.debug_vvv("Full Picture Freeze mode "
//				+ p.hFullPictureFreezeRelease);
//
//		DebugOut.debug_vvv("Custom PCF " + p.hCustomPCF);
//		DebugOut.debug_vvv("# Unrestricted Motion Vector mode "
//				+ p.hUnrestrictedMotionVector);
//		DebugOut.debug_vvv("# Syntax-based Arithmetic Coding mode "
//				+ p.hSyntaxArithmeticCoding);
//		DebugOut.debug_vvv("# Advanced Prediction mode " + p.hAdvancedPrediction);
//		DebugOut.debug_vvv("# Advanced INTRA Coding mode " + p.hAdvanceINTRACoding);
//		DebugOut.debug_vvv("Deblocking Filter mode " + p.hDeblockingFilter);
//		DebugOut.debug_vvv("Slice Structured mode " + p.hSliceStructured);
//		DebugOut.debug_vvv("Reference Picture Selection mode "
//				+ p.hReferencePicureSelection);
//		DebugOut.debug_vvv("Independent Segment Decoding mode "
//				+ p.hIndependentSegmentDecoding);
//		DebugOut.debug_vvv("Alternative INTER VLC mode " + p.hAlternativeINTERVLC);
//		DebugOut.debug_vvv("Modified Quantization mode " + p.hModifiedQuantization);

		if (p.hSourceFormat == 6) {
			width = (p.hCPFMTPictureWidthIndication + 1) * 4;
			height = (p.hCPFMTPictureHeightIndication * 4);

//			DebugOut.debug_vv("Picutre dimensions: " + width + "x" + height);
		}
		
		// only decode P frames (INTER). I Frames have no MVD!
		if (parseGOBs && p.hPictureCodingType == H263PCT.INTER){
			if(noGSCMode ){
				// directly parse all macro blocks
				blockWidth = (width+15)/16;
				blockHeight = (height+15)/16;
				int numOfMBs = (blockWidth * blockHeight);
				
//				DebugOut.debug_vv("Decoding " + numOfMBs  + " macroblocks");
				// set up some space for mvds
				// vertical + horizontal
				// [2] because of value and predictor
				p.hMVDs = new double[blockWidth][blockHeight][2][2];
				for (int y = 0; y < blockHeight; y++) {
					for (int x = 0; x < blockWidth; x++){
						decodeMacroBlock(p, x,y);
					}
					
				}
			} else {
				// find start code sequence and then decode macro block
				decodeGOBS(p);
			}
		} else {
//			DebugOut.debug_vv("Deeper parsing disabled or not P-Frame");
		}
		
		pictureBoxCount++;
//		DebugOut.debug_vv("Picture count: " + pictureBoxCount);
//		DebugOut.debug_vvv("GOB count: " + groupOfBlocksCount);
		// DebugOut.debug_v("Macroblock count (TODO): " + macroBlockCount);
//		DebugOut.debug_v("########################################");
		
		if (p.hMVDs != null){
			
			//     | B | C |
			//   ----------
			//   A | X |   |
			// 
			
			double[][][] mvs = new double[blockWidth][blockHeight][2];
			double[] empty = { 0.0, 0.0 };
			double[] mvA, mvB, mvC;
			double predictorX;
			double predictorY;
			double[] horizDiffs;
			double[] vertDiffs;
			double tempX;
			double tempY;
			for (int y = 0; y < blockHeight; y++) {
				String buf = "";
				for (int x = 0; x < blockWidth; x++) {

					// setting up candidates

					if (x == 0) {
						// fix left side of screen
						mvA = empty;
					} else {
						mvA = mvs[x - 1][y];
					}
					if (y == 0) {
						// fix top side of screen
						mvB = mvA;
						mvC = mvA;
					} else {
						mvB = mvs[x][y - 1];
						// fix mvC on right side of screen
						if (x < blockWidth - 1) {
							mvC = mvs[x + 1][y - 1];
						} else {
							mvC = empty;
						}
					}

					predictorX = mvMedian(false, mvA, mvB, mvC);
					predictorY = mvMedian(true, mvA, mvB, mvC);
					horizDiffs = p.hMVDs[x][y][0];
					vertDiffs = p.hMVDs[x][y][1];

					tempX = horizDiffs[0] + predictorX;

					// [-16,15.5] range!
					if (tempX >= -16.0 && tempX <= 15.5) {
						// tada
					} else {
						tempX = horizDiffs[1] + predictorX;
					}

					tempY = vertDiffs[0] + predictorY;
					if (tempY >= -16.0 && tempY <= 15.5) {
						// tada
					} else {
						tempY = vertDiffs[1] + predictorY;
					}

					double[] mv = { tempX, tempY };
					mvs[x][y] = mv;

					// print MVs:
//					if (x == 0) {
//						buf += "" + mv[0] + "/" + mv[1];
//					} else {
//						buf += ", " + mv[0] + "/" + mv[1];
//					}
					// print differences, deprecated
					// buf += ", " + p.hMVDs[x][y][0][0] + "/" +
					// p.hMVDs[x][y][0][1] +
					// "|" + p.hMVDs[x][y][1][0] +"/" + p.hMVDs[x][y][1][1];
				}
//				DebugOut.debug_v(buf);
			}
//			DebugOut.debug_v("########################################");
			return mvs;
		} else {
			return null;
		}
	}
	
	private double mvMedian(boolean vertical, double[] mvA, double[] mvB,
			double[] mvC) {
		double a;
		double b;
		double c;
		if (vertical) {
			// return vertical component [1]
			a = mvA[1];
			b = mvB[1];
			c = mvC[1];
		} else {
			// return horizontal component [0]
			a = mvA[0];
			b = mvB[0];
			c = mvC[0];
		}
		
		double[] temp = {a,b,c};
		
		Arrays.sort(temp);
		
		return temp[1];
	}
	
	private void decodeGOBS(H263PictureLayer p) throws IOException, EOSException{
		// A Group Of Blocks (GOB) comprises of up to k * 16 lines, where k
		// depends on the number of lines in the picture format and depends
		// on whether the optional Reduced-Resolution Update mode is in use
		// (see Annex Q).
		// int k = 1; <- k is a field of this class to used for frames
		// w/o extended info on picture size

		if (height >= 4 && height <= 400) {
			k = 1;
			if (p.hReducedResolutionUpdate) {
				k = 2;
			}
		} else if (height >= 401 && height <= 800) {
			k = 2;
		} else if (height >= 801 && height <= 1152) {
			k = 4;
		}
		
//		DebugOut.debug_vv("gobs per picture: " + (height / 16) / k);

		// note: Group Number between 0 and (480/16)/k

		// For the GOB with number 0, the GOB header including GSTUF,
		// GBSC, GN, GSBI, GFID and GQUANT is empty; as group number 0
		// is used in the PSC.
		// --> We currently do not detect block 0

		// TODO: Detect Block 0 here!

		// TODO: For all other GOBs, the GOB header may be empty,
		// --> This is currently not detected :(

		// --> parse other blocks (1-17) here
		for (int i = 1; i < (height / 16) / k; i++) {
			// Jump to GOB Start Code (works only for GOBS n > 0)
			checkForGOBStartCode();
			groupOfBlocksCount++;
			
			// Group Number
			int hGN = readBits(5);

			// GOB Sub-Bitstream Indicator
			int hGSBI = -1;
			if (p.hCPM) {
				hGSBI = readBits(2);
			}

			int hGFID = readBits(2);
			int hGQUANT = readBits(5);
			DebugOut.debug_vvv("Group Frame ID " + hGFID);

			// COD is only present in pictures that are not of type "INTRA",
			// for each macroblock in these pictures
			// i.e. we are skipping I frames, or P frames <--- ???
			if (!(p.hPictureCodingType == H263PCT.INTRA)) {
//						dumpNextByteToOut();
//						decodeMacroBlock(p,hGN);
//						decodeMacroBlock(p,hGN);
//						decodeMacroBlock(p,hGN);
//						decodeMacroBlock(p,hGN);
//						decodeMacroBlock(p,hGN);
//						decodeMacroBlock(p,hGN);
//						decodeMacroBlock(p,hGN);
//						decodeMacroBlock(p,hGN);
//						decodeMacroBlock(p,hGN);
			}

		}

		
	}
	
	
	private void decodeMacroBlock(H263PictureLayer p, int x, int y) throws IOException{
		boolean hMCOD = readBits(1) == 1; // false = coded
		double[] empty = {0,0};
		int hmMCBPC[];
		int hmCBPY[];
		int hMDQUANT;
		double mvdHorizontal[];
		double mvdVertical[];
		
		if (hMCOD) {
//			DebugOut.debug_vv("("
//					+ x + "/" + y
//					+ ") INTER macroblock /!\\ MVDS in this macroblock are all zero");
			
			p.hMVDs[x][y][0] = empty;
			p.hMVDs[x][y][1] = empty;
			
			// no further information in this macro block
			// motion vector for the whole block equal to zero
			// no coefficient data
			// LUMINANCE 0 BLOCK
//			decodeBlockLayer(true, false);
				
			// LUMINANCE 1 BLOCK			
//			decodeBlockLayer(true, false);

			// LUMINANCE 2 BLOCK
//			decodeBlockLayer(true, false);

			// LUMINANCE 3 BLOCK
//			decodeBlockLayer(true, false);
			
			// COLOR DIFF 0 BLOCK
//			decodeBlockLayer(true, false);
			
			// COLOR DIFF 1 BLOCK
//			decodeBlockLayer(true, false);
						
		} else {
			// coded macroblock
//			DebugOut.debug_vv("(" + x + "/" + y + ") Coded macroblock @ " + fisPtr + "/" + bitPtr);

			// only decode normal pictures

			// TODO: parse MCBPC (always present, variable length)
			// use Table 8
			hmMCBPC = readMCBPC4PFrames();
//			DebugOut.debug_vv("(" + x + "/" + y + ") Read MCBPC @ " + fisPtr + "/" + bitPtr);

			if(hmMCBPC!=null){
//				DebugOut.debug_vv("Macroblock type " + hmMCBPC[0] + " CBPC_0 = " + hmMCBPC[1] + " CBPC_1 = " + hmMCBPC[2]);
			
			} else {
//				DebugOut.debug_vv("MCBPC decoding failed!!!!!!!!!!!!!!!!!!!!!!!!!! /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\");
			}
			
			// TODO: parse CBPY present if MCBPC is not stuffing
			// Coded Block Pattern for luminance (CBPY) (Variable
			// length)
			hmCBPY = null;
			if (hmMCBPC != null && hmMCBPC[0] != -1){ // stuffing check
				hmCBPY = readCBPY();
//				DebugOut.debug_vv("(" + x + "/" + y + ")  Read CBPY @ " + fisPtr + "/" + bitPtr);
				if (hmCBPY != null){
//					DebugOut.debug_vv("CBPY says CBPY_0 = " + hmCBPY[0] + " CBPY_1 = " + hmCBPY[1] + 
//							" CBPY_2 = " + hmCBPY[2] + " CBPY_3 = " + hmCBPY[3]);
				} else {
//					DebugOut.debug_vv("/!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ CBPY decoding failed!!!!!!!!!!!!!!!!!!!!!!!!!! ");
				}
			} else {
				// TODO: there might be stuffing
//				DebugOut.debug_vv("MCBPC decoding failed!!!!!!!!!!!!!!!!!!!!!!!!!! /!!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\");
			}

			if (!p.hModifiedQuantization && (hmMCBPC[0] == 1 ||hmMCBPC[0] == 4 || hmMCBPC[0] == 5)) {
				// TODO: parse DQUANT (2bits)
//				DebugOut.debug_vv("Reading hMDQUANT");
				hMDQUANT = readBits(2);
			} else if (p.hModifiedQuantization) {
				// TODO: THIS MIGHT BE WRONG.
//				DebugOut.debug_vv("FUUUUUUUUUUUUUUUUUU (ModifiedQuantization)");
			}

			// TODO: parse MVD (two Variable Length Codes (VLC)

			if (hmMCBPC[0] != 3 && hmMCBPC[0] != 4 && hmMCBPC[0] != -1){
				if (!p.hUnrestrictedMotionVector) {
					// horizontal component followed by vertical component
					mvdHorizontal = readMVDComponent();
					if (mvdHorizontal == null){
//						DebugOut.debug_vv("/!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ horiz. MVD  component failed!");
					} 
					mvdVertical = readMVDComponent();
					if (mvdVertical == null){
//						DebugOut.debug_vv("/!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ vert. MVD  component failed!");
					}
					if (mvdHorizontal!=null && mvdVertical!=null){
//						DebugOut.debug_vv("/!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ MVD Components: " + mvdHorizontal[0] + "/" + mvdHorizontal[1] + " " + mvdVertical[0] + "/" + mvdVertical[1]);
						
						p.hMVDs[x][y][0] = mvdHorizontal;
						p.hMVDs[x][y][1] = mvdVertical;
					} else {
						
						p.hMVDs[x][y][0] = empty;
						p.hMVDs[x][y][1] = empty;
					}
					
					if (hmMCBPC[0] == 2 || hmMCBPC[0] == 5){
						// we have MVD_(2-4) as indicated by MCBPC block types 2 and 5 from table 9
//						DebugOut.debug_vv("Reading further MVDs");
						double[] mvdHorizontal2 = readMVDComponent();
						double[] mvdVertical2 = readMVDComponent();
						double[] mvdHorizontal3 = readMVDComponent();
						double[] mvdVertical3 = readMVDComponent();
						double[] mvdHorizontal4 = readMVDComponent();
						double[] mvdVertical4 = readMVDComponent();
					}
				} else {
					// read MVD component (x2) from Table D.3
//					DebugOut.debug_vv("FUUUUUUUUUUUUUUUUUU (UnrestrictedMotionVector)");
				}
			} else if(hmMCBPC[0] == 3) {
//				DebugOut.debug_vv("/!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ No MVD data, MB type" + hmMCBPC[0]);
				p.hMVDs[x][y][0] = empty;
				p.hMVDs[x][y][1] = empty;
				
			} else {
//				DebugOut.debug_vv("/!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ /!\\ MB decoding failed (unimplemented)" + hmMCBPC[0]);
			}
			
			// TODO: Read 6 blocks
			// 4 Luminance blocks
			// 2 color difference blocks
			// each block: [INTRADC, TCOEFF]
			
			// INTRADC is present for every block of the macroblock if
			// MCBPC indicates macroblock type 3 or	4 
			boolean bINTRADC = (hmMCBPC[0] == 3 || hmMCBPC[0] == 4);
			
			// TCOEF is present if indicated by MCBPC or CBPY
			
			if (hmCBPY!=null){
				
				if(hmMCBPC[0] == 3){
					// LUMINANCE 0 BLOCK
		//			decodeBlockLayer(bINTRADC, hmCBPY!=null && hmCBPY[0] == 1);
					decodeBlockLayer(bINTRADC, !(hmCBPY[0] == 1));
						
					// LUMINANCE 1 BLOCK			
		//			decodeBlockLayer(bINTRADC, hmCBPY!=null && hmCBPY[1] == 1);
					decodeBlockLayer(bINTRADC, !(hmCBPY[1] == 1));
		
					// LUMINANCE 2 BLOCK
		//			decodeBlockLayer(bINTRADC, hmCBPY!=null && hmCBPY[2] == 1);
					decodeBlockLayer(bINTRADC, !(hmCBPY[2] == 1));
		
					// LUMINANCE 3 BLOCK
		//			decodeBlockLayer(bINTRADC, hmCBPY!=null && hmCBPY[3] == 1);
					decodeBlockLayer(bINTRADC, !(hmCBPY[3] == 1));
				} else {
		// LUMINANCE 0 BLOCK
		//			decodeBlockLayer(bINTRADC, hmCBPY!=null && hmCBPY[0] == 1);
					decodeBlockLayer(bINTRADC, (hmCBPY[0] == 1));
						
					// LUMINANCE 1 BLOCK			
		//			decodeBlockLayer(bINTRADC, hmCBPY!=null && hmCBPY[1] == 1);
					decodeBlockLayer(bINTRADC, (hmCBPY[1] == 1));
		
					// LUMINANCE 2 BLOCK
		//			decodeBlockLayer(bINTRADC, hmCBPY!=null && hmCBPY[2] == 1);
					decodeBlockLayer(bINTRADC, (hmCBPY[2] == 1));
		
					// LUMINANCE 3 BLOCK
		//			decodeBlockLayer(bINTRADC, hmCBPY!=null && hmCBPY[3] == 1);
					decodeBlockLayer(bINTRADC, (hmCBPY[3] == 1));
				}
			}
			
			if (hmCBPY!=null){
			// COLOR DIFF 0 BLOCK
			decodeBlockLayer(bINTRADC, hmMCBPC[1] == 1);
			
			// COLOR DIFF 1 BLOCK
			decodeBlockLayer(bINTRADC, hmMCBPC[2] == 1);
			}
		} // else if (hMCOD)
	}


	
	private void decodeBlockLayer(boolean intradc, boolean tcoef) throws IOException{
		if (intradc) {
			// read INTRADC
			int hmINTRADC = readBits(8);
//			DebugOut.debug_vv("[block] hmINTRADC read (" + hmINTRADC +")");
		}
		
		
		
		if(tcoef){
			boolean tcoef_alive = true;
			while(tcoef_alive){
				int[] ret = consumeTCOEFF();
				// ret contains last0 run1 and level2
				//  [block] 1, level 1, last 1
//				DebugOut.debug_vvv("ret=" + (ret == null));
				if (ret != null) {
//					DebugOut.debug_vv("[block] " + ret[1] + ", level " + ret[2] +", last " + ret[0]);
//					DebugOut.debug_vvv("ret[0]=" + ret[0]);
					if (ret[0] == 1) {
						tcoef_alive = false;
//						DebugOut.debug_vv("[block] stop");
					}
				} else {
//					DebugOut.debug_vv("[block] failed");
				}
			}
		}
		
	}
	
	private void dumpNextByteToOut() throws IOException{
		int x = readBits(32);
		System.out.println(Integer.toBinaryString(x));
	}
	// #########################################################################
	// ################## single steps to read Picture Layer ###################
	// ############### (in order of appearance in bit stream) ##################

	private void checkForPictureStartCode() throws IOException, EOSException {
		// ??|Picture Start Code| = "??|00 0000 0000 0000 0010 0000|" = 22 bits;
		// | 0 0 0 0 2 0

		// "0000 0000 0011 1111 1111 1111 1111 1111" "clear mask";
		// 0x 0 0 3 f f f f f"

		int bitsBufPSC = 0;
		int bitCount = 0;
		while (true) {
			if (fis.available() == 0) {
				if(blocking){
					throw new EOSException("EOS");
				}
			}
			// push next read bit into bitsBufPSC from right side
			bitsBufPSC = bitsBufPSC << 1;
			bitsBufPSC = bitsBufPSC | readNextBit();
//			DebugOut.debug_vvv("bitsBufPSC is "
//					+ Integer.toBinaryString(bitsBufPSC));
			bitCount++;
			// clear left most 10 bit (only right most 22 bit are checked)
			int tmp = bitsBufPSC & 0x003fffff;

//			DebugOut.debug_vvv("tmp        is " + Integer.toBinaryString(tmp)
//					+ " = " + Integer.toHexString(tmp));

			if (bitCount >= 22 && tmp == 0x20) {
//				DebugOut.debug_vvv("found Picture Start Code @ " + fisPtr + " bit "
//						+ bitPtr);
				return;
			}
		}
	}

	private void checkForGOBStartCode() throws IOException, EOSException {
		// ??|E O S| = "??|0 0000 0000 0000 0001|" = 17 bits;
		// | 0 0 0 0 0 1

		// "0000 0000 0000 0001 1111 1111 1111 1111" "clear mask";
		// 0x 0 0 0 1 f f f f"

		int bitsBufGOB = 0;
		int bitCount = 0;
		while (true) {
			if (fis.available() == 0) {
				if(blocking){
					throw new EOSException("EOS");
				}
			}
			// push next read bit into bitsBufPSC from right side
			bitsBufGOB = bitsBufGOB << 1;
			bitsBufGOB = bitsBufGOB | readNextBit();
//			DebugOut.debug_vvv("EOS is " + Integer.toBinaryString(bitsBufGOB));
			bitCount++;
			// clear left most 10 bit (only right most 17 bit are checked)
			int tmp = bitsBufGOB & 0x0001ffff;

//			DebugOut.debug_vvv("tmp        is " + Integer.toBinaryString(tmp)
//					+ " = " + Integer.toHexString(tmp));

			if (bitCount >= 17 && tmp == 0x01) {
				DebugOut.debug_vv("found GOB Start Code @ " + fisPtr + " bit "
						+ bitPtr + ", " + (fisPtr - lastFisPtr) + " bits behind PSC");
				return;
			}
		}
	}
	
	/**
	 * TODO: This function has to be implemented for I-frames.
	 * @return
	 */
	private int readMCBPC4IFrames() {
		// Table 7/H.263 – VLC table for MCBPC (for I-pictures)

		// {MBType, CBPC_0, CBPC_1, NumberOfBits, Code}

		// MBTYPE = -1 = stuffing
		// CBPC_i = -1 = none?

		// 0 {3, 0,0, 1, 1}

		// 1 {3, 0,1, 3, 001}
		// 2 {3, 1,0, 3, 010}
		// 3 {3, 1,1, 3, 011}

		// 4 {4, 0,0, 4, 0001}

		// 5 {4, 0,1, 6, 0000 01}
		// 6 {4, 1,0, 6, 0000 10}
		// 7 {4, 1,1, 6, 0000 11}

		// 8 {-1, -1,-1, 9, 0000 0000 1}

		return 0;
	}

	/**
	 * This functions parses the MCBPC field for P-frames. It returns an integer
	 * array with the MBType, and the split CBPC field.
	 * 
	 * @return
	 * @throws IOException
	 */
	private int[] readMCBPC4PFrames() throws IOException {
		// Table 8/H.263 – VLC table for MCBPC (for P-pictures)

		// {MBType, CBPC_0, CBPC_1, NumberOfBits, Code}

		// MBTYPE = -1 = stuffing
		// CBPC_i = -1 = none?

		int tempBits = evalNext(0, 1, 1, 1);
		// 0 {0, 0,0, 1, 1}
		if (tempBits == -1){
			int[] res = {0,0,0};
			return res;
		}
		// 4 {1, 0,0, 3, 011}
		// 8 {2, 0,0, 3, 010}
		tempBits = evalNext(tempBits,2,0x3,3);
		if (tempBits == -1){
			int[] res = {1,0,0};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x2,3);
		if (tempBits == -1){
			int[] res = {2,0,0};
			return res;
		}
		// 1 {0, 0,1, 4, 0011}
		// 2 {0, 1,0, 4, 0010}
		tempBits = evalNext(tempBits,1,0x3,4);
		if (tempBits == -1){
			int[] res = {0,0,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x2,4);
		if (tempBits == -1){
			int[] res = {0,1,0};
			return res;
		}
		
		// 12 {3, 0,0, 5, 0001 1}
		tempBits = evalNext(tempBits,1,0x3,5);
		if (tempBits == -1){
			int[] res = {3,0,0};
			return res;
		}
		// 3 {0, 1,1, 6, 0001 01}
		// 16 {4, 0,0, 6, 0001 00}
		tempBits = evalNext(tempBits,1,0x5,6);
		if (tempBits == -1){
			int[] res = {0,1,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x4,6);
		if (tempBits == -1){
			int[] res = {4,0,0};
			return res;
		}
		
		
		// 5 {1, 0,1, 7, 0000 111}
		// 6 {1, 1,0, 7, 0000 110}
		// 15 {3, 1,1, 7, 0000 011}
		// 9 {2, 0,1, 7, 0000 101}
		// 10 {2, 1,0, 7, 0000 100}
		tempBits = evalNext(tempBits,1,0x7,7);
		if (tempBits == -1){
			int[] res = {1,0,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x6,7);
		if (tempBits == -1){
			int[] res = {1,1,0};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x3,7);
		if (tempBits == -1){
			int[] res = {3,1,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x5,7);
		if (tempBits == -1){
			int[] res = {2,0,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x4,7);
		if (tempBits == -1){
			int[] res = {2,1,0};
			return res;
		}
		
		
		// 11 {2, 1,1, 8, 0000 0101}
		// 13 {3, 0,1, 8, 0000 0100}
		// 14 {3, 1,0, 8, 0000 0011}
		tempBits = evalNext(tempBits,1,0x5,8);
		if (tempBits == -1){
			int[] res = {2,1,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x4,8);
		if (tempBits == -1){
			int[] res = {3,0,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x3,8);
		if (tempBits == -1){
			int[] res = {3,1,0};
			return res;
		}
		
		// 17 {4, 0,1, 9, 0000 0010 0}
		// 18 {4, 1,0, 9, 0000 0001 1}
		// 19 {4, 1,1, 9, 0000 0001 0}
		// 7 {1, 1,1, 9, 0000 0010 1}
		// 20 {-1, -1,-1, 9, 0000 0000 1}
		tempBits = evalNext(tempBits,1,0x4,9);
		if (tempBits == -1){
			int[] res = {4,0,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x3,9);
		if (tempBits == -1){
			int[] res = {4,1,0};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x2,9);
		if (tempBits == -1){
			int[] res = {4,1,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x5,9);
		if (tempBits == -1){
			int[] res = {1,1,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0x1,9);
		if (tempBits == -1){
			int[] res = {-1,-1,-1};
			return res;
		}
		
		
		// 21 {5, 0,0, 11, 0000 0000 010}
		tempBits = evalNext(tempBits,2,0x2,11);
		if (tempBits == -1){
			int[] res = {5,0,0};
			return res;
		}
		
		// 22 {5, 0,1, 13, 0000 0000 0110 0}
		// 23 {5, 1,0, 13, 0000 0000 0111 0}
		// 24 {5, 1,1, 13, 0000 0000 0111 1}
		tempBits = evalNext(tempBits,2,0xC,13);
		if (tempBits == -1){
			int[] res = {5,0,1};
			return res;
		}
		tempBits = evalNext(tempBits,0,0xE,13);
		if (tempBits == -1){
			int[] res = {5,1,0};
			return res;
		}
		tempBits = evalNext(tempBits,0,0xF,13);
		if (tempBits == -1){
			int[] res = {5,1,1};
			return res;
		}
		
		return null;
	}

	private int[] readCBPY() throws IOException {
		// Table 12/H.263 – VLC table for CBPY
		// Index CBPY(INTRA) CBPY(INTER) Number of 
		//       (12, 34) (12, 34) bits 
		// ONLY CBPY(INTER) (CBPY(INTRA) is inverted!)
		
		int tempBits = evalNext(0, 2, 0x3, 2);
		
		// 15 00 00 2 11
		if (tempBits == -1){
			int[] res = {0,0,0,0};
			return res;
		}
		
		// 0  11 11 4 0011
		// 12 00 11 4 0100
		// 10 01 01 4 0101
		// 14 00 01 4 0110
		// 5  10 10 4 0111
		// 13 00 10 4 1000
		// 3  11 00 4 1001
		// 11 01 00 4 1010
		// 7  10 00 4 1011
		tempBits = evalNext(tempBits, 2, 0x3, 4);
		if (tempBits == -1){
			int[] res = {1,1,1,1};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x4, 4);
		if (tempBits == -1){
			int[] res = {0,0,1,1};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x5, 4);
		if (tempBits == -1){
			int[] res = {0,1,0,1};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x6, 4);
		if (tempBits == -1){
			int[] res = {0,0,0,1};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x7, 4);
		if (tempBits == -1){
			int[] res = {1,0,1,0};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x8, 4);
		if (tempBits == -1){
			int[] res = {0,0,1,0};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x9, 4);
		if (tempBits == -1){
			int[] res = {1,1,0,0};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0xA, 4);
		if (tempBits == -1){
			int[] res = {0,1,0,0};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0xB, 4);
		if (tempBits == -1){
			int[] res = {1,0,0,0};
			return res;
		}
		
		// 8  01 11 5 0001 0
		// 4  10 11 5 0001 1 
		// 2  11 01 5 0010 0
		// 1  11 10 5 0010 1
		tempBits = evalNext(tempBits, 1, 0x2, 5);
		if (tempBits == -1){
			int[] res = {0,1,1,1};
			return res;
		}	
		tempBits = evalNext(tempBits, 0, 0x3, 5);
		if (tempBits == -1){
			int[] res = {1,0,1,1};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x4, 5);
		if (tempBits == -1){
			int[] res = {1,1,0,1};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x5, 5);
		if (tempBits == -1){
			int[] res = {1,1,1,0};
			return res;
		}
		
		// 6  10 01 6 0000 10
		// 9  01 10 6 0000 11
		tempBits = evalNext(tempBits, 1, 0x2, 6);
		if (tempBits == -1){
			int[] res = {1,0,0,1};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x3, 6);
		if (tempBits == -1){
			int[] res = {0,1,1,0};
			return res;
		}
		return null;
	}

	private double[] readMVDComponent() throws IOException{
		// Table 14/H.263 – VLC table for MVD
		
		int tempBits = evalNext(0, 1, 0x1, 1);
		// 32 0 	-		1 1
		if (tempBits == -1){
			double[] res = {0, Double.NaN};
			return res;
		}
		
		
		// 31 –0.5 	31.5 	3 011
		// 33 0.5 	–31.5 	3 010
		tempBits = evalNext(tempBits, 2, 0x3, 3);
		if (tempBits == -1){
			double[] res = {-0.5, 31.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x2, 3);
		if (tempBits == -1){
			double[] res = {0.5, -31.5};
			return res;
		}
		
		// 30 –1 	31 		4 0011
		// 34 1 	–31 	4 0010
		tempBits = evalNext(tempBits, 1, 0x3, 4);
		if (tempBits == -1){
			double[] res = {-1, 31};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x2, 4);
		if (tempBits == -1){
			double[] res = {1, -31};
			return res;
		}
		
		// 29 –1.5 	30.5 	5 0001 1
		// 35 1.5 	–30.5 	5 0001 0
		tempBits = evalNext(tempBits, 1, 0x3, 5);
		if (tempBits == -1){
			double[] res = {-1.5, 30.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x2, 5);
		if (tempBits == -1){
			double[] res = {1.5, -30.5};
			return res;
		}
		
		
		// 28 –2 	30 		7 0000 111
		// 36 2 	–30 	7 0000 110
		tempBits = evalNext(tempBits, 2, 0x7, 7);
		if (tempBits == -1){
			double[] res = {-2, 30};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x6, 7);
		if (tempBits == -1){
			double[] res = {2, -30};
			return res;
		}
		
		// 25 –3.5 	28.5 	8 0000 0111
		// 26 –3 	29 		8 0000 1001
		// 27 –2.5 	29.5 	8 0000 1011
		// 37 2.5 	–29.5 	8 0000 1010
		// 38 3 	–29 	8 0000 1000
		// 39 3.5 	–28.5 	8 0000 0110
		tempBits = evalNext(tempBits, 1, 0x7, 8);
		if (tempBits == -1){
			double[] res = {-3.5, 28.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x9, 8);
		if (tempBits == -1){
			double[] res = {-3, 29};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0xB, 8);
		if (tempBits == -1){
			double[] res = {-2.5, 29.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0xA, 8);
		if (tempBits == -1){
			double[] res = {2.5, -29.5};
			return res;
		}
		
		tempBits = evalNext(tempBits, 0, 0x8, 8);
		if (tempBits == -1){
			double[] res = {3, -29};
			return res;
		}
		
		tempBits = evalNext(tempBits, 0, 0x6, 8);
		if (tempBits == -1){
			double[] res = {3.5, -28.5};
			return res;
		}
		
		
		
		
		// 22 –5 	27 		10 0000 0100 11
		// 23 –4.5 	27.5 	10 0000 0101 01
		// 24 –4 	28 		10 0000 0101 11
		// 40 4 	–28 	10 0000 0101 10
		// 41 4.5 	–27.5 	10 0000 0101 00
		// 42 5 	–27 	10 0000 0100 10
		tempBits = evalNext(tempBits, 2, 0x13, 10);
		if (tempBits == -1){
			double[] res = {-5, 27};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x15, 10);
		if (tempBits == -1){
			double[] res = {-4.5, 27.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x17, 10);
		if (tempBits == -1){
			double[] res = {-4, 28};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x16, 10);
		if (tempBits == -1){
			double[] res = {4, -28};
			return res;
		}
		
		tempBits = evalNext(tempBits, 0, 0x14, 10);
		if (tempBits == -1){
			double[] res = {4.5, -27.5};
			return res;
		}
		
		tempBits = evalNext(tempBits, 0, 0x12, 10);
		if (tempBits == -1){
			double[] res = {5, -27};
			return res;
		}
		
		
		// 8 –12 	20 		11 0000 0001 001
		// 9 –11.5 	20.5 	11 0000 0001 011
		// 10 –11 	21 		11 0000 0001 101
		// 11 –10.5 21.5 	11 0000 0001 111
		// 12 –10 	22 		11 0000 0010 001
		// 13 –9.5 	22.5 	11 0000 0010 011
		tempBits = evalNext(tempBits, 1, 0x09, 11);
		if (tempBits == -1){
			double[] res = {-12, 20};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x0B, 11);
		if (tempBits == -1){
			double[] res = {-11.5, 20.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x0D, 11);
		if (tempBits == -1){
			double[] res = {-11, 21};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x0F, 11);
		if (tempBits == -1){
			double[] res = {-10.5, 21.5};
			return res;
		}
		
		tempBits = evalNext(tempBits, 0, 0x11, 11);
		if (tempBits == -1){
			double[] res = {-10, 22};
			return res;
		}
		
		tempBits = evalNext(tempBits, 0, 0x13, 11);
		if (tempBits == -1){
			double[] res = {-9.5, 22.5};
			return res;
		}
		
		// 14 –9 	23 		11 0000 0010 101
		// 15 –8.5 	23.5 	11 0000 0010 111
		// 16 –8 	24 		11 0000 0011 001
		// 17 –7.5 	24.5 	11 0000 0011 011
		// 18 –7 	25 		11 0000 0011 101
		// 19 –6.5 	25.5 	11 0000 0011 111
		tempBits = evalNext(tempBits, 0, 0x15, 11);
		if (tempBits == -1){
			double[] res = {-9, 23};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x17, 11);
		if (tempBits == -1){
			double[] res = {-8.5, 23.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x19, 11);
		if (tempBits == -1){
			double[] res = {-8, 24};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x1B, 11);
		if (tempBits == -1){
			double[] res = {-7.5, 24.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x1D, 11);
		if (tempBits == -1){
			double[] res = {-7, 25};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x1F, 11);
		if (tempBits == -1){
			double[] res = {-6.5, 25.5};
			return res;
		}
		
		
		// 20 –6 	26 		11 0000 0100 001
		// 21 –5.5 	26.5 	11 0000 0100 011
		// 43 5.5 	–26.5 	11 0000 0100 010
		// 44 6 	–26 	11 0000 0100 000
		// 45 6.5 	–25.5 	11 0000 0011 110
		// 46 7 	–25 	11 0000 0011 100
		tempBits = evalNext(tempBits, 0, 0x21, 11);
		if (tempBits == -1){
			double[] res = {-6, 26};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x23, 11);
		if (tempBits == -1){
			double[] res = {-5.5, 26.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x22, 11); // fixed 0x24 to 0x22
		if (tempBits == -1){
			double[] res = {5.5, -26.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x20, 11);
		if (tempBits == -1){
			double[] res = {6, -26};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x1E, 11);
		if (tempBits == -1){
			double[] res = {6.5, -25.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x1C, 11);
		if (tempBits == -1){
			double[] res = {7, -25};
			return res;
		}
		
		// 47 7.5 	–24.5 	11 0000 0011 010
		// 48 8 	–24 	11 0000 0011 000
		// 49 8.5 	–23.5 	11 0000 0010 110
		// 50 9 	–23 	11 0000 0010 100
		// 51 9.5 	–22.5 	11 0000 0010 010
		// 52 10 	–22 	11 0000 0010 000
		tempBits = evalNext(tempBits, 0, 0x1A, 11);
		if (tempBits == -1){
			double[] res = {7.5, -24.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x18, 11);
		if (tempBits == -1){
			double[] res = {8, -24};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x16, 11);
		if (tempBits == -1){
			double[] res = {8.5, -23.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x14, 11);
		if (tempBits == -1){
			double[] res = {9, -23};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x12, 11);
		if (tempBits == -1){
			double[] res = {9.5, -22.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x10, 11);
		if (tempBits == -1){
			double[] res = {10, -22};
			return res;
		}
		
		
		// 53 10.5 	–21.5 	11 0000 0001 110
		// 54 11 	–21 	11 0000 0001 100
		// 55 11.5 	–20.5 	11 0000 0001 010
		// 56 12 	–20 	11 0000 0001 000
		tempBits = evalNext(tempBits, 0, 0x0E, 11);
		if (tempBits == -1){
			double[] res = {10.5, -21.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x0C, 11);
		if (tempBits == -1){
			double[] res = {11, -21};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x0A, 11);
		if (tempBits == -1){
			double[] res = {11.5, -20.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x08, 11);
		if (tempBits == -1){
			double[] res = {12, -20};
			return res;
		}
		
		
		
		// 2 –15 	17 		12 0000 0000 0101
		// 3 –14.5 	17.5 	12 0000 0000 0111
		// 4 –14 	18 		12 0000 0000 1001
		// 5 –13.5 	18.5 	12 0000 0000 1011
		// 6 –13 	19 		12 0000 0000 1101
		// 7 –12.5 	19.5 	12 0000 0000 1111
		tempBits = evalNext(tempBits, 1, 0x05, 12);
		if (tempBits == -1){
			double[] res = {-15, 17};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x07, 12);
		if (tempBits == -1){
			double[] res = {-14.5, 17.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x09, 12);
		if (tempBits == -1){
			double[] res = {-14, 18};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x0B, 12);
		if (tempBits == -1){
			double[] res = {-13.5, 18.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x0D, 12);
		if (tempBits == -1){
			double[] res = {-13, 19};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x0F, 12);
		if (tempBits == -1){
			double[] res = {-12,5, 19.5};
			return res;
		}
		
		// 57 12.5 	–19.5 	12 0000 0000 1110
		// 58 13 	–19 	12 0000 0000 1100
		// 59 13.5 	–18.5 	12 0000 0000 1010
		// 60 14 	–18 	12 0000 0000 1000
		// 61 14.5 	–17.5 	12 0000 0000 0110
		// 62 15 	–17 	12 0000 0000 0100
		tempBits = evalNext(tempBits, 0, 0x0E, 12);
		if (tempBits == -1){
			double[] res = {12.5, -19.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x0C, 12);
		if (tempBits == -1){
			double[] res = {13, -19};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x0A, 12);
		if (tempBits == -1){
			double[] res = {13.5, -18.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x08, 12);
		if (tempBits == -1){
			double[] res = {14, -18};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x06, 12);
		if (tempBits == -1){
			double[] res = {14.5, -17.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x04, 12);
		if (tempBits == -1){
			double[] res = {15, -17};
			return res;
		}
		
		// 0 –16 	16 		13 0000 0000 0010 1
		// 1 –15.5 	16.5 	13 0000 0000 0011 1
		// 63 15.5 	–16.5 	13 0000 0000 0011 0
		tempBits = evalNext(tempBits, 1, 0x05, 13);
		if (tempBits == -1){
			double[] res = {-16, 16};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x07, 13);
		if (tempBits == -1){
			double[] res = {-15.5, 16.5};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x06, 13);
		if (tempBits == -1){
			double[] res = {15.5, -16.5};
			return res;
		}

		
		return null;
	}
	
	private int[] consumeTCOEFF() throws IOException{
		int tempBits = 0;
		// 0 0 0 1 		3 10s
		tempBits = evalNextWithSBit(tempBits, 3, 0x02,2);
		if (tempBits == -1){
			int[] res = {0,0,1, (tempBits & 0x1)};
			return res;
		}
		
		
		// 12 0 1 1 		4 110s
		tempBits = evalNextWithSBit(tempBits, 1, 0x06,3);
		if (tempBits == -1){
			int[] res = {0,1,1, (tempBits & 0x1)};
			return res;
		}
		
		// 1 0 0 2 			5 1111s
		// 18 0 2 1 		5 1110s
		// 58 1 0 1 		5 0111s
		tempBits = evalNextWithSBit(tempBits, 1, 0x0F,4);
		if (tempBits == -1){
			int[] res = {0,0,2, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0E,4);
		if (tempBits == -1){
			int[] res = {0,2,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x07,4);
		if (tempBits == -1){
			int[] res = {1,0,1, (tempBits & 0x1)};
			return res;
		}

		// 22 0 3 1 		6 0110 1s
		// 25 0 4 1 		6 0110 0s
		// 28 0 5 1 		6 0101 1s
		tempBits = evalNextWithSBit(tempBits, 1, 0x0D,5);
		if (tempBits == -1){
			int[] res = {0,3,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0C,5);
		if (tempBits == -1){
			int[] res = {0,4,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0B,5);
		if (tempBits == -1){
			int[] res = {0,5,1, (tempBits & 0x1)};
			return res;
		}
		
		// 2 0 0 3 			7 0101 01s
		// 13 0 1 2 		7 0101 00s
		// 31 0 6 1 		7 0100 11s
		tempBits = evalNextWithSBit(tempBits, 1, 0x15,6);
		if (tempBits == -1){
			int[] res = {0,0,3, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x14,6);
		if (tempBits == -1){
			int[] res = {0,1,2, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x13,6);
		if (tempBits == -1){
			int[] res = {0,6,1, (tempBits & 0x1)};
			return res;
		}
		
		// 34 0 7 1 		7 0100 10s
		// 36 0 8 1 		7 0100 01s
		// 38 0 9 1 		7 0100 00s
		// 61 1 1 1 		7 0011 11s
		tempBits = evalNextWithSBit(tempBits, 0, 0x12,6);
		if (tempBits == -1){
			int[] res = {0,7,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x11,6);
		if (tempBits == -1){
			int[] res = {0,8,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x10,6);
		if (tempBits == -1){
			int[] res = {0,9,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0F,6);
		if (tempBits == -1){
			int[] res = {1,1,1, (tempBits & 0x1)};
			return res;
		}
		
		// 63 1 2 1 		7 0011 10s
		// 64 1 3 1 		7 0011 01s
		// 65 1 4 1 		7 0011 00s
		// 102 ESCAPE - - 	7 0000 011
		tempBits = evalNextWithSBit(tempBits, 0, 0x0E,6);
		if (tempBits == -1){
			int[] res = {1,2,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0D,6);
		if (tempBits == -1){
			int[] res = {1,3,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0C,6);
		if (tempBits == -1){
			int[] res = {1,4,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNext(tempBits, 0, 0x03,7); // evalNext, because length is 7 and no s bit
		if (tempBits == -1){
			int[] res = {readBits(1),readBits(6),readBits(8), 0xE5CA}; // sign == 0xE5CA -> interpret level according to table 17
			return res;
		}
		
		// 42 0 11 1 		8 0010 101s
		// 43 0 12 1 		8 0010 100s
		// 40 0 10 1 		8 0010 110s
		// 3 0 0 4 			8 0010 111s
		tempBits = evalNextWithSBit(tempBits, 1, 0x15,7);
		if (tempBits == -1){
			int[] res = {0,11,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x14,7);
		if (tempBits == -1){
			int[] res = {0,12,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x16,7);
		if (tempBits == -1){
			int[] res = {0,10,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x17,7);
		if (tempBits == -1){
			int[] res = {0,0,4, (tempBits & 0x1)};
			return res;
		}
		
		// 66 1 5 1 		8 0010 011s
		// 67 1 6 1 		8 0010 010s
		// 68 1 7 1 		8 0010 001s
		// 69 1 8 1 		8 0010 000s
		tempBits = evalNextWithSBit(tempBits, 0, 0x13,7);
		if (tempBits == -1){
			int[] res = {1,5,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x12,7);
		if (tempBits == -1){
			int[] res = {1,6,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x11,7);
		if (tempBits == -1){
			int[] res = {1,7,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x10,7);
		if (tempBits == -1){
			int[] res = {1,8,1, (tempBits & 0x1)};
			return res;
		}
		

		// 4 0 0 5 			9 0001 1111s
		// 14 0 1 3 		9 0001 1110s
		// 19 0 2 2 		9 0001 1101s
		// 44 0 13 1 		9 0001 1100s
		tempBits = evalNextWithSBit(tempBits, 1, 0x1F,8);
		if (tempBits == -1){
			int[] res = {0,0,5, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x1E,8);
		if (tempBits == -1){
			int[] res = {0,1,3, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x1D,8);
		if (tempBits == -1){
			int[] res = {0,2,2, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x1C,8);
		if (tempBits == -1){
			int[] res = {0,13,1, (tempBits & 0x1)};
			return res;
		}
		
		// 45 0 14 1 		9 0001 1011s
		// 70 1 9 1 		9 0001 1010s
		// 71 1 10 1 		9 0001 1001s
		// 72 1 11 1 		9 0001 1000s
		tempBits = evalNextWithSBit(tempBits, 0, 0x1B,8);
		if (tempBits == -1){
			int[] res = {0,14,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x1A,8);
		if (tempBits == -1){
			int[] res = {1,9,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x19,8);
		if (tempBits == -1){
			int[] res = {1,10,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x18,8);
		if (tempBits == -1){
			int[] res = {1,11,1, (tempBits & 0x1)};
			return res;
		}
		
		// 73 1 12 1 		9 0001 0111s
		// 74 1 13 1 		9 0001 0110s
		// 75 1 14 1 		9 0001 0101s
		// 76 1 15 1 		9 0001 0100s
		// 77 1 16 1 		9 0001 0011s
		tempBits = evalNextWithSBit(tempBits, 0, 0x17,8);
		if (tempBits == -1){
			int[] res = {1,12,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x16,8);
		if (tempBits == -1){
			int[] res = {1,13,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x15,8);
		if (tempBits == -1){
			int[] res = {1,14,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x14,8);
		if (tempBits == -1){
			int[] res = {1,15,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x13,8);
		if (tempBits == -1){
			int[] res = {1,16,1, (tempBits & 0x1)};
			return res;
		}
		
		// 5 0 0 6 			10 0001 0010 1s
		// 6 0 0 7 			10 0001 0010 0s
		// 23 0 3 2 		10 0001 0001 1s
		// 26 0 4 2 		10 0001 0001 0s
		tempBits = evalNextWithSBit(tempBits, 1, 0x25,9);
		if (tempBits == -1){
			int[] res = {0,0,6, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x24,9);
		if (tempBits == -1){
			int[] res = {0,0,7, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x23,9);
		if (tempBits == -1){
			int[] res = {0,3,2, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x22,9);
		if (tempBits == -1){
			int[] res = {0,4,2, (tempBits & 0x1)};
			return res;
		}
		
		// 46 0 15 1 		10 0001 0000 1s
		// 47 0 16 1 		10 0001 0000 0s
		// 48 0 17 1 		10 0000 1111 1s
		// 49 0 18 1 		10 0000 1111 0s
		tempBits = evalNextWithSBit(tempBits, 0, 0x21,9);
		if (tempBits == -1){
			int[] res = {0,15,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x20,9);
		if (tempBits == -1){
			int[] res = {0,16,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x1F,9);
		if (tempBits == -1){
			int[] res = {0,17,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x1E,9);
		if (tempBits == -1){
			int[] res = {0,18,1, (tempBits & 0x1)};
			return res;
		}
		
		// 50 0 19 1 		10 0000 1110 1s
		// 51 0 20 1 		10 0000 1110 0s
		// 52 0 21 1 		10 0000 1101 1s
		// 53 0 22 1 		10 0000 1101 0s
		tempBits = evalNextWithSBit(tempBits, 0, 0x1D,9);
		if (tempBits == -1){
			int[] res = {0,19,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x1C,9);
		if (tempBits == -1){
			int[] res = {0,20,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x1B,9);
		if (tempBits == -1){
			int[] res = {0,21,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x1A,9);
		if (tempBits == -1){
			int[] res = {0,22,1, (tempBits & 0x1)};
			return res;
		}
		
		// 59 1 0 2 		10 0000 1100 1s
		// 78 1 17 1 		10 0000 1100 0s
		// 79 1 18 1 		10 0000 1011 1s
		// 80 1 19 1 		10 0000 1011 0s
		tempBits = evalNextWithSBit(tempBits, 0, 0x19,9);
		if (tempBits == -1){
			int[] res = {1,0,2, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x18,9);
		if (tempBits == -1){
			int[] res = {1,17,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x17,9);
		if (tempBits == -1){
			int[] res = {1,18,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x16,9);
		if (tempBits == -1){
			int[] res = {1,19,1, (tempBits & 0x1)};
			return res;
		}
		
		// 81 1 20 1 		10 0000 1010 1s
		// 82 1 21 1 		10 0000 1010 0s
		// 83 1 22 1 		10 0000 1001 1s
		// 84 1 23 1 		10 0000 1001 0s
		// 85 1 24 1 		10 0000 1000 1s
		tempBits = evalNextWithSBit(tempBits, 0, 0x15,9);
		if (tempBits == -1){
			int[] res = {1,20,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x14,9);
		if (tempBits == -1){
			int[] res = {1,21,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x13,9);
		if (tempBits == -1){
			int[] res = {1,22,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x12,9);
		if (tempBits == -1){
			int[] res = {1,23,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x11,9);
		if (tempBits == -1){
			int[] res = {1,24,1, (tempBits & 0x1)};
			return res;
		}

		// 7 0 0 8 			11 0000 1000 01s
		// 8 0 0 9 			11 0000 1000 00s
		// 15 0 1 4 		11 0000 0011 11s
		// 20 0 2 3 		11 0000 0011 10s
		tempBits = evalNextWithSBit(tempBits, 1, 0x21,10);
		if (tempBits == -1){
			int[] res = {0,0,8, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x20,10);
		if (tempBits == -1){
			int[] res = {0,0,9, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0F,10);
		if (tempBits == -1){
			int[] res = {0,1,4, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0E,10);
		if (tempBits == -1){
			int[] res = {0,2,3, (tempBits & 0x1)};
			return res;
		}
		
		// 24 0 3 3 		11 0000 0011 01s
		// 29 0 5 2 		11 0000 0011 00s
		// 32 0 6 2 		11 0000 0010 11s
		// 35 0 7 2 		11 0000 0010 10s
		tempBits = evalNextWithSBit(tempBits, 0, 0x0D,10);
		if (tempBits == -1){
			int[] res = {0,3,3, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0C,10);
		if (tempBits == -1){
			int[] res = {0,5,2, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0B,10);
		if (tempBits == -1){
			int[] res = {0,6,2, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x0A,10);
		if (tempBits == -1){
			int[] res = {0,7,2, (tempBits & 0x1)};
			return res;
		}		
		
		// 37 0 8 2 		11 0000 0010 01s
		// 39 0 9 2 		11 0000 0010 00s
		// 86 1 25 1 		11 0000 0001 11s
		// 87 1 26 1 		11 0000 0001 10s
		// 88 1 27 1 		11 0000 0001 01s
		// 89 1 28 1 		11 0000 0001 00s
		tempBits = evalNextWithSBit(tempBits, 0, 0x09,10);
		if (tempBits == -1){
			int[] res = {0,8,2, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x08,10);
		if (tempBits == -1){
			int[] res = {0,9,2, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x07,10);
		if (tempBits == -1){
			int[] res = {1,25,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x06,10);
		if (tempBits == -1){
			int[] res = {1,26,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x05,10);
		if (tempBits == -1){
			int[] res = {1,27,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x04,10);
		if (tempBits == -1){
			int[] res = {1,28,1, (tempBits & 0x1)};
			return res;
		}

		// 9 0 0 10 		12 0000 0000 111s
		// 10 0 0 11 		12 0000 0000 110s
		// 11 0 0 12 		12 0000 0100 000s
		// 16 0 1 5 		12 0000 0100 001s
		tempBits = evalNextWithSBit(tempBits, 1, 0x07,11);
		if (tempBits == -1){
			int[] res = {0,0,10, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x06,11);
		if (tempBits == -1){
			int[] res = {0,0,11, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x20,11);
		if (tempBits == -1){
			int[] res = {0,0,12, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x21,11);
		if (tempBits == -1){
			int[] res = {0,1,5, (tempBits & 0x1)};
			return res;
		}
		
		// 54 0 23 1 		12 0000 0100 010s
		// 55 0 24 1 		12 0000 0100 011s
		// 60 1 0 3 		12 0000 0000 101s
		// 62 1 1 2 		12 0000 0000 100s
		tempBits = evalNextWithSBit(tempBits, 0, 0x22,11);
		if (tempBits == -1){
			int[] res = {0,23,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x23,11);
		if (tempBits == -1){
			int[] res = {0,24,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x05,11);
		if (tempBits == -1){
			int[] res = {1,0,3, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x04,11);
		if (tempBits == -1){
			int[] res = {1,1,2, (tempBits & 0x1)};
			return res;
		}
		
		// 90 1 29 1 		12 0000 0100 100s
		// 91 1 30 1 		12 0000 0100 101s
		// 92 1 31 1 		12 0000 0100 110s
		// 93 1 32 1 		12 0000 0100 111s
		tempBits = evalNextWithSBit(tempBits, 0, 0x24,11);
		if (tempBits == -1){
			int[] res = {1,29,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x25,11);
		if (tempBits == -1){
			int[] res = {1,30,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x26,11);
		if (tempBits == -1){
			int[] res = {1,31,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x27,11);
		if (tempBits == -1){
			int[] res = {1,32,1, (tempBits & 0x1)};
			return res;
		}
		
		
		// 17 0 1 6 		13 0000 0101 0000s
		// 21 0 2 4 		13 0000 0101 0001s
		// 27 0 4 3 		13 0000 0101 0010s
		// 30 0 5 3 		13 0000 0101 0011s
		tempBits = evalNextWithSBit(tempBits, 1, 0x50,12);
		if (tempBits == -1){
			int[] res = {0,1,6, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x51,12);
		if (tempBits == -1){
			int[] res = {0,2,4, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x52,12);
		if (tempBits == -1){
			int[] res = {0,4,3, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x53,12);
		if (tempBits == -1){
			int[] res = {0,5,3, (tempBits & 0x1)};
			return res;
		}
		
		// 33 0 6 3 		13 0000 0101 0100s
		// 41 0 10 2 		13 0000 0101 0101s
		// 56 0 25 1 		13 0000 0101 0110s
		// 57 0 26 1 		13 0000 0101 0111s

		tempBits = evalNextWithSBit(tempBits, 0, 0x54,12);
		if (tempBits == -1){
			int[] res = {0,6,3, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x55,12);
		if (tempBits == -1){
			int[] res = {0,10,2, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x56,12);
		if (tempBits == -1){
			int[] res = {0,25,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x57,12);
		if (tempBits == -1){
			int[] res = {0,26,1, (tempBits & 0x1)};
			return res;
		}
		
		// 94 1 33 1 		13 0000 0101 1000s
		// 95 1 34 1 		13 0000 0101 1001s
		// 96 1 35 1 		13 0000 0101 1010s
		// 97 1 36 1 		13 0000 0101 1011s
		tempBits = evalNextWithSBit(tempBits, 0, 0x58,12);
		if (tempBits == -1){
			int[] res = {1,33,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x59,12);
		if (tempBits == -1){
			int[] res = {1,34,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x5A,12);
		if (tempBits == -1){
			int[] res = {1,35,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x5B,12);
		if (tempBits == -1){
			int[] res = {1,36,1, (tempBits & 0x1)};
			return res;
		}
		
		// 98 1 37 1 		13 0000 0101 1100s
		// 99 1 38 1 		13 0000 0101 1101s
		// 100 1 39 1 		13 0000 0101 1110s
		// 101 1 40 1 		13 0000 0101 1111s
		tempBits = evalNextWithSBit(tempBits, 0, 0x5C,12);
		if (tempBits == -1){
			int[] res = {1,37,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x5D,12);
		if (tempBits == -1){
			int[] res = {1,38,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x5E,12);
		if (tempBits == -1){
			int[] res = {1,39,1, (tempBits & 0x1)};
			return res;
		}
		tempBits = evalNextWithSBit(tempBits, 0, 0x5F,12);
		if (tempBits == -1){
			int[] res = {1,40,1, (tempBits & 0x1)};
			return res;
		}
		
		return null;
	}
	
	/**
	 * This function takes tempBits and shifts this integer numNewBits to the
	 * left and adds the same number of new bits from the bit stream to it. The
	 * resulting tempBits is AND'ed with 0001..1 (refLen ones) to check if it
	 * then equals ref. If this is the case -1 is returned, else the new and
	 * modified version of tempBits.
	 * 
	 * @param tempBits
	 *            the integer to read new bits to (from the right hand side)
	 * @param numNewBits
	 *            amount of bits to shift and read
	 * @param ref
	 *            a value to check if the right refLen bits of tempBits is equal
	 *            to
	 * @param refLen
	 *            the length of ref bits
	 * @return -1 if ref was equally matched, else the modified version of
	 *         tempBits
	 * @throws IOException
	 */
	private int evalNext(int tempBits, int numNewBits, int ref, int refLen)
			throws IOException {
		for (int i = 0; i < numNewBits; i++) {
			tempBits = tempBits << 1;
			tempBits = tempBits | readNextBit();
		}
	
		if ((tempBits & getBitMask(refLen)) == ref) {
			DebugOut.debug_vv("Matched " + refLen + " bits and found " + Integer.toBinaryString(ref) +" @ " + fisPtr + "/" + bitPtr);
			return -1;
		}
	
		return tempBits;
	}
	
	/**
	 * Similar to evalNext. only refLen bits and ref are compared, but before
	 * the pattern is right shifted such that the LSB can be 0 or 1.
	 * @param tempBits
	 * @param numNewBits
	 * @param ref
	 * @param refLen
	 * @return
	 * @throws IOException
	 */
	private int evalNextWithSBit(int tempBits, int numNewBits, int ref, int refLen)
			throws IOException {
		for (int i = 0; i < numNewBits; i++) {
			tempBits = tempBits << 1;
			tempBits = tempBits | readNextBit();
		}
	
		// by right shifting we ignore le right most bit!
		if (((tempBits>>1) & getBitMask(refLen)) == ref) {
			DebugOut.debug_vv("Matched_ " + refLen + " bits and found " + Integer.toBinaryString(ref) +" @ " + fisPtr + "/" + bitPtr);
			return -1;
		}
	
		return tempBits;
	}

	/**
	 * This function returns an integer with numOnes ones on LSB side
	 * 
	 * @param numOnes
	 *            number of ones
	 * @return the generated integer
	 */
	private int getBitMask(int numOnes) {
		int res = 0;
		for (int i = 0; i < numOnes; i++) {
			res <<= 1;
			res |= 1;
		}
		return res;
	}

	private void checkForEndOfSequenceCode() throws IOException {
		// ??|E O S| = "??|00 0000 0000 0000 00 11 1111|" = 22 bits;
		// | 0 0 0 0 3 f

		// "0000 0000 0011 1111 1111 1111 1111 1111" "clear mask";
		// 0x 0 0 3 f f f f f"

		int bitsBufEOS = 0;
		int bitCount = 0;
		while (true) {
			// push next read bit into bitsBufPSC from right side
			bitsBufEOS = bitsBufEOS << 1;
			bitsBufEOS = bitsBufEOS | readNextBit();
//			DebugOut.debug_vvv("EOS is " + Integer.toBinaryString(bitsBufEOS));
			bitCount++;
			// clear left most 10 bit (only right most 22 bit are checked)
			int tmp = bitsBufEOS & 0x003fffff;

//			DebugOut.debug_vvv("tmp        is " + Integer.toBinaryString(tmp)
//					+ " = " + Integer.toHexString(tmp));

			if (bitCount >= 22 && tmp == 0x3f) {
				DebugOut.debug_vv("found End Of Sequence Code @ " + fisPtr
						+ " bit " + bitPtr);
				return;
			}
		}
	}

	// #########################################################################
	// ########################### bit stream access ###########################
	// #########################################################################

	/**
	 * Checks if bit i in integer data is set to 1
	 * 
	 * @param data
	 *            the integer to check
	 * @param i
	 *            the bit to check (from right to left, 0 - 31)(LSB is 0, right)
	 * @return if bit i in integer data is set to 1
	 */
	private boolean b(int data, int i) {
		int mask = (1 << i);
		return (data & mask) << (31 - i) == -2147483648;

		// Java is kind of 'weird' here:
		// if we shift (1 << 31> (0b10....) to the right java introduces new
		// 1's from the left e.g. (1<<31)>>3 = 0b11110.... (3 new 1's)
		// int mask = (1 << i);
		// return (data & mask) >> i == 1;
	}

	private int readBits(int numBits) throws IOException {
		int res = 0;

		for (int i = 0; i < numBits; i++) {
			res = res << 1;
			int nextBit = readNextBit();
//			DebugOut.debug_vvv("next bit (" + i + ") was " + nextBit);
			res = res | nextBit;
		}

		return res;
	}

	private int readNextBit() throws IOException {
		int res = (currentByte & (0x01 << bitPtr)) >> bitPtr;

//		DebugOut.debug_vvv("read bit = " + Integer.toBinaryString(res));

		bitPtr--;
		if (bitPtr < 0) {
			bitPtr = 7;
			readNextByte();
		}
		return res;
	}

	private boolean useByteBuffer = true;
	private void readNextByte() throws IOException {
		fisPtr++;
		int b;
		if (useByteBuffer){
			b = byteBufferNextByte();
		} else {
			b = fis.read();
		}
//		DebugOut.debug_vvv("read b = " + Integer.toBinaryString(b));
		this.currentByte = b;
	}

	private int byteBufferSize = 3072; // ~3kb
	private byte[] byteBuffer = new byte[byteBufferSize];
	private int byteBufferPointer = -1;
	
	private int byteBufferNextByte() throws IOException{
		if ( byteBufferPointer == -1 || byteBufferPointer == byteBufferSize){
			// update buffer
			int x = fis.read(byteBuffer);
			byteBufferPointer = 0;
		}
		
		int b = byteBuffer[byteBufferPointer];
		
		byteBufferPointer++;		
		return b;
	}
	
	public String getStats(){
		return "I Frames: " + numIframes + ", P Frames: " + numPframes +
				"\nGOBs: " + groupOfBlocksCount + ", MBs: " + macroBlockCount +
				"\nSize: " + width + "x" + height;
	}
	
	// #########################################################################
	// ########################## old test code below ##########################
	// #########################################################################

	private void testBitIIsOne() {
		DebugOut.debug_vv("test: " + b(1 << 0, 0));
		DebugOut.debug_vv("test: " + b(1 << 1, 1));
		DebugOut.debug_vv("test: " + b(1 << 2, 2));
		DebugOut.debug_vv("test: " + b(1 << 16, 16));
		DebugOut.debug_vv("test: " + b(1 << 30, 30));
		DebugOut.debug_vv("test: " + b(1 << 31, 31)); // THIS FAILS!!
		DebugOut.debug_vv("test: " + ((1 << 31)) + " "
				+ Integer.toBinaryString(((1 << 31))));
		int x = ((1 << 31) >> 31) & 1 << 31;
		DebugOut.debug_vv("test: " + x + " " + Integer.toBinaryString(x) + "|"
				+ Integer.toBinaryString(x).length());

	}

	private void testReadNextBit() throws IOException {
		DebugOut.debug_vv("testing readNextBit()");

		currentByte = 0xAB; // = 0b10101011
		bitPtr = 7;
		int numBits = 8;
		while (numBits > 0) {
			DebugOut.debug_vv("read " + (readNextBit() == 1 ? "1" : "0"));
			numBits--;
		}

	}
}
