package com.example.iotivityserver.iotivityserver.resources;

import android.util.Log;

import com.google.android.things.contrib.driver.bmx280.Bmx280;

import org.iotivity.base.EntityHandlerResult;
import org.iotivity.base.ErrorCode;
import org.iotivity.base.ObservationInfo;
import org.iotivity.base.OcException;
import org.iotivity.base.OcPlatform;
import org.iotivity.base.OcRepresentation;
import org.iotivity.base.OcResourceHandle;
import org.iotivity.base.OcResourceRequest;
import org.iotivity.base.OcResourceResponse;
import org.iotivity.base.RequestHandlerFlag;
import org.iotivity.base.RequestType;
import org.iotivity.base.ResourceProperty;

import java.io.IOException;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class BMP280Sensor implements OcPlatform.EntityHandler{
    private static final String NAME_KEY = "name";

    String TAG = "Android BMP280Sensor";
    private String mResourceUri;                //resource URI path
    private String mResourceTypeName;           //resource type name.
    private String mResourceInterface;          //resource interface.
    private OcResourceHandle mResourceHandle;   //resource handle

    private String mName;                       //sensor name.
    private Bmx280 bmx280;

    public BMP280Sensor(String resourceUri, String name, Bmx280 bmx){
        mResourceUri = resourceUri;
        mResourceTypeName = "core.temperature";
        mResourceInterface = OcPlatform.DEFAULT_INTERFACE;
        mResourceHandle = null;                 // this is set when resource is registered

        mName = name;

        bmx280 = bmx;
    }
    public synchronized void registerResource() throws OcException {
        if (null == mResourceHandle) {
            mResourceHandle = OcPlatform.registerResource(mResourceUri, mResourceTypeName, mResourceInterface,
                    this, EnumSet.of(ResourceProperty.DISCOVERABLE, ResourceProperty.OBSERVABLE));
        }
    }
    @Override
    public EntityHandlerResult handleEntity(OcResourceRequest ocResourceRequest) {
        EntityHandlerResult entityHandlerResult = EntityHandlerResult.ERROR;
        if (null == ocResourceRequest) {
            Log.d(TAG,"Server request is invalid");
            return entityHandlerResult;
        }
        EnumSet<RequestHandlerFlag> requestHandlerFlags = ocResourceRequest.getRequestHandlerFlagSet();
        if(requestHandlerFlags.contains(RequestHandlerFlag.INIT)){
            Log.d(TAG, "\t\tRequest Flag: Init");
            entityHandlerResult = EntityHandlerResult.OK;
        }
        if(requestHandlerFlags.contains(RequestHandlerFlag.REQUEST)){
            Log.d(TAG, "\t\tRequest Flag: Request");
            entityHandlerResult = handleRequest(ocResourceRequest);
        }
        if(requestHandlerFlags.contains(RequestHandlerFlag.OBSERVER)){
            Log.d(TAG, "\t\tRequest Flag: Observer");
            entityHandlerResult = handleObserver(ocResourceRequest);
        }

        return entityHandlerResult;
    }
    private EntityHandlerResult handleRequest(OcResourceRequest request) {
        EntityHandlerResult entityHandlerResult = EntityHandlerResult.ERROR;

        Map<String, String> queries = request.getQueryParameters();
        if(!queries.isEmpty()){
            Log.d(TAG, "");
        } else {
            Log.d(TAG, "No query parameters in this request");
        }

        for(Map.Entry<String, String> entry : queries.entrySet()){
            Log.d(TAG, "query is " + entry.getKey() +", "+ entry.getValue());
        }

        RequestType requestType = request.getRequestType();
        switch(requestType){
            case GET:
                Log.d(TAG, "\t\t\tRequest Type is GET");
                entityHandlerResult = handleGetRequest(request);
                break;
            case PUT:
                Log.d(TAG, "\t\t\tRequest Type is PUT");
                break;
            case POST:
                Log.d(TAG, "\t\t\tRequest Type is POST");
                break;
            case DELETE:
                Log.d(TAG, "\t\t\tRequest Type is DELETE");
                break;
        }
        return entityHandlerResult;
    }
    private EntityHandlerResult handleGetRequest(final OcResourceRequest request){
        EntityHandlerResult entityHandlerResult;
        OcResourceResponse response = new OcResourceResponse();
        response.setRequestHandle(request.getRequestHandle());
        response.setResourceHandle(request.getResourceHandle());

        response.setResponseResult(EntityHandlerResult.OK);
        response.setResourceRepresentation(getOcRepresentation());
        entityHandlerResult = sendResponse(response);
        return entityHandlerResult;
    }

    private List<Byte> mObservationIds;             //IDs of observes

    private EntityHandlerResult handleObserver(final OcResourceRequest request){
        ObservationInfo observationInfo = request.getObservationInfo();
        switch(observationInfo.getObserveAction()){
            case REGISTER:
                if (null == mObservationIds) {
                    mObservationIds = new LinkedList<>();
                }
                mObservationIds.add(observationInfo.getOcObservationId());
                break;
            case UNREGISTER:
                mObservationIds.remove((Byte)observationInfo.getOcObservationId());
                break;
        }

//        if (null == mObserverNotifier) {
//            mObserverNotifier = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    notifyObservers(request);
//                }
//            });
//            mObserverNotifier.start();
//        }
        return  EntityHandlerResult.OK;
    }

    private void notifyObservers(OcResourceRequest request){
        while(true){
            try{
                if (mIsListOfObservers) {
                    OcResourceResponse response = new OcResourceResponse();
                    response.setResourceRepresentation(getOcRepresentation());
                    OcPlatform.notifyListOfObservers(
                            mResourceHandle,
                            mObservationIds,
                            response);
                } else{
                    OcPlatform.notifyAllObservers(mResourceHandle);
                }
            } catch (OcException e) {
                ErrorCode errorCode = e.getErrorCode();
                if (errorCode.NO_OBSERVERS == errorCode){
                    Log.d(TAG, "No more observers, stopping notifications");
                    mObserverNotifier = null;
                }
                return ;
            }
        }
    }

    private EntityHandlerResult sendResponse(OcResourceResponse response){
        try{
            OcPlatform.sendResponse(response);
            return EntityHandlerResult.OK;
        } catch (OcException e){
            Log.d(TAG, e.toString());
            return EntityHandlerResult.ERROR;
        }
    }
    public synchronized void unregisterResource() throws OcException {
        if (null != mResourceHandle) {
            OcPlatform.unregisterResource(mResourceHandle);
        }
    }

    public OcRepresentation getOcRepresentation() {
        OcRepresentation rep = new OcRepresentation();

        Log.d(TAG, "get temperature form sensor");
        try{
            float temp = bmx280.readTemperature();
            rep.setValue(NAME_KEY, mName);
            rep.setValue("temperature",String.valueOf(temp));           // set temperature results
            Log.d(TAG, "the temperature is :" + String.valueOf(temp));
        }catch (OcException e) {
            Log.d(TAG, e.toString());

        } catch (IOException e) {
            Log.d(TAG, e.toString());
        }
        return rep;
    }

    private Thread mObserverNotifier;
    private boolean mIsListOfObservers = false;

}
