package com.jike.ultracamera.camera2.module.manager;

import com.jike.ultracamera.camera2.module.BaseModule;
import com.jike.ultracamera.camera2.module.FusionModule;
import com.jike.ultracamera.camera2.module.MoonModule;
import com.jike.ultracamera.camera2.module.MoreModule;
import com.jike.ultracamera.camera2.module.NightSightModule;
import com.jike.ultracamera.camera2.module.PictureModule;
import com.jike.ultracamera.camera2.module.PortraitModule;
import com.jike.ultracamera.camera2.module.StarNightModule;

import java.util.ArrayList;

public class ModuleManager {

    public ArrayList<BaseModule> modules = new ArrayList<>();

    public BaseModule curModule;

    public ModuleManager(){
        modules.add(new StarNightModule());
        modules.add(new NightSightModule());
        modules.add(new PictureModule());
        modules.add(new FusionModule());
        modules.add(new PortraitModule());
        modules.add(new MoonModule());
        modules.add(new MoreModule());

        for(int i = 0; i < modules.size(); i++){
            if(modules.get(i).getModuleName().equals("拍照")){
                curModule = modules.get(i);
            }
        }

    }

    public void setCurModule(int idx) {
        curModule = modules.get(idx);
    }

    public BaseModule getCurModule() {
        return curModule;
    }
}
