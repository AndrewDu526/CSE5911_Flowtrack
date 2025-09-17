package org.example.model;

public class RespondToMobile {

    public String packageId;
    public boolean processResult;

    public RespondToMobile() {}

    public static RespondToMobile of(String packageId, boolean processResult){
        RespondToMobile respond = new RespondToMobile();
        respond.packageId = packageId;
        respond.processResult = processResult;

        return respond;
    }
}
