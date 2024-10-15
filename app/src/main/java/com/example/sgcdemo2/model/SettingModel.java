package com.example.sgcdemo2.model;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SettingModel extends ViewModel {
    public MutableLiveData<String> matchServer = new MutableLiveData<>();
    public MutableLiveData<String> roomServer = new MutableLiveData<>();

    public SettingModel() {
        matchServer.setValue("s://www.hakureif.site:8080");
        roomServer.setValue("s://ww2.hakureif.site:8080");
    }
}
