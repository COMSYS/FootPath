package de.uvwxy.footpath2.gui;

import android.graphics.Canvas;
import de.uvwxy.footpath2.movement.steps.StepDetection;
import de.uvwxy.footpath2.tools.PaintBox;

public class PaintBoxMovementStepDetection extends PaintBox {
	private StepDetection s;
	
	public PaintBoxMovementStepDetection(Context context, StepDetection s){
		super(context);
		this.s = s;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		
	}

}
