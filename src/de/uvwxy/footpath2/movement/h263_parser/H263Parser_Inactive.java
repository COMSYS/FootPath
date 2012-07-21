package de.uvwxy.footpath2.movement.h263_parser;

import java.io.IOException;


/**
 * Collecting inactive functions to clean up the code
 * @author paul
 *
 */
public class H263Parser_Inactive {
	// private int[] getTCOEFF() throws IOException {
	// int tempBits = 0;
	// // 0 0 0 1 3 10s
	// tempBits = evalNextWithSBit(tempBits, 3, 0x02, 2);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 12 0 1 1 4 110s
	// tempBits = evalNextWithSBit(tempBits, 1, 0x06, 3);
	// if (tempBits == -1) {
	// int[] res = { 0, 1, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 1 0 0 2 5 1111s
	// // 18 0 2 1 5 1110s
	// // 58 1 0 1 5 0111s
	// tempBits = evalNextWithSBit(tempBits, 1, 0x0F, 4);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 2, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0E, 4);
	// if (tempBits == -1) {
	// int[] res = { 0, 2, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x07, 4);
	// if (tempBits == -1) {
	// int[] res = { 1, 0, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 22 0 3 1 6 0110 1s
	// // 25 0 4 1 6 0110 0s
	// // 28 0 5 1 6 0101 1s
	// tempBits = evalNextWithSBit(tempBits, 1, 0x0D, 5);
	// if (tempBits == -1) {
	// int[] res = { 0, 3, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0C, 5);
	// if (tempBits == -1) {
	// int[] res = { 0, 4, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0B, 5);
	// if (tempBits == -1) {
	// int[] res = { 0, 5, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 2 0 0 3 7 0101 01s
	// // 13 0 1 2 7 0101 00s
	// // 31 0 6 1 7 0100 11s
	// tempBits = evalNextWithSBit(tempBits, 1, 0x15, 6);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 3, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x14, 6);
	// if (tempBits == -1) {
	// int[] res = { 0, 1, 2, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x13, 6);
	// if (tempBits == -1) {
	// int[] res = { 0, 6, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 34 0 7 1 7 0100 10s
	// // 36 0 8 1 7 0100 01s
	// // 38 0 9 1 7 0100 00s
	// // 61 1 1 1 7 0011 11s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x12, 6);
	// if (tempBits == -1) {
	// int[] res = { 0, 7, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x11, 6);
	// if (tempBits == -1) {
	// int[] res = { 0, 8, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x10, 6);
	// if (tempBits == -1) {
	// int[] res = { 0, 9, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0F, 6);
	// if (tempBits == -1) {
	// int[] res = { 1, 1, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 63 1 2 1 7 0011 10s
	// // 64 1 3 1 7 0011 01s
	// // 65 1 4 1 7 0011 00s
	// // 102 ESCAPE - - 7 0000 011
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0E, 6);
	// if (tempBits == -1) {
	// int[] res = { 1, 2, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0D, 6);
	// if (tempBits == -1) {
	// int[] res = { 1, 3, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0C, 6);
	// if (tempBits == -1) {
	// int[] res = { 1, 4, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNext(tempBits, 0, 0x03, 7); // evalNext, because length
	// // is 7 and no s bit
	// if (tempBits == -1) {
	// int[] res = { readBits(1), readBits(6), readBits(8), 0xE5CA }; // sign
	// // ==
	// // 0xE5CA
	// // ->
	// // interpret
	// // level
	// // according
	// // to
	// // table
	// // 17
	// return res;
	// }
	//
	// // 42 0 11 1 8 0010 101s
	// // 43 0 12 1 8 0010 100s
	// // 40 0 10 1 8 0010 110s
	// // 3 0 0 4 8 0010 111s
	// tempBits = evalNextWithSBit(tempBits, 1, 0x15, 7);
	// if (tempBits == -1) {
	// int[] res = { 0, 11, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x14, 7);
	// if (tempBits == -1) {
	// int[] res = { 0, 12, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x16, 7);
	// if (tempBits == -1) {
	// int[] res = { 0, 10, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x17, 7);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 4, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 66 1 5 1 8 0010 011s
	// // 67 1 6 1 8 0010 010s
	// // 68 1 7 1 8 0010 001s
	// // 69 1 8 1 8 0010 000s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x13, 7);
	// if (tempBits == -1) {
	// int[] res = { 1, 5, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x12, 7);
	// if (tempBits == -1) {
	// int[] res = { 1, 6, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x11, 7);
	// if (tempBits == -1) {
	// int[] res = { 1, 7, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x10, 7);
	// if (tempBits == -1) {
	// int[] res = { 1, 8, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 4 0 0 5 9 0001 1111s
	// // 14 0 1 3 9 0001 1110s
	// // 19 0 2 2 9 0001 1101s
	// // 44 0 13 1 9 0001 1100s
	// tempBits = evalNextWithSBit(tempBits, 1, 0x1F, 8);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 5, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1E, 8);
	// if (tempBits == -1) {
	// int[] res = { 0, 1, 3, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1D, 8);
	// if (tempBits == -1) {
	// int[] res = { 0, 2, 2, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1C, 8);
	// if (tempBits == -1) {
	// int[] res = { 0, 13, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 45 0 14 1 9 0001 1011s
	// // 70 1 9 1 9 0001 1010s
	// // 71 1 10 1 9 0001 1001s
	// // 72 1 11 1 9 0001 1000s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1B, 8);
	// if (tempBits == -1) {
	// int[] res = { 0, 14, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1A, 8);
	// if (tempBits == -1) {
	// int[] res = { 1, 9, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x19, 8);
	// if (tempBits == -1) {
	// int[] res = { 1, 10, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x18, 8);
	// if (tempBits == -1) {
	// int[] res = { 1, 11, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 73 1 12 1 9 0001 0111s
	// // 74 1 13 1 9 0001 0110s
	// // 75 1 14 1 9 0001 0101s
	// // 76 1 15 1 9 0001 0100s
	// // 77 1 16 1 9 0001 0011s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x17, 8);
	// if (tempBits == -1) {
	// int[] res = { 1, 12, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x16, 8);
	// if (tempBits == -1) {
	// int[] res = { 1, 13, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x15, 8);
	// if (tempBits == -1) {
	// int[] res = { 1, 14, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x14, 8);
	// if (tempBits == -1) {
	// int[] res = { 1, 15, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x13, 8);
	// if (tempBits == -1) {
	// int[] res = { 1, 16, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 5 0 0 6 10 0001 0010 1s
	// // 6 0 0 7 10 0001 0010 0s
	// // 23 0 3 2 10 0001 0001 1s
	// // 26 0 4 2 10 0001 0001 0s
	// tempBits = evalNextWithSBit(tempBits, 1, 0x25, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 6, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x24, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 7, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x23, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 3, 2, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x22, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 4, 2, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 46 0 15 1 10 0001 0000 1s
	// // 47 0 16 1 10 0001 0000 0s
	// // 48 0 17 1 10 0000 1111 1s
	// // 49 0 18 1 10 0000 1111 0s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x21, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 15, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x20, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 16, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1F, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 17, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1E, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 18, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 50 0 19 1 10 0000 1110 1s
	// // 51 0 20 1 10 0000 1110 0s
	// // 52 0 21 1 10 0000 1101 1s
	// // 53 0 22 1 10 0000 1101 0s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1D, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 19, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1C, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 20, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1B, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 21, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x1A, 9);
	// if (tempBits == -1) {
	// int[] res = { 0, 22, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 59 1 0 2 10 0000 1100 1s
	// // 78 1 17 1 10 0000 1100 0s
	// // 79 1 18 1 10 0000 1011 1s
	// // 80 1 19 1 10 0000 1011 0s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x19, 9);
	// if (tempBits == -1) {
	// int[] res = { 1, 0, 2, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x18, 9);
	// if (tempBits == -1) {
	// int[] res = { 1, 17, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x17, 9);
	// if (tempBits == -1) {
	// int[] res = { 1, 18, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x16, 9);
	// if (tempBits == -1) {
	// int[] res = { 1, 19, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 81 1 20 1 10 0000 1010 1s
	// // 82 1 21 1 10 0000 1010 0s
	// // 83 1 22 1 10 0000 1001 1s
	// // 84 1 23 1 10 0000 1001 0s
	// // 85 1 24 1 10 0000 1000 1s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x15, 9);
	// if (tempBits == -1) {
	// int[] res = { 1, 20, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x14, 9);
	// if (tempBits == -1) {
	// int[] res = { 1, 21, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x13, 9);
	// if (tempBits == -1) {
	// int[] res = { 1, 22, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x12, 9);
	// if (tempBits == -1) {
	// int[] res = { 1, 23, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x11, 9);
	// if (tempBits == -1) {
	// int[] res = { 1, 24, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 7 0 0 8 11 0000 1000 01s
	// // 8 0 0 9 11 0000 1000 00s
	// // 15 0 1 4 11 0000 0011 11s
	// // 20 0 2 3 11 0000 0011 10s
	// tempBits = evalNextWithSBit(tempBits, 1, 0x21, 10);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 8, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x20, 10);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 9, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0F, 10);
	// if (tempBits == -1) {
	// int[] res = { 0, 1, 4, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0E, 10);
	// if (tempBits == -1) {
	// int[] res = { 0, 2, 3, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 24 0 3 3 11 0000 0011 01s
	// // 29 0 5 2 11 0000 0011 00s
	// // 32 0 6 2 11 0000 0010 11s
	// // 35 0 7 2 11 0000 0010 10s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0D, 10);
	// if (tempBits == -1) {
	// int[] res = { 0, 3, 3, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0C, 10);
	// if (tempBits == -1) {
	// int[] res = { 0, 5, 2, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0B, 10);
	// if (tempBits == -1) {
	// int[] res = { 0, 6, 2, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x0A, 10);
	// if (tempBits == -1) {
	// int[] res = { 0, 7, 2, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 37 0 8 2 11 0000 0010 01s
	// // 39 0 9 2 11 0000 0010 00s
	// // 86 1 25 1 11 0000 0001 11s
	// // 87 1 26 1 11 0000 0001 10s
	// // 88 1 27 1 11 0000 0001 01s
	// // 89 1 28 1 11 0000 0001 00s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x09, 10);
	// if (tempBits == -1) {
	// int[] res = { 0, 8, 2, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x08, 10);
	// if (tempBits == -1) {
	// int[] res = { 0, 9, 2, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x07, 10);
	// if (tempBits == -1) {
	// int[] res = { 1, 25, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x06, 10);
	// if (tempBits == -1) {
	// int[] res = { 1, 26, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x05, 10);
	// if (tempBits == -1) {
	// int[] res = { 1, 27, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x04, 10);
	// if (tempBits == -1) {
	// int[] res = { 1, 28, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 9 0 0 10 12 0000 0000 111s
	// // 10 0 0 11 12 0000 0000 110s
	// // 11 0 0 12 12 0000 0100 000s
	// // 16 0 1 5 12 0000 0100 001s
	// tempBits = evalNextWithSBit(tempBits, 1, 0x07, 11);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 10, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x06, 11);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 11, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x20, 11);
	// if (tempBits == -1) {
	// int[] res = { 0, 0, 12, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x21, 11);
	// if (tempBits == -1) {
	// int[] res = { 0, 1, 5, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 54 0 23 1 12 0000 0100 010s
	// // 55 0 24 1 12 0000 0100 011s
	// // 60 1 0 3 12 0000 0000 101s
	// // 62 1 1 2 12 0000 0000 100s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x22, 11);
	// if (tempBits == -1) {
	// int[] res = { 0, 23, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x23, 11);
	// if (tempBits == -1) {
	// int[] res = { 0, 24, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x05, 11);
	// if (tempBits == -1) {
	// int[] res = { 1, 0, 3, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x04, 11);
	// if (tempBits == -1) {
	// int[] res = { 1, 1, 2, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 90 1 29 1 12 0000 0100 100s
	// // 91 1 30 1 12 0000 0100 101s
	// // 92 1 31 1 12 0000 0100 110s
	// // 93 1 32 1 12 0000 0100 111s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x24, 11);
	// if (tempBits == -1) {
	// int[] res = { 1, 29, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x25, 11);
	// if (tempBits == -1) {
	// int[] res = { 1, 30, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x26, 11);
	// if (tempBits == -1) {
	// int[] res = { 1, 31, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x27, 11);
	// if (tempBits == -1) {
	// int[] res = { 1, 32, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 17 0 1 6 13 0000 0101 0000s
	// // 21 0 2 4 13 0000 0101 0001s
	// // 27 0 4 3 13 0000 0101 0010s
	// // 30 0 5 3 13 0000 0101 0011s
	// tempBits = evalNextWithSBit(tempBits, 1, 0x50, 12);
	// if (tempBits == -1) {
	// int[] res = { 0, 1, 6, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x51, 12);
	// if (tempBits == -1) {
	// int[] res = { 0, 2, 4, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x52, 12);
	// if (tempBits == -1) {
	// int[] res = { 0, 4, 3, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x53, 12);
	// if (tempBits == -1) {
	// int[] res = { 0, 5, 3, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 33 0 6 3 13 0000 0101 0100s
	// // 41 0 10 2 13 0000 0101 0101s
	// // 56 0 25 1 13 0000 0101 0110s
	// // 57 0 26 1 13 0000 0101 0111s
	//
	// tempBits = evalNextWithSBit(tempBits, 0, 0x54, 12);
	// if (tempBits == -1) {
	// int[] res = { 0, 6, 3, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x55, 12);
	// if (tempBits == -1) {
	// int[] res = { 0, 10, 2, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x56, 12);
	// if (tempBits == -1) {
	// int[] res = { 0, 25, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x57, 12);
	// if (tempBits == -1) {
	// int[] res = { 0, 26, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 94 1 33 1 13 0000 0101 1000s
	// // 95 1 34 1 13 0000 0101 1001s
	// // 96 1 35 1 13 0000 0101 1010s
	// // 97 1 36 1 13 0000 0101 1011s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x58, 12);
	// if (tempBits == -1) {
	// int[] res = { 1, 33, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x59, 12);
	// if (tempBits == -1) {
	// int[] res = { 1, 34, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x5A, 12);
	// if (tempBits == -1) {
	// int[] res = { 1, 35, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x5B, 12);
	// if (tempBits == -1) {
	// int[] res = { 1, 36, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// // 98 1 37 1 13 0000 0101 1100s
	// // 99 1 38 1 13 0000 0101 1101s
	// // 100 1 39 1 13 0000 0101 1110s
	// // 101 1 40 1 13 0000 0101 1111s
	// tempBits = evalNextWithSBit(tempBits, 0, 0x5C, 12);
	// if (tempBits == -1) {
	// int[] res = { 1, 37, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x5D, 12);
	// if (tempBits == -1) {
	// int[] res = { 1, 38, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x5E, 12);
	// if (tempBits == -1) {
	// int[] res = { 1, 39, 1, (tempBits & 0x1) };
	// return res;
	// }
	// tempBits = evalNextWithSBit(tempBits, 0, 0x5F, 12);
	// if (tempBits == -1) {
	// int[] res = { 1, 40, 1, (tempBits & 0x1) };
	// return res;
	// }
	//
	// return null;
	// }
	//
	// /**
	// * This function returns an integer with numOnes ones on LSB side
	// *
	// * @param numOnes
	// * number of ones
	// * @return the generated integer
	// */
	// private int getBitMask(int numOnes) {
	// int res = 0;
	// for (int i = 0; i < numOnes; i++) {
	// res <<= 1;
	// res |= 1;
	// }
	// return res;
	// }
	//
	// private void checkForEndOfSequenceCode() throws IOException {
	// // ??|E O S| = "??|00 0000 0000 0000 00 11 1111|" = 22 bits;
	// // | 0 0 0 0 3 f
	//
	// // "0000 0000 0011 1111 1111 1111 1111 1111" "clear mask";
	// // 0x 0 0 3 f f f f f"
	//
	// int bitsBufEOS = 0;
	// int bitCount = 0;
	// while (true) {
	// // push next read bit into bitsBufPSC from right side
	// bitsBufEOS = bitsBufEOS << 1;
	// bitsBufEOS = bitsBufEOS | readNextBit();
	// bitCount++;
	// // clear left most 10 bit (only right most 22 bit are checked)
	// int tmp = bitsBufEOS & 0x003fffff;
	//
	// if (bitCount >= 22 && tmp == 0x3f) {
	// // found EOS code
	// return;
	// }
	// }
	// }
	//

	// private int errBufSize = 80;
	// private int[] errBuf = new int[errBufSize];
	// private int errBufPtr = 0;

	// private void printAndroidLogError(String s) {
	// CD("\n>>>>\n" + decTry + " " + s + "\n@" + (fisPtr + 1));
	// CD("\n" + decTry + " " + s);
	// if (CD) {
	// String bits = "";
	//
	// for (int i = 1; i <= errBufSize; i++) {
	// bits += byteToBin((byte) errBuf[(errBufPtr + i) % errBufSize]) + " ";
	// if (i % 10 == 0) {
	// bits += "\n        ";
	// }
	// }
	//
	// CD("\n## Bits " + bits);
	// CD("\n## PSC is " + (fisPtr - lastFisPtr) + " bytes old");
	// }
	// }


	private String xZeros(int x) {
		String temp = "";
		while (x > 0) {
			x--;
			temp += "0";
		}
		return temp;
	}

}
