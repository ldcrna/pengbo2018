package com.bloomberglp.blpapi.examples;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.bloomberglp.blpapi.Event;
import com.bloomberglp.blpapi.Message;
import com.bloomberglp.blpapi.MessageIterator;
import com.bloomberglp.blpapi.Request;
import com.bloomberglp.blpapi.Element;
import com.bloomberglp.blpapi.Service;
import com.bloomberglp.blpapi.Session;
import com.bloomberglp.blpapi.SessionOptions;
import com.bloomberglp.blpapi.Name;
import com.bloomberglp.blpapi.examples.model.CoreDataResponse;
import com.bloomberglp.blpapi.examples.model.SimpleFileInfo;

public class SimpleFieldSearchAPI extends HttpServlet {


    private static final String APIFLDS_SVC = "//blp/apiflds";
    private static final int ID_LEN = 13;
    private static final int MNEMONIC_LEN = 36;
    private static final int DESC_LEN = 40;
    private static final String PADDING =
            "                                            ";
    private static final Name FIELD_ID = new Name("id");
    private static final Name FIELD_MNEMONIC = new Name("mnemonic");
    private static final Name FIELD_DATA = new Name("fieldData");
    private static final Name FIELD_DESC = new Name("description");
    private static final Name FIELD_INFO = new Name("fieldInfo");
    private static final Name FIELD_ERROR = new Name("fieldError");
    private static final Name FIELD_MSG = new Name("message");

    private String d_serverHost;
    private int d_serverPort;


    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=utf-8");
        PrintWriter out = response.getWriter();

        String searchterm = request.getParameter("searchterm");
        try {
            out.println(run(searchterm));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String run(String searchterm) throws Exception {
        d_serverHost = "localhost";
        d_serverPort = 8194;

        SessionOptions sessionOptions = new SessionOptions();
        try {
            sessionOptions.setServerHost(d_serverHost);
            sessionOptions.setServerPort(d_serverPort);
        } catch (Exception eip) {
            // Ignoring
        }

        Session session = new Session(sessionOptions);
        boolean sessionStarted = session.start();
        if (!sessionStarted) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String cmd = "C:\\javaweb\\xampp\\tomcat\\bin\\Java轻度服务重启.bat";
                        Runtime.getRuntime().exec(cmd).waitFor();

                    } catch (InterruptedException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }).start();

            return  toRespose("Failed to start session.",null,-1);
        }

        if (!session.openService(APIFLDS_SVC)) {
            return  toRespose("Failed to open service: " + APIFLDS_SVC,null,-2);
        }

        Service fieldInfoService = session.getService(APIFLDS_SVC);
        Request request = fieldInfoService.createRequest("FieldSearchRequest");
        request.set("searchSpec", searchterm);

        //Element exclude = request.getElement("exclude");
        // exclude.setElement ("fieldType", "Static");

        request.set("returnFieldDocumentation", false);

        session.sendRequest(request, null);

        while (true) {
            try {
                Event event = session.nextEvent();
                MessageIterator msgIter = event.messageIterator();

                while (msgIter.hasNext()) {
                    Message msg = msgIter.next();

                    if (event.eventType() != Event.EventType.RESPONSE &&
                            event.eventType() != Event.EventType.PARTIAL_RESPONSE) {
                        continue;
                    }

                    Element fields = msg.getElement(FIELD_DATA);
                    int numElements = fields.numValues();

                    //  printHeader();
                    List<SimpleFileInfo> data = new ArrayList<SimpleFileInfo>();
                    for (int i = 0; i < numElements; i++) {
                        SimpleFileInfo info = new SimpleFileInfo();
                        Element field = fields.getValueAsElement(i);
                        String fldId, fldMnemonic, fldDesc;
                        fldId = field.getElementAsString(FIELD_ID);
                        if (field.hasElement(FIELD_INFO)) {
                            Element fldInfo = field.getElement(FIELD_INFO);
                            fldMnemonic = fldInfo.getElementAsString(FIELD_MNEMONIC);
                            fldDesc = fldInfo.getElementAsString(FIELD_DESC);

                            info.setId(fldId);
                            info.setDescription(fldDesc);
                            info.setMnemonic(fldMnemonic);
                            data.add(info);

                        } else {
                            Element fldError = field.getElement(FIELD_ERROR);
                            fldDesc = fldError.getElementAsString(FIELD_MSG);
                            info.setMessage(fldDesc);
                            System.out.println("\n ERROR: " + fldId + " - " + fldDesc);
                        }
                    }
                    return  toRespose("请求成功",data,0);
                }
                if (event.eventType() == Event.EventType.RESPONSE) break;
            } catch (Exception ex) {
                return  toRespose("Got Exception:" + ex,null,-5);
            }
        }

        return  toRespose("未知错误",null,-4);
    }

    public String toRespose(String msg, List<SimpleFileInfo> data, int status){
        CoreDataResponse<List<SimpleFileInfo> > responseInfo = new CoreDataResponse<List<SimpleFileInfo> >();
        responseInfo.setMsg(msg);
        responseInfo.setData(data);
        responseInfo.setStatus(status);
        String respose=JSON.toJSONString(responseInfo);
        return  respose;
    }

}
