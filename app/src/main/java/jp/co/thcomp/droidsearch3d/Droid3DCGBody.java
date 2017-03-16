package jp.co.thcomp.droidsearch3d;

import android.content.Context;

import javax.microedition.khronos.opengles.GL10;

import jp.co.thcomp.glsurfaceview.GLCylinder;
import jp.co.thcomp.glsurfaceview.GLDrawView;
import jp.co.thcomp.glsurfaceview.RotateInfo;
import jp.co.thcomp.util.DisplayUtil;

public class Droid3DCGBody extends Droid3DCGParts {
	private GLCylinder mBodyCylinder;
	private RotateInfo mSwingInfo;
	private RotateInfo mPartsRotateInfo;
	private boolean mFirstSurfaceCreate = true;

	public Droid3DCGBody(GLDrawView view) {
		super(view);

		new Thread(new Runnable(){
			@Override
			public void run() {
				GLDrawView view = mView;

				mBodyCylinder = new GLCylinder(view);
				mBodyCylinder.setColors(RobotColorR, RobotColorG, RobotColorB, RobotColorA);
			}
		}).start();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mSurfaceWidth = width;
		mSurfaceHeight = height;

		if(mFirstSurfaceCreate){
			float density = DisplayUtil.getDefaultDisplayDensity(mContext);
			Context context = mContext;
			float halfWidth = (((float)width) - Droid3DCGParts.getRobotWidth(context)) / 2;
			float halfHeight = (((float)height) - Droid3DCGParts.getRobotHeight(context)) / 2;

			mBodyCylinder.setCylinderInfo(density * (RobotBodyOffsetX + RobotBodyRadius) + halfWidth, density * RobotBodyOffsetY + halfHeight, DefaultZOrder, density * RobotBodyRadius, density * RobotBodyHeight);
			mFirstSurfaceCreate = false;
		}
	}

	public void swing(float degree, int axis) {
		RotateInfo swingInfo = mSwingInfo;

		if(swingInfo == null){
			swingInfo = mSwingInfo = new RotateInfo();
		}

		swingInfo.rotateDegree = degree;
		switch(axis){
		case AxisX:
			swingInfo.centerX = 1f;
			swingInfo.centerY = 0f;
			swingInfo.centerZ = 0f;
			break;
		case AxisY:
			swingInfo.centerX = 0f;
			swingInfo.centerY = 1f;
			swingInfo.centerZ = 0f;
			break;
		case AxisZ:
			swingInfo.centerX = 0f;
			swingInfo.centerY = 0f;
			swingInfo.centerZ = 1f;
			break;
		}
	}

	public void rotate(float degree, int axis) {
		RotateInfo partsRotateInfo = mPartsRotateInfo;

		if(partsRotateInfo == null){
			partsRotateInfo = mPartsRotateInfo = new RotateInfo();
			mBodyCylinder.addRotation(partsRotateInfo);
		}

		partsRotateInfo.rotateDegree = degree;
		switch(axis){
		case AxisX:
			partsRotateInfo.centerX = 1f;
			partsRotateInfo.centerY = 0f;
			partsRotateInfo.centerZ = 0f;
			break;
		case AxisY:
			partsRotateInfo.centerX = 0f;
			partsRotateInfo.centerY = 1f;
			partsRotateInfo.centerZ = 0f;
			break;
		case AxisZ:
			partsRotateInfo.centerX = 0f;
			partsRotateInfo.centerY = 0f;
			partsRotateInfo.centerZ = 1f;
			break;
		}
	}

	@Override
	public void drawBase(GL10 gl) {
		boolean pushMatrix = false;
		RotateInfo swingInfo = mSwingInfo;

		try{
			if(swingInfo != null && swingInfo.rotateDegree != 0){
				gl.glPushMatrix();
				pushMatrix = true;
				gl.glRotatef(swingInfo.rotateDegree, swingInfo.centerX, swingInfo.centerY, swingInfo.centerZ);
			}

			mBodyCylinder.draw(gl);
		}finally{
			if(pushMatrix){
				gl.glPopMatrix();
			}
		}
	}

	@Override
	public void release(GL10 gl) {
	}
}
