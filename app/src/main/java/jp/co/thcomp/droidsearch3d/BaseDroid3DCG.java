package jp.co.thcomp.droidsearch3d;

import android.graphics.PointF;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jp.co.thcomp.glsurfaceview.GLDrawElement;
import jp.co.thcomp.glsurfaceview.GLDrawView;

public abstract class BaseDroid3DCG extends GLDrawElement {
	public static final int AxisX = Droid3DCGParts.AxisX;
	public static final int AxisY = Droid3DCGParts.AxisY;
	public static final int AxisZ = Droid3DCGParts.AxisZ;

	public static final int PartsHead = 0;
	public static final int PartsLArm = 1;
	public static final int PartsRArm = 2;
	public static final int PartsBody = 3;
	public static final int PartsLFoot = 4;
	public static final int PartsRFoot = 5;
	private static final int PartsCount = PartsRFoot + 1;
	private Droid3DCGParts[] mDroid3DCGParts;

	public BaseDroid3DCG(final GLDrawView view) {
		super(view);
		mDroid3DCGParts = new Droid3DCGParts[PartsCount];

		new Thread(new Runnable(){
			@Override
			public void run() {
				GLDrawView view = mView;
				Droid3DCGParts[] droid3DCGParts = mDroid3DCGParts;

				for(int i=0; i<PartsCount; i++){
					switch(i){
					case PartsHead:
						droid3DCGParts[i] = new Droid3DCGHead(view);
						break;
					case PartsLArm:
						droid3DCGParts[i] = new Droid3DCGArm(view, Droid3DCGArm.LeftArm);
						break;
					case PartsRArm:
						droid3DCGParts[i] = new Droid3DCGArm(view, Droid3DCGArm.RightArm);
						break;
					case PartsBody:
						droid3DCGParts[i] = new Droid3DCGBody(view);
						break;
					case PartsLFoot:
						droid3DCGParts[i] = new Droid3DCGFoot(view, Droid3DCGFoot.LeftFoot);
						break;
					case PartsRFoot:
						droid3DCGParts[i] = new Droid3DCGFoot(view, Droid3DCGFoot.RightFoot);
						break;
					}
				}
			}
		}).start();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		Droid3DCGParts[] droid3DCGParts = mDroid3DCGParts;
		for(int i=0; i<PartsCount; i++){
			if(droid3DCGParts[i] != null){
				droid3DCGParts[i].onSurfaceChanged(gl, width, height);
			}
		}
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
	}

	@Override
	public void draw(GL10 gl) {
		Droid3DCGParts[] droid3DCGParts = mDroid3DCGParts;
		for(int i=0; i<PartsCount; i++){
			if(droid3DCGParts[i] != null){
				droid3DCGParts[i].draw(gl);
			}
		}
	}

	@Override
	public void release(GL10 gl) {
	}

	public void swingParts(int partsType, float degree, int axis) {
		Droid3DCGParts[] droid3DCGParts = mDroid3DCGParts;

		if(droid3DCGParts[partsType] != null){
			droid3DCGParts[partsType].swing(degree, axis);
			mView.requestRender();
		}
	}

//	public void rotateDroid(float degree, int axis) {
//		Droid3DCGParts[] droid3DCGParts = mDroid3DCGParts;
//
//		for(int i=0, size=droid3DCGParts.length; i<size; i++){
//			if(droid3DCGParts[i] != null){
//				droid3DCGParts[i].rotate(degree, axis);
//			}
//		}
//		mView.requestRender();
//	}

	@Override
	public boolean isInRect(float xPosWR, float yPoxWR) {
		return false;
	}

	@Override
	public float getX() {
		return 0;
	}

	@Override
	public float getY() {
		return 0;
	}

	@Override
	public float getZ() {
		return 0;
	}

	@Override
	public float getXinVP() {
		return 0;
	}

	@Override
	public float getYinVP() {
		return 0;
	}

	@Override
	public float getZinVP() {
		return 0;
	}

	public void startMoveTo(PointF fromPoint, PointF toPoint){
		
	}

	public void stopMoveTo(){
		
	}
}
