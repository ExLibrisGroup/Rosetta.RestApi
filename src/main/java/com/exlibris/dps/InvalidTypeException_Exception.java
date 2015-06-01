
package com.exlibris.dps;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 * 
 */
@WebFault(name = "InvalidTypeException", targetNamespace = "http://dps.exlibris.com/")
public class InvalidTypeException_Exception
    extends java.lang.Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private InvalidTypeException faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public InvalidTypeException_Exception(String message, InvalidTypeException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public InvalidTypeException_Exception(String message, InvalidTypeException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: com.exlibris.dps.InvalidTypeException
     */
    public InvalidTypeException getFaultInfo() {
        return faultInfo;
    }

}
