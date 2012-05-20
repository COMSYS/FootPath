package de.uvwxy.footpath2.movement;

//hier variable anzahl an elementen - irgendwie blöd - oder?!
public class SensorTriple {
	private final static int numElements = 3;

	public float[] values = new float[numElements];

	/**
	 * timestamp
	 */
	long ts;

	/**
	 * wofür?
	 */
	int type;

	public SensorTriple(float[] values, long ts, int type) {
		// nicht unbedingt schön, aber generischer.
		if (values.length != numElements)
			throw new IllegalArgumentException();

		for (int i = 0; i < numElements; i++) {
			this.values[i] = values[i];
		}

		this.ts = ts;
		this.type = type;
	}

	@Override
	public SensorTriple clone() {
		return new SensorTriple(values, ts, type);
	}
}
