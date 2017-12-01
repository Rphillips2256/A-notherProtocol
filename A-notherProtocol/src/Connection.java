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
    private InetAddress addr1, addr2;
    private int port1, port2;
    
    
    public Connection(InetAddress addr1, int port1, InetAddress addr2, int port2) {
        this.addr1 = addr1;
        this. port1 = port1;
        this.addr2 = addr2;
        this.port2 = port2;
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
}
