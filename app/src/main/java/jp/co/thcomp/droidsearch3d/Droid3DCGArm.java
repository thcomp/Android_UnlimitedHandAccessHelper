package jp.co.thcomp.droidsearch3d;

import android.content.Context;

import javax.microedition.khronos.opengles.GL10;

import jp.co.thcomp.glsurfaceview.GLCylinder;
import jp.co.thcomp.glsurfaceview.GLDrawView;
import jp.co.thcomp.glsurfaceview.GLHemiSphere;
import jp.co.thcomp.glsurfaceview.RotateInfo;
import jp.co.thcomp.util.DisplayUtil;

public class Droid3DCGArm extends Droid3DCGParts {
	public static final int LeftArm = 0;
	public static final int RightArm = 1;
	public static final float RobotArmOffsetX[] = {RobotLArmOffsetX, RobotRArmOffsetX};
	private GLHemiSphere mArmTopHemiSphere;
	private GLCylinder mArmCylinder;
	private GLHemiSphere mArmBtmHemiSphere;
	private int mArmIndex;
	private RotateInfo mSwingInfo;
	private RotateInfo mPartsRotateInfo;
	private boolean mFirstSurfaceCreate = true;

	public Droid3DCGArm(GLDrawView view, int armIndex) {
		super(view);
		mArmIndex = armIndex;
		if(armIndex != LeftArm && armIndex != RightArm){
			throw new UnsupportedOperationException();
		}

		new Thread(new Runnable(){
			@Override
			public void run() {
				GLDrawView view = mView;

				mArmTopHemiSphere = new GLHemiSphere(view);
				mArmCylinder = new GLCylinder(view);
				mArmBtmHemiSphere = new GLHemiSphere(view);

				mArmTopHemiSphere.setColors(RobotColorR, RobotColorG, RobotColorB, RobotColorA);
				mArmCylinder.setColors(RobotColorR, RobotColorG, RobotColorB, RobotColorA);
				mArmBtmHemiSphere.setColors(RobotColorR, RobotColorG, RobotColorB, RobotColorA);
			}
		}).start();
	}

	public void swing(float degree, int axis){
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

	public void rotate(float degree, int axis){
		RotateInfo partsRotateInfo = mPartsRotateInfo;

		if(partsRotateInfo == null){
			partsRotateInfo = mPartsRotateInfo = new RotateInfo();
			mArmTopHemiSphere.addRotation(partsRotateInfo);
			mArmCylinder.addRotation(partsRotateInfo);
			mArmBtmHemiSphere.addRotation(partsRotateInfo);
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
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		mSurfaceWidth = width;
		mSurfaceHeight = height;
		float density = DisplayUtil.getDefaultDisplayDensity(mContext);

		if(mFirstSurfaceCreate){
			int armIndex = mArmIndex;
			Context context = mContext;
			float halfWidth = (((float)width) - Droid3DCGParts.getRobotWidth(context)) / 2;
			float halfHeight = (((float)height) - Droid3DCGParts.getRobotHeight(context)) / 2;
			mArmTopHemiSphere.setSphereInfo(density * (RobotArmOffsetX[armIndex] + RobotArmRadius) + halfWidth, density * (RobotArmOffsetY + RobotArmRadius) + halfHeight, DefaultZOrder, density * RobotArmRadius);
			mArmCylinder.setCylinderInfo(density * (RobotArmOffsetX[armIndex] + RobotArmRadius) + halfWidth, density * (RobotArmOffsetY + RobotArmRadius) + halfHeight, DefaultZOrder, density * RobotArmRadius, density * RobotArmCylinderHeight);
			mArmBtmHemiSphere.setSphereInfo(density * (RobotArmOffsetX[armIndex] + RobotArmRadius) + halfWidth, density * (RobotArmOffsetY + RobotArmRadius + RobotArmCylinderHeight) + halfHeight, DefaultZOrder, density * RobotArmRadius);
			mArmBtmHemiSphere.addRotation(180, 1f, 0f, 0f);
			mFirstSurfaceCreate = false;
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

			mArmTopHemiSphere.draw(gl);
			mArmCylinder.draw(gl);
			mArmBtmHemiSphere.draw(gl);
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
