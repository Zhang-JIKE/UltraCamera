package com.jike.ultracamera.camera2;

public class UCamera {
    private boolean facingFront;
    private String logicId;
    private String[] physicIds;

    private String mainPhysicId;
    private String curPhysicId;

    private String[] titleList;
    private double[] angleList;

    public void setCurPhysicId(String curPhysicId) {
        this.curPhysicId = curPhysicId;
    }

    public String getCurPhysicId() {
        return curPhysicId;
    }

    public void setMainPhysicId(String mainPhysicId) {
        this.mainPhysicId = mainPhysicId;
    }

    public String getMainPhysicId() {
        return mainPhysicId;
    }

    public double[] getAngleList() {
        return angleList;
    }

    public String getLogicId() {
        return logicId;
    }

    public String[] getPhysicIds() {
        return physicIds;
    }

    public String[] getTitleList() {
        return titleList;
    }

    public boolean isHasPhysicalCamera() {
        if(physicIds != null && physicIds.length > 0){
            return true;
        }
        return false;
    }

    public void setLogicId(String logicId) {
        this.logicId = logicId;
    }

    public void setTitleList(String[] titleList) {
        this.titleList = titleList;
    }

    public void setAngleList(double[] angleList) {
        this.angleList = angleList;
    }

    public void setFacingFront(boolean facingFront) {
        this.facingFront = facingFront;
    }

    public void setPhysicIds(String[] physicIds) {
        this.physicIds = physicIds;
    }
}