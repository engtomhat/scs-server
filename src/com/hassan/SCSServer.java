package com.hassan;

import com.genesyslab.platform.commons.protocol.Endpoint;
import com.genesyslab.platform.commons.protocol.Message;
import com.genesyslab.platform.commons.protocol.MessageHandler;
import com.genesyslab.platform.commons.protocol.ProtocolException;
import com.genesyslab.platform.management.protocol.SolutionControlServerProtocol;
import com.genesyslab.platform.management.protocol.solutioncontrolserver.events.*;
import com.genesyslab.platform.management.protocol.solutioncontrolserver.requests.solutions.RequestStartSolution;
import com.genesyslab.platform.management.protocol.solutioncontrolserver.requests.solutions.RequestStopSolution;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SCSServer {

    SolutionControlServerProtocol scsProtocol;
    String scsApplicationName;
    Integer scsApplicationDBid;
    String userName;
    String scsHost;
    Integer scsPort;
    Integer solutionID;
    String action;

    MessageHandler scsMessageHandler;

    SCSServer(String scsApplicationName, Integer scsApplicationDBid, String userName,
              String scsHost, Integer scsPort, Integer solutionID, String action) {
        this.scsApplicationName = scsApplicationName;
        this.scsApplicationDBid = scsApplicationDBid;
        this.userName = userName;
        this.scsHost = scsHost;
        this.scsPort = scsPort;
        this.solutionID = solutionID;
        this.action = action.toLowerCase();
    }

    private void go() {
        connect();
        switch (action) {
            case "stop":
                stopSolution();
                break;
            case "start":
                startSolution();
                break;
            case "restart":
                restartSolution();
                break;
            default:
                out(String.format("Unsupported action (%s)", action));
                break;
        }
    }

    private void connect() {
        out(String.format("connecting to %s:%d", scsHost, scsPort));
        scsProtocol = new SolutionControlServerProtocol(new Endpoint(scsHost, scsPort));
        scsProtocol.setClientName(scsApplicationName);
        scsProtocol.setClientId(scsApplicationDBid);
        scsProtocol.setUserName(userName);
        setupEventHandler();
        try {
            out("open protocol");
            scsProtocol.open();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void restartSolution() {
        out(String.format("restarting solution (%d)", solutionID));
        stopSolution();
        startSolution();
    }

    private void startSolution() {
        out(String.format("starting solution (%d)", solutionID));
        RequestStartSolution request = RequestStartSolution.create(solutionID);
        try {
            out("sending start request");
            scsProtocol.send(request);
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
    }

    private void stopSolution() {
        out(String.format("stopping solution (%d)", solutionID));
        RequestStopSolution request = RequestStopSolution.create(solutionID);
        try {
            out("sending stop request");
            scsProtocol.send(request);
        } catch (ProtocolException e) {
            e.printStackTrace();
        }
    }

    private void setupEventHandler() {
        scsMessageHandler = message -> {
            switch (message.messageId()) {
                case EventInfo.ID:
                    handleEventInfo(message);
                    break;
            }
        };
        scsProtocol.setMessageHandler(scsMessageHandler);
    }

    private void handleEventInfo(Message message) {
        out(String.format("Event info: %s", message));
    }

    private static String nowMS() {
        // Display the ms time in a readable state
        DateFormat SDFormat = new SimpleDateFormat("yyyyMMdd.HHmmss.SSS");
        long t = System.currentTimeMillis();
        return SDFormat.format(new Date(t));
    }

    private static void out(String s) {
        System.out.printf("%s: %s\n", nowMS(), s);
    }

    public static void main(String[] args) {
        if (args.length < 7) {
            out("java -jar scs-server.jar app-name app-db-id username scs-host scs-port solution-id action");
            System.exit(1);
        }

        SCSServer scsServer = new SCSServer(
                args[0], Integer.valueOf(args[1]), args[2],
                args[3], Integer.valueOf(args[4]), Integer.valueOf(args[5]), args[6]);
        scsServer.go();
        out("done");
    }
}
