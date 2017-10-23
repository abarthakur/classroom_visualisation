import java.net.*;
import java.io.*;

import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import java.util.concurrent.*;
import java.util.Arrays;

//The main server gui class
//the main method, first initializes the gui, then sets up the server port
//after which it loops listening for connections
public class ServerGui{

	private static ServerSocket server = null;

	JFrame mainWindow;
	JPanel classPanel;
	int noSeats,seatRows,seatColumns;
	//array of panels representing seats
	SeatPanel[] seats;
	//boolean array to indicate which seats are filled
	public boolean[] seatsFilled;

	ImageIcon vacant_image;

	Object lock ;
	//initialize the gui
	ServerGui(int rows,int columns){
		//initialize gui variables
		lock = new String("LOCK");
		seatRows=rows;
		seatColumns=columns;
		noSeats=rows*columns;
		seats = new SeatPanel[noSeats];
		seatsFilled = new boolean[noSeats];
		Arrays.fill(seatsFilled, false);	

		//load vacantimage
		ImageIcon image = new ImageIcon("image/guest.png");
		vacant_image = new ImageIcon ( image.getImage().getScaledInstance(150,150,Image.SCALE_DEFAULT));


		//create window	
		mainWindow = new JFrame ( "Classroom Visualization");
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//set size
		mainWindow.setSize(800,800);
		mainWindow.setMinimumSize(new Dimension(600,600));
		
		JPanel mainPanel=new JPanel(new BorderLayout());
		mainWindow.add(mainPanel);

		//build the main class panel which displays the seats in
		//a rowsx cols grids
		classPanel = new JPanel ();
		initClassPanel(classPanel,rows,columns);
		
		//encase the class panel in a scroll panel to make it scrollable, in case
		//of very large class
		JScrollPane scrollPanel = new JScrollPane(classPanel);
		mainPanel.add(scrollPanel,BorderLayout.CENTER);		
		//display window
		mainWindow.setVisible(true);


	}
	
//build the class panel
	private void initClassPanel(JPanel panel,int rows,int columns){
		int i=0;
		SeatPanel spanel;
		//create all seats
		for(i=0;i<noSeats;i++){
			spanel = new SeatPanel(i+1);
			this.seats[i]=spanel;
			panel.add(spanel);
		}
		//set layout to grid
		GridLayout g = new GridLayout(rows,columns);
		g.setVgap(2);
		g.setHgap(2);
		panel.setLayout (g);
	}

	public SeatPanel getSPanel(int seatNo){
		System.out.println(this.seats[seatNo]);
		return this.seats[seatNo];
	}

	//used by a clienthandler thread to get a seat
	//needs to be synchronized since seatsFilled is a shared variable
	public boolean acquireSeat(int seatno){
		synchronized(lock){
			if (!seatsFilled[seatno]){
				seatsFilled[seatno]=true;
				return true;
			}
			else{
				return false;
			}
		}
	}			


	//inner class to represent a seat
	class SeatPanel extends JPanel{

		private JLabel seatLabel,nameLabel,rollLabel,imageLabel ;
		private String name="jdoe";
		private int roll=-1;
		public int seat=-1;

		//arranged using grid bag layout
		SeatPanel(int seat_no){
			Font labelFont = new Font("URW Gothic L",Font.BOLD, 12);
			this.setBackground(Color.white);
			seat = seat_no;
			//seat text
			seatLabel = new JLabel ("Seat "+seat);
			seatLabel.setHorizontalAlignment(SwingConstants.CENTER);
			//image , initialize with stock
			// ImageIcon image = new ImageIcon("image/guest.png");
			// image = new ImageIcon ( image.getImage().getScaledInstance(150,150,Image.SCALE_DEFAULT));
			ImageIcon image=vacant_image;
			imageLabel = new JLabel("", image, JLabel.CENTER);
			imageLabel.setMaximumSize(new Dimension(300,300));
			imageLabel.setPreferredSize(new Dimension(150,150));			
			//name and roll no
			nameLabel = new JLabel();
			rollLabel = new JLabel();
			this.setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			//arrange on panel with proper grid bag layout params
			c.fill=GridBagConstraints.BOTH;
			c.gridx=0;
			c.gridy=0;
			c.gridwidth=2;
//			c.weighty=0.2;
			this.add(seatLabel,c);
			seatLabel.setText("Seat "+seat);

			c.gridx=0;
			c.gridy=1;
//			c.weighty=0.2;
			this.add( imageLabel,c);

			c.gridx=0;
			c.gridy=2;
//			c.weighty=0.2;
			c.weightx=1 ;
			c.gridwidth=1;
			this.add(nameLabel,c);
			nameLabel.setText("Vacant");


			c.gridx=1;
			c.gridy=2;
			//c.weighty=0.2;
//			c.fill = GridBagConstraints.NONE;
			c.weightx= 0;
			this.add(rollLabel,c);
			rollLabel.setText("");
			seatLabel.setFont(labelFont);
			nameLabel.setFont(labelFont);
			nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

			this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.black),this.getBorder()));
			//imageLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.red),imageLabel.getBorder()));
			//nameLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.red),nameLabel.getBorder()));
			//seatLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.red),seatLabel.getBorder()));
			//rollLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.red),rollLabel.getBorder()));

		}

		//called to plug in student details into a seat panel
		//IMPORTANT : do not call directly from outer thread, use runnable and invokelater
		public void updatePanel(ImageIcon img, int rollNo, String studName){
			imageLabel.setIcon(img);		
			rollLabel.setText(String.valueOf(rollNo));		
			nameLabel.setText(studName);		
			this.repaint();
		}

	}


	public static void main( String args[]){
		//read command line args
		int port = Integer.parseInt(args[0]);
		int rows = Integer.parseInt(args[1]);
		int cols = Integer.parseInt(args[2]);
		int n1= Integer.parseInt(args[3]);
		int n2 = Integer.parseInt(args[4]);

		//create thread pool for handling client
		ExecutorService servicePool = Executors.newFixedThreadPool(n1);
		//create a fork join pool for loading images and updating gui
		//Forkjoinpool takes advantage of CPU level parallelism
		//It also uses a work stealing algo
		ForkJoinPool guiPool = new ForkJoinPool(n2);


		try{
			//set up server
			server = new ServerSocket(port);
			System.out.println("Server started at port "+ port);
			//start gui
			ServerGui gui = new ServerGui(rows,cols);
			
			while(true){
				System.out.println("Waiting for client...");
				Socket socket=server.accept();
				//add a clienthandler for this connection to the threadpool queue
				servicePool.submit(new ClientHandler(socket,gui,guiPool));
			}	


		}
		catch(IOException e){
			System.out.println(e);
		}

	}
}

//handler which reads client input and sends status
class ClientHandler implements Runnable{

	Socket socket =null;
	ServerGui gui =null;
	ForkJoinPool guiPool=null;

	ClientHandler(Socket socket,ServerGui gui,ForkJoinPool guiPool){
		this.socket=socket;
		this.gui=gui;
		this.guiPool=guiPool;
	}
	public void run(){
		System.out.println("Client accepted");
		try{
			//read input
			DataInputStream input = new DataInputStream ( new BufferedInputStream( socket.getInputStream()));
			DataOutputStream output = new DataOutputStream ( socket.getOutputStream());
			String name  = input.readUTF();
			int roll = input.readInt();
			int seat = input.readInt();
			seat=seat-1;
			File check = new File("image/"+roll+".jpg");
			System.out.println(check);
			//check error conditions
			if (!check.exists()){
				System.out.println("Roll does not exist");
				output.writeInt(2);
			}
			else if (seat>gui.noSeats){
				System.out.println("Invalid seat");
				output.writeInt(3);
			}

			else if (!gui.acquireSeat(seat)){
				System.out.println("Seat Filled");
				output.writeInt(4);
			}
			else{
				//if all good, reply 1 
				output.writeInt(1);
				System.out.println(name+" "+ roll + " " + seat + " logging in");
				//add task (which loads image, and adds a gui update task) to 
				//fork join thread pool
				SeatChanger changer = new SeatChanger ( seat,roll,name, gui);
				guiPool.submit(changer);
			}
			System.out.println("Closing connection...");
			input.close();
			output.close();
			socket.close();

		}
		//close up regardless of success/failure to free this thread
		catch(IOException e){
			System.out.println(e);
		}
	}
}

//runs on fork join pool to 1. load image 2. add task to event dispatch queue
class SeatChanger implements Runnable{
	ImageIcon imgIcon = null;
	int seatNo = 0;
	int rollNo = 0;
	String studName = null;
	ServerGui gui = null;

	SeatChanger(int seatNo, int rollNo, String studName, ServerGui gui){
		this.seatNo = seatNo;
		this.rollNo = rollNo;
		this.studName = studName;
		this.gui = gui;

		}

	public void run(){
		//read image	
		ImageIcon image = new ImageIcon("image/"+String.valueOf(rollNo)+".jpg");
		System.out.println(image);
		imgIcon = new ImageIcon ( image.getImage().getScaledInstance(150,150,Image.SCALE_DEFAULT));
		//get appropriate panel
		System.out.println(gui.getSPanel(seatNo));
		Runnable updateRunnable = new Runnable(){
			@Override 
			public void run(){
				gui.getSPanel(seatNo).updatePanel(imgIcon, rollNo, studName);
			}
		};
		//add task to swing queue
		SwingUtilities.invokeLater(updateRunnable);

	}

}


