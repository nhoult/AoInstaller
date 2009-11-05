/*
 * ConsoleRedirector.java
 *
 * Created on March 26, 2008, 7:26 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package aoinstaller.gui;

import java.io.*;
import javax.swing.JTextArea;

/**
 *
 * @author http://www.comweb.nl/java/Console/Console.html
 * @author nathan Hoult
 *
 * credit also goes to whoever wrote this, it is modified by Nathan Hoult
 * and the statement:
 * "Permision to use and distribute into your own applications"
 * in the provided zip implies to me it if free for public and private use
 */
public class ConsoleRedirector implements Runnable {
    private Thread reader  = null;
    private Thread reader2 = null;
    private boolean quit = false;
    private final PipedInputStream pin  = new PipedInputStream();
    private final PipedInputStream pin2 = new PipedInputStream();
    private JTextArea text = null;
        
    /** Creates a new instance of ConsoleRedirector */
    public ConsoleRedirector(JTextArea text)
//    throws Exception
    {
        this.text = text;
        
        try
	{
		PipedOutputStream pout=new PipedOutputStream(this.pin);
		System.setOut(new PrintStream(pout,true));
	}
	catch (java.io.IOException io)
	{
		log("Couldn't redirect STDOUT to this console\n"+io.getMessage());
            
	}
	catch (SecurityException se)
	{
		log("Couldn't redirect STDOUT to this console\n"+se.getMessage());
   	}

        try
	{
		PipedOutputStream pout2=new PipedOutputStream(this.pin2);
		System.setErr(new PrintStream(pout2,true));
	}
	catch (java.io.IOException io)
	{
		log("Couldn't redirect STDERR to this console\n"+io.getMessage());
	}
	catch (SecurityException se)
	{
		log("Couldn't redirect STDERR to this console\n"+se.getMessage());
	}

        quit=false; // signals the Threads that they should exit

        // Starting two separate threads to read from the PipedInputStreams
	//
	reader=new Thread(this);
	reader.setDaemon(true);
	reader.start();
	//
	reader2=new Thread(this);
	reader2.setDaemon(true);
	reader2.start();

    }

    private void log(String line){
        text.append(line);
    }
    
    public synchronized void run()
    {
	try
	{
		while (Thread.currentThread()==reader)
		{
			try { this.wait(100);}catch(InterruptedException ie) {}
			if (pin.available()!=0)
			{
				String input=this.readLine(pin);
				log(input);
			}
			if (quit) return;
		}

		while (Thread.currentThread()==reader2)
		{
			try { this.wait(100);}catch(InterruptedException ie) {}
			if (pin2.available()!=0)
			{
				String input=this.readLine(pin2);
				log(input);
			}
			if (quit) return;
		}
	} catch (Exception e)
	{
		log("\nConsole reports an Internal error.");
		log("The error is: "+e);
	}
    }
    
    public synchronized String readLine(PipedInputStream in) throws IOException
    {
	String input="";
	do
	{
		int available=in.available();
		if (available==0) break;
		byte b[]=new byte[available];
		in.read(b);
		input=input+new String(b,0,b.length);
	}while( !input.endsWith("\n") &&  !input.endsWith("\r\n") && !quit);
	return input;
    }


}
