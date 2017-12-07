/*
 * Contains the IP addresses and port numbers of established connections
 * 
 * 
 */

import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;


public class Connection {
    private int priority, id;
    private InetAddress addr1, addr2;
    private int port1, port2;
    private long startTime;
    
    
    public Connection() {
        id = 0;
        priority = 0;
        addr1 = null;
        port1 = 0;
        addr2 = null;
        port2 = 0;
        startTime = 0;
    }
    
    public Connection(int priority, InetAddress addr1, int port1, InetAddress addr2, int port2, long startTime) {
        id = 0;
        this.priority = priority;
        this.addr1 = addr1;
        this. port1 = port1;
        this.addr2 = addr2;
        this.port2 = port2;
        this.startTime = startTime;
    }
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public InetAddress getAddr1() {
        return addr1;
    }

    public void setAddr1(InetAddress addr1) {
        this.addr1 = addr1;
    }

    public InetAddress getAddr2() {
        return addr2;
    }

    public void setAddr2(InetAddress addr2) {
        this.addr2 = addr2;
    }

    public int getPort1() {
        return port1;
    }

    public void setPort1(int port1) {
        this.port1 = port1;
    }

    public int getPort2() {
        return port2;
    }

    public void setPort2(int port2) {
        this.port2 = port2;
    }
    
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public String toString() {
        String conn = ("ID: " + id + "\n" +
                      "Priority: " + priority + "\n" +
                      "Host IP address: " + addr1 + "\n" +
                      "Host port number: " + port1 + "\n" +
                      "Server IP address: " + addr2 + "\n" +
                      "Server port number: " + port2);
        return conn;
    }
}
